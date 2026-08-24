package cloud.bizflow.tempox.game

import kotlinx.coroutines.flow.StateFlow

/**
 * Purchase gateway abstraction. Swapping [MockBillingRepositoryImpl] for the
 * real Google Play Billing client must require zero changes in Compose
 * screens or view models — they only ever see this contract.
 */
interface BillingRepository {

    /** True once the player owns the "Remove Ads" product. */
    val isAdFreeUser: StateFlow<Boolean>

    /**
     * Launches the purchase flow for the Remove Ads product.
     * Exactly one of the callbacks is invoked on completion.
     */
    fun purchaseRemoveAds(onSuccess: () -> Unit, onError: (String) -> Unit)

    /** Re-validates prior purchases (reinstalls / Play Store compliance). */
    fun restorePurchases()
}
