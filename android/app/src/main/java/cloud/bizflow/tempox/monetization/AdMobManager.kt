package cloud.bizflow.tempox.monetization

import android.app.Activity
import android.util.Log
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.FullScreenContentCallback
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.interstitial.InterstitialAd
import com.google.android.gms.ads.interstitial.InterstitialAdLoadCallback
import com.google.android.gms.ads.rewarded.RewardedAd
import com.google.android.gms.ads.rewarded.RewardedAdLoadCallback

/**
 * Central manager for loading and showing Rewarded and Interstitial ads.
 * Uses the official Google test IDs via [AdConstants] in debug builds.
 * Reward is ONLY granted when the user watches the ad to completion — closing
 * early never triggers the callback.
 */
object AdMobManager {
    private const val TAG = "AdMobManager"

    private var rewardedAd: RewardedAd? = null
    private var interstitialAd: InterstitialAd? = null

    // ── Rewarded ──────────────────────────────────────────────────────────

    fun loadRewarded(activity: Activity) {
        if (rewardedAd != null) return
        val adRequest = AdRequest.Builder().build()
        RewardedAd.load(
            activity,
            AdConstants.REWARDED_AD_UNIT_ID,
            adRequest,
            object : RewardedAdLoadCallback() {
                override fun onAdLoaded(ad: RewardedAd) {
                    rewardedAd = ad
                    Log.d(TAG, "Rewarded ad loaded")
                }

                override fun onAdFailedToLoad(error: LoadAdError) {
                    rewardedAd = null
                    Log.e(TAG, "Rewarded ad failed: ${error.message}")
                }
            },
        )
    }

    fun showRewarded(
        activity: Activity,
        onRewardEarned: () -> Unit,
        onAdDismissed: () -> Unit,
    ) {
        val ad = rewardedAd
        if (ad == null) {
            loadRewarded(activity)
            onAdDismissed()
            return
        }
        var earned = false
        ad.fullScreenContentCallback = object : FullScreenContentCallback() {
            override fun onAdDismissedFullScreenContent() {
                rewardedAd = null
                loadRewarded(activity)
                if (earned) onRewardEarned()
                onAdDismissed()
            }

            override fun onAdFailedToShowFullScreenContent(error: com.google.android.gms.ads.AdError) {
                rewardedAd = null
                onAdDismissed()
            }
        }
        ad.show(activity) { earned = true }
    }

    // ── Interstitial ──────────────────────────────────────────────────────

    fun loadInterstitial(activity: Activity) {
        if (interstitialAd != null) return
        val adRequest = AdRequest.Builder().build()
        InterstitialAd.load(
            activity,
            AdConstants.INTERSTITIAL_AD_UNIT_ID,
            adRequest,
            object : InterstitialAdLoadCallback() {
                override fun onAdLoaded(ad: InterstitialAd) {
                    interstitialAd = ad
                    Log.d(TAG, "Interstitial ad loaded")
                }

                override fun onAdFailedToLoad(error: LoadAdError) {
                    interstitialAd = null
                    Log.e(TAG, "Interstitial ad failed: ${error.message}")
                }
            },
        )
    }

    fun showInterstitial(activity: Activity, onDismissed: () -> Unit) {
        val ad = interstitialAd
        if (ad == null) {
            loadInterstitial(activity)
            onDismissed()
            return
        }
        ad.fullScreenContentCallback = object : FullScreenContentCallback() {
            override fun onAdDismissedFullScreenContent() {
                interstitialAd = null
                loadInterstitial(activity)
                onDismissed()
            }

            override fun onAdFailedToShowFullScreenContent(error: com.google.android.gms.ads.AdError) {
                interstitialAd = null
                onDismissed()
            }
        }
        ad.show(activity)
    }
}
