package cloud.bizflow.tempox.game

/**
 * Single gate shared by every simulated ad placement. Reads the billing
 * abstraction so VIP players skip waits and overlays automatically — and so
 * a future real AdManager can replace this object without touching UI code.
 */
object MockAdManager {

    /** Wired once at app composition root. */
    var billing: BillingRepository? = null

    val isAdFree: Boolean get() = billing?.isAdFreeUser?.value == true

    /**
     * Interstitial seam. VIP users are dismissed instantly, never seeing an
     * overlay; non-VIP currently gets a no-op until real interstitials land.
     */
    fun showInterstitialIfAllowed(onDismiss: () -> Unit) {
        if (!isAdFree) {
            // Simulated interstitial placement — intentionally empty for now.
        }
        onDismiss()
    }

    /** Rewarded wait: VIP revives/doubles resolve instantly. */
    fun rewardedWaitMillis(): Long = if (isAdFree) 0L else 1600L
}
