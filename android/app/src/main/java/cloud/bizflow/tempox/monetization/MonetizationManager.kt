package cloud.bizflow.tempox.monetization

import android.content.Context
import android.util.Log
import com.google.android.gms.ads.MobileAds
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * Bootstraps the Google Mobile Ads SDK fully OFF the main thread so app
 * startup never janks.
 *
 * Debug builds automatically receive the official Google TEST ad IDs via
 * AdConstants — no production traffic is ever sent during development.
 */
object MonetizationManager {
    private const val TAG = "MonetizationManager"

    @Volatile
    private var initialized = false

    fun initialize(context: Context) {
        if (initialized) return
        synchronized(this) {
            if (initialized) return
            CoroutineScope(Dispatchers.IO).launch {
                runCatching {
                    MobileAds.initialize(context) { initializationStatus ->
                        Log.i(TAG, "SDK pronto — adapters: ${initializationStatus.adapterStatusMap.keys}")
                    }
                }.onFailure { e ->
                    Log.w(TAG, "AdMob init failed: ${e.message}")
                }
                initialized = true
            }
        }
    }
}
