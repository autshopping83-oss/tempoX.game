package cloud.bizflow.tempox.game

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Reusable connectivity probe. Returns true only for a network that is both
 * connected to the internet AND validated (CAPABILITY_VALIDATED), matching the
 * semantics already used in LegalConstants.isOnline(). Never throws: on any
 * failure (missing permission, ROM quirks) it reports offline — the ad path is
 * simply hidden, which is the safe default for the "double coins" feature.
 */
private fun Context.currentOnline(): Boolean {
    return runCatching {
        val cm = getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager
            ?: return@runCatching false
        val network = cm.activeNetwork ?: return@runCatching false
        val caps = cm.getNetworkCapabilities(network) ?: return@runCatching false
        caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) &&
            caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)
    }.getOrElse { false }
}

/**
 * Reactively tracks online/offline state via a default network callback so the
 * UI can show/hide the rewarded-ad "double coins" shortcut the moment the
 * connection changes — even while the result screen is on screen.
 */
class NetworkMonitor(context: Context) {
    private val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager
    private val _online = MutableStateFlow(context.currentOnline())
    val online: StateFlow<Boolean> = _online.asStateFlow()

    private val callback = object : ConnectivityManager.NetworkCallback() {
        override fun onAvailable(network: Network) {
            _online.value = true
        }

        override fun onLost(network: Network) {
            _online.value = false
        }

        override fun onCapabilitiesChanged(network: Network, caps: NetworkCapabilities) {
            _online.value = caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) &&
                caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)
        }
    }

    fun register() {
        val cm = this.cm ?: return
        runCatching { cm.registerDefaultNetworkCallback(callback) }
    }

    fun unregister() {
        val cm = this.cm ?: return
        runCatching { cm.unregisterNetworkCallback(callback) }
    }
}

/** Composable that stays in sync with connectivity for the current screen. */
@Composable
fun rememberIsOnline(): State<Boolean> {
    val context = LocalContext.current.applicationContext
    val monitor = remember { NetworkMonitor(context) }
    DisposableEffect(Unit) {
        monitor.register()
        onDispose { monitor.unregister() }
    }
    return monitor.online.collectAsState()
}
