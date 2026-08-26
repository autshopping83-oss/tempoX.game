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

    val isAdFree: Boolean get() = billing?.isAdFreeUser?.value == true

    /** Show a real interstitial ad if the user is not ad-free. */
    fun showInterstitialIfAllowed(activity: Activity, onDismiss: () -> Unit) {
        if (isAdFree) { onDismiss(); return }
        AdMobManager.showInterstitial(activity, onDismiss)
    }

    /** Preload both rewarded + interstitial ads (call when Activity is available). */
    fun preloadAds(activity: Activity) {
        AdMobManager.loadRewarded(activity)
        AdMobManager.loadInterstitial(activity)
    }
}
