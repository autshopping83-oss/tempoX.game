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

    /** True while a purchase flow is actively in flight (both launcher UI shown and awaiting result). */
    val isPurchasing: StateFlow<Boolean>

    /**
     * Single source of truth for "ads removed". Every ad placement in the game
     * MUST call this before loading or showing any ad. Never rely on a bare
     * local variable — this reads the billing-backed [isAdFreeUser].
     */
    fun isAdsRemoved(): Boolean = isAdFreeUser.value

    /**
     * Launches the purchase flow for the Remove Ads product.
     * Exactly one of the callbacks is invoked on completion.
     *
     * @param onSuccess called when the purchase is confirmed PURCHASED + acknowledged.
     * @param onError called with a stable error key: "product_unavailable",
     *        "billing_unavailable", or a billing debug message.
     */
    fun purchaseRemoveAds(activity: Activity, onSuccess: () -> Unit, onError: (String) -> Unit)

    /** Re-validates prior purchases (reinstalls / Play Store compliance / product already owned). */
    fun restorePurchases()
}
