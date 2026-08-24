package cloud.bizflow.tempox.game

import android.content.Context
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Simulated billing provider backed by local preferences. The real gateway
 * would launch the Play purchase sheet inside [purchaseRemoveAds]; the UI
 * layer presents the mock confirmation dialog before calling through.
 */
class MockBillingRepositoryImpl(context: Context) : BillingRepository {

    private val prefs =
        context.getSharedPreferences("temprox_billing", Context.MODE_PRIVATE)

    private val _isAdFreeUser = MutableStateFlow(prefs.getBoolean(K_AD_FREE, false))

    override val isAdFreeUser: StateFlow<Boolean> = _isAdFreeUser.asStateFlow()

    override fun purchaseRemoveAds(onSuccess: () -> Unit, onError: (String) -> Unit) {
        if (_isAdFreeUser.value) {
            onSuccess()
            return
        }
        // Mock approval — a real implementation resolves via PurchasesUpdatedListener
        // and routes cancellations/failures to [onError].
        _isAdFreeUser.value = true
        prefs.edit().putBoolean(K_AD_FREE, true).apply()
        onSuccess()
    }

    override fun restorePurchases() {
        _isAdFreeUser.value = prefs.getBoolean(K_AD_FREE, false)
    }

    companion object {
        private const val K_AD_FREE = "adFree"
    }
}
