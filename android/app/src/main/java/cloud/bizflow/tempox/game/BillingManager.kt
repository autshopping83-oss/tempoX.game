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
import kotlinx.coroutines.launch
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

    private val _isPurchasing = MutableStateFlow(false)
    override val isPurchasing: StateFlow<Boolean> = _isPurchasing.asStateFlow()

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

    private fun queryProductDetails(onResolved: (() -> Unit)? = null) {
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
                } else {
                    Log.w(TAG, "Product details list is empty for $PRODUCT_REMOVE_ADS")
                }
            } else {
                Log.w(TAG, "queryProductDetails failed: ${result.debugMessage}")
            }
            _isLoading.value = false
            onResolved?.invoke()
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
        _isPurchasing.value = false
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

        // Product already resolved — launch directly.
        val details = productDetails
        if (details != null) {
            launchBillingFlow(activity, details, onError)
            return
        }

        // Product not resolved yet (billing reconnected, or the initial query ran
        // before setup finished). Re-query it before giving up, with a short
        // timeout so the user is not left hanging.
        Log.w(TAG, "Remove-ads product not resolved yet — re-querying…")
        scope.launch { retryFetchProductAndPurchase(activity, onError) }
    }

    /**
     * When the product was not available on demand, wait for the billing client
     * to be ready, re-query the product and launch the flow. Falls back to
     * [onError] if the product never resolves within a short timeout.
     */
    private suspend fun retryFetchProductAndPurchase(activity: Activity, onError: (String) -> Unit) {
        val deadline = System.currentTimeMillis() + 10_000L
        while (System.currentTimeMillis() < deadline) {
            if (billingClient.isReady) {
                val resolved = productDetails
                if (resolved != null) {
                    launchBillingFlow(activity, resolved, onError)
                    return
                }
                // Ready but not resolved — query and check once.
                var fetched = false
                queryProductDetails {
                    fetched = true
                    val now = productDetails
                    if (now != null) {
                        launchBillingFlow(activity, now, onError)
                    } else {
                        onError("product_unavailable")
                    }
                }
                // Let the async query complete.
                var waited = 0
                while (!fetched && waited < 5000) {
                    delay(100)
                    waited += 100
                }
                if (fetched) return
            } else {
                // Billing dropped or never connected — re-request a connection.
                Log.w(TAG, "Billing not ready, reconnecting…")
                if (!billingClient.isReady) {
                    runCatching { billingClient.startConnection(this) }
                }
            }
            delay(500)
        }
        Log.w(TAG, "Remove-ads product never resolved within timeout")
        _isPurchasing.value = false
        onError("product_unavailable")
    }

    private fun launchBillingFlow(activity: Activity, details: ProductDetails, onError: (String) -> Unit) {
        val flowParams = BillingFlowParams.newBuilder()
            .setProductDetailsParamsList(
                listOf(
                    BillingFlowParams.ProductDetailsParams.newBuilder()
                        .setProductDetails(details)
                        .build()
                ),
            )
            .build()
        _isPurchasing.value = true
        val billingResult = billingClient.launchBillingFlow(activity, flowParams)
        if (billingResult.responseCode != BillingClient.BillingResponseCode.OK) {
            Log.w(TAG, "Could not launch billing: ${billingResult.debugMessage}")
            _isPurchasing.value = false
            if (billingResult.responseCode == BillingClient.BillingResponseCode.BILLING_UNAVAILABLE) {
                onError("billing_unavailable")
            } else {
                onError(billingResult.debugMessage ?: "billing_launch_failed")
            }
        }
    }

    override fun restorePurchases() {
        queryPurchases()
    }
}
