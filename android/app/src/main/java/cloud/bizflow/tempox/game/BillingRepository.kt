package cloud.bizflow.tempox.game

import android.app.Activity
import kotlinx.coroutines.flow.StateFlow

/**
 * Purchase gateway abstraction. The UI layer only sees this contract —
 * the real Google Play Billing client lives in [BillingManager].
 */
interface BillingRepository {

    /** True once the player owns the "Remove Ads" product. */
    val isAdFreeUser: StateFlow<Boolean>

    /** Dynamically resolved price from Google Play (e.g. "US$ 4.99"). */
    val formattedPrice: StateFlow<String>

    /** True while the billing client is connecting / resolving product details. */
    val isLoading: StateFlow<Boolean>

    /**
     * Launches the purchase flow for the Remove Ads product.
     * Exactly one of the callbacks is invoked on completion.
     */
    fun purchaseRemoveAds(activity: Activity, onSuccess: () -> Unit, onError: (String) -> Unit)

    /** Re-validates prior purchases (reinstalls / Play Store compliance). */
    fun restorePurchases()
}
