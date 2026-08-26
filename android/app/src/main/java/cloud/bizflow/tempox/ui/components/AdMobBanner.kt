package cloud.bizflow.tempox.ui.components

import android.content.Context
import android.util.Log
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.google.android.gms.ads.AdListener
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.AdSize
import com.google.android.gms.ads.AdView
import com.google.android.gms.ads.LoadAdError

private const val BANNER_TAG = "AdMobBanner"

/**
 * Production-safe AdMob banner component.
 *
 * - Uses an official Google TEST ad unit during development.
 * - Reserves a fixed 50 dp slot so the game UI never jumps when the ad loads.
 * - Applies [navigationBarsPadding] to stay above the system gesture line.
 * - Is fully destroyed in [DisposableEffect] onDispose to avoid memory leaks.
 */
@Composable
fun AdMobBanner(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val adView = remember { createAdView(context) }

    DisposableEffect(Unit) {
        onDispose {
            Log.d(BANNER_TAG, "destroying AdView")
            adView.destroy()
        }
    }

    AndroidView(
        factory = { adView },
        modifier = modifier
            .fillMaxWidth()
            .height(50.dp)
            .navigationBarsPadding(),
        update = { view ->
            view.loadAd(AdRequest.Builder().build())
        },
    )
}

private fun createAdView(context: Context): AdView {
    return AdView(context).apply {
        // Official Google TEST banner ID — safe for development.
        adUnitId = "ca-app-pub-3940256099942544/6300978111"
        setAdSize(AdSize.BANNER)
        adListener = object : AdListener() {
            override fun onAdFailedToLoad(error: LoadAdError) {
                Log.w(BANNER_TAG, "Banner failed: ${error.message}")
            }
            override fun onAdLoaded() {
                Log.d(BANNER_TAG, "Banner loaded successfully")
            }
        }
    }
}
