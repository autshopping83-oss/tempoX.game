package cloud.bizflow.tempox.game

import android.app.Activity
import android.content.Context
import android.util.Log
import com.android.billingclient.api.AcknowledgePurchaseParams
import com.android.billingclient.api.BillingClient
import com.android.billingclient.api.BillingClientStateListener
import com.android.billingclient.api.BillingFlowParams
import com.android.billingclient.api.BillingResult
import com.android.billingclient.api.PendingPurchasesParams
import com.android.billingclient.api.ProductDetails
import com.android.billingclient.api.Purchase
import com.android.billingclient.api.PurchasesUpdatedListener
import com.android.billingclient.api.QueryProductDetailsParams
import com.android.billingclient.api.QueryPurchasesParams
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Real Google Play Billing implementation (Billing Library v8.2.1).
 *
 * Implements [BillingRepository] so the UI layer is completely decoupled from
 * the underlying billing SDK.  Call [startConnection] during app launch and
 * [endConnection] in [android.app.Activity.onDestroy].
 *
 * Product ID must match the one registered in Google Play Console under
 * "Monetize → Products → In-app products".
 */
class BillingManager(private val context: Context) : BillingRepository,
    PurchasesUpdatedListener, BillingClientStateListener {

    companion object {
        private const val TAG = "BillingManager"
        private const val PRODUCT_REMOVE_ADS = "tempox_no_ads"
        private const val BillingSetupTimeoutMs = 10_000L
    }

    // ── BillingRepository contract ────────────────────────────────────────────
    private val _isAdFreeUser = MutableStateFlow(false)
    override val isAdFreeUser: StateFlow<Boolean> = _isAdFreeUser.asStateFlow()

    private val _formattedPrice = MutableStateFlow("")
    override val formattedPrice: StateFlow<String> = _formattedPrice.asStateFlow()

    private val _isLoading = MutableStateFlow(true)
    override val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    // ── Internal state ────────────────────────────────────────────────────────
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var productDetails: ProductDetails? = null

    private val prefs by lazy {
        context.getSharedPreferences("temprox_billing", Context.MODE_PRIVATE)
    }

    private val billingClient: BillingClient = BillingClient.newBuilder(context)
        .setListener(this)
        .enablePendingPurchases(
            PendingPurchasesParams.newBuilder().enableOneTimeProducts().build()
        )
        .enableAutoServiceReconnection()
        .build()

    // ── Connection lifecycle ──────────────────────────────────────────────────

    fun startConnection() {
        _isAdFreeUser.value = prefs.getBoolean("adFree", false)
        if (!billingClient.isReady) {
            Log.d(TAG, "Starting billing connection…")
            billingClient.startConnection(this)
        }
        // Safety timeout: never leave isLoading stuck true if the billing setup
        // callback never fires or reports OK (keeps the paywall CTA responsive).
        scope.launch {
            delay(BillingSetupTimeoutMs)
            _isLoading.value = false
        }
    }

    fun endConnection() {
        Log.d(TAG, "Ending billing connection")
        billingClient.endConnection()
    }

    override fun onBillingSetupFinished(result: BillingResult) {
        // ALWAYS clear isLoading here, regardless of outcome. Otherwise, if the
        // connection never reports OK (e.g. in Closed Testing the product/app may
        // not be queryable), the CTA button would show an infinite spinner.
        _isLoading.value = false
        if (result.responseCode == BillingClient.BillingResponseCode.OK) {
            Log.d(TAG, "Billing connected")
            queryPurchases()
            queryProductDetails()
        } else {
            Log.w(TAG, "Billing setup failed: ${result.debugMessage}")
        }
    }

    override fun onBillingServiceDisconnected() {
        Log.w(TAG, "Billing disconnected — reconnect handled by enableAutoServiceReconnection")
    }

    // ── Purchase queries ──────────────────────────────────────────────────────

    private fun queryPurchases() {
        val params = QueryPurchasesParams.newBuilder()
            .setProductType(BillingClient.ProductType.INAPP)
            .build()

        billingClient.queryPurchasesAsync(params) { result, purchases ->
            if (result.responseCode == BillingClient.BillingResponseCode.OK) {
                val hasPremium = purchases.any {
                    it.products.contains(PRODUCT_REMOVE_ADS) &&
                        it.purchaseState == Purchase.PurchaseState.PURCHASED
                }
                setPremiumUnlocked(hasPremium)
            }
        }
    }

    private fun queryProductDetails() {
        val product = QueryProductDetailsParams.Product.newBuilder()
            .setProductId(PRODUCT_REMOVE_ADS)
            .setProductType(BillingClient.ProductType.INAPP)
            .build()

        val params = QueryProductDetailsParams.newBuilder()
            .setProductList(listOf(product))
            .build()

        billingClient.queryProductDetailsAsync(params) { result, queryResult ->
            if (result.responseCode == BillingClient.BillingResponseCode.OK) {
                val details = queryResult.productDetailsList
                if (details.isNotEmpty()) {
                    productDetails = details.first()
                    _formattedPrice.value = details.first().oneTimePurchaseOfferDetails?.formattedPrice ?: ""
                    Log.d(TAG, "Product resolved: ${details.first().title} — ${_formattedPrice.value}")
                }
            }
            _isLoading.value = false
        }
    }

    // ── PurchasesUpdatedListener ──────────────────────────────────────────────

    override fun onPurchasesUpdated(result: BillingResult, purchases: List<Purchase>?) {
        if (result.responseCode == BillingClient.BillingResponseCode.OK && purchases != null) {
            purchases.forEach { handlePurchase(it) }
        } else if (result.responseCode == BillingClient.BillingResponseCode.USER_CANCELED) {
            Log.d(TAG, "Purchase cancelled by user")
        } else {
            Log.w(TAG, "Purchase error: ${result.debugMessage}")
        }
    }

    // ── Purchase handling ─────────────────────────────────────────────────────

    private fun handlePurchase(purchase: Purchase) {
        if (purchase.purchaseState == Purchase.PurchaseState.PURCHASED) {
            if (!purchase.isAcknowledged) {
                val params = AcknowledgePurchaseParams.newBuilder()
                    .setPurchaseToken(purchase.purchaseToken)
                    .build()
                billingClient.acknowledgePurchase(params) { result ->
                    if (result.responseCode == BillingClient.BillingResponseCode.OK) {
                        setPremiumUnlocked(true)
                        Log.i(TAG, "Purchase acknowledged")
                    }
                }
            } else {
                setPremiumUnlocked(true)
            }
        }
    }

    private fun setPremiumUnlocked(unlocked: Boolean) {
        _isAdFreeUser.value = unlocked
        prefs.edit().putBoolean("adFree", unlocked).apply()
    }

    // ── BillingRepository implementation ──────────────────────────────────────

    override fun purchaseRemoveAds(activity: Activity, onSuccess: () -> Unit, onError: (String) -> Unit) {
        if (_isAdFreeUser.value) {
            onSuccess()
            return
        }
        val details = productDetails
        if (details == null) {
            // Product could not be resolved (not found, inactive, or billing not
            // ready). Surface a friendly message instead of silently hanging.
            Log.w(TAG, "Remove-ads product not available")
            onError("product_unavailable")
            return
        }
        val flowParams = BillingFlowParams.newBuilder()
            .setProductDetailsParamsList(
                listOf(
                    BillingFlowParams.ProductDetailsParams.newBuilder()
                        .setProductDetails(details)
                        .build()
                ),
            )
            .build()
        val billingResult = billingClient.launchBillingFlow(activity, flowParams)
        if (billingResult.responseCode != BillingClient.BillingResponseCode.OK) {
            onError("Could not launch billing: ${billingResult.debugMessage}")
        }
    }

    override fun restorePurchases() {
        queryPurchases()
    }
}
