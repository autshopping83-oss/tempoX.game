package cloud.bizflow.tempox.game

import android.app.Activity
import cloud.bizflow.tempox.monetization.AdMobManager

/**
 * Single gate shared by every ad placement. Reads the billing abstraction so
 * VIP players skip ads — and delegates to [AdMobManager] for real ad display.
 */
object MockAdManager {

    /** Wired once at app composition root. */
    var billing: BillingRepository? = null

    /** Single source of truth: ads are removed when the user owns the product. */
    val isAdFree: Boolean get() = billing?.isAdsRemoved() == true

    /** Show a real interstitial ad if the user is not ad-free. */
    fun showInterstitialIfAllowed(activity: Activity, onDismiss: () -> Unit) {
        if (isAdFree) { onDismiss(); return }
        AdMobManager.showInterstitial(activity, onDismiss)
    }

    /** Preload ads only for non-premium users — never load ads for ad-free players. */
    fun preloadAds(activity: Activity) {
        if (isAdFree) return
        AdMobManager.loadRewarded(activity)
        AdMobManager.loadInterstitial(activity)
    }
}
