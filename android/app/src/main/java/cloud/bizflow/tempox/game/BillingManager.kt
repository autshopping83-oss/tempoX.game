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
import kotlinx.coroutines.launch

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
        private const val PRODUCT_REMOVE_ADS = "remove_ads"
    }

    // ── BillingRepository contract ────────────────────────────────────────────
    private val _isAdFreeUser = MutableStateFlow(false)
    override val isAdFreeUser: StateFlow<Boolean> = _isAdFreeUser.asStateFlow()

    // ── Internal state ────────────────────────────────────────────────────────
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var productDetails: ProductDetails? = null

    private val billingClient: BillingClient = BillingClient.newBuilder(context)
        .setListener(this)
        .enablePendingPurchases(
            PendingPurchasesParams.newBuilder().enableOneTimeProducts().build()
        )
        .enableAutoServiceReconnection()
        .build()

    // ── Connection lifecycle ──────────────────────────────────────────────────

    fun startConnection() {
        if (!billingClient.isReady) {
            Log.d(TAG, "Starting billing connection…")
            billingClient.startConnection(this)
        }
    }

    fun endConnection() {
        Log.d(TAG, "Ending billing connection")
        billingClient.endConnection()
    }

    override fun onBillingSetupFinished(result: BillingResult) {
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
                purchases.forEach { handlePurchase(it) }
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
                    Log.d(TAG, "Product resolved: ${details.first().title}")
                }
            }
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
                        _isAdFreeUser.value = true
                        Log.i(TAG, "Purchase acknowledged")
                    }
                }
            } else {
                _isAdFreeUser.value = true
            }
        }
    }

    // ── BillingRepository implementation ──────────────────────────────────────

    override fun purchaseRemoveAds(onSuccess: () -> Unit, onError: (String) -> Unit) {
        if (_isAdFreeUser.value) {
            onSuccess()
            return
        }
        val details = productDetails
        if (details == null) {
            onError("Product not loaded yet")
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
        val billingResult = billingClient.launchBillingFlow(context as Activity, flowParams)
        if (billingResult.responseCode != BillingClient.BillingResponseCode.OK) {
            onError("Could not launch billing: ${billingResult.debugMessage}")
        }
    }

    override fun restorePurchases() {
        queryPurchases()
    }
}
