package cloud.bizflow.tempox.monetization

import android.content.Context
import com.google.android.gms.ads.MobileAds
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * Bootstraps the Google Mobile Ads SDK fully OFF the main thread so app
 * startup never janks. Uses the official Google TEST application ID while in
 * development (see AndroidManifest meta-data) to avoid invalid-traffic bans.
 */
object MonetizationManager {
    @Volatile
    private var initialized = false

    fun initialize(context: Context) {
        if (initialized) return
        synchronized(this) {
            if (initialized) return
            CoroutineScope(Dispatchers.IO).launch {
                runCatching {
                    MobileAds.initialize(context) { status ->
                        // Adapter status map — logged for diagnostics only.
                        android.util.Log.i("MonetizationManager", "AdMob init: ${status.adapterStatusMap.keys}")
                    }
                }.onFailure { e ->
                    android.util.Log.w("MonetizationManager", "AdMob init failed: ${e.message}")
                }
                initialized = true
            }
        }
    }
}
