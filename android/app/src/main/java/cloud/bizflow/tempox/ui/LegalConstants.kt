package cloud.bizflow.tempox.ui

import android.content.Context
import android.content.Intent
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.net.Uri
import android.util.Log
import android.widget.Toast
import androidx.browser.customtabs.CustomTabsIntent
import cloud.bizflow.tempox.R

/** Sanitized legal endpoints — never concatenate, always reference as-is. */
object LegalConstants {
    const val PRIVACY_POLICY_URL = "https://tempox.biz-flow.cloud/privacidade"
    const val TERMS_OF_SERVICE_URL = "https://tempox.biz-flow.cloud/termos"
}

enum class LegalType { PRIVACY, TERMS }

/**
 * Opens a legal page in Chrome Custom Tabs, falling back to any browser via
 * ACTION_VIEW when CCT is unavailable. Every hop is exception-guarded so a
 * missing browser or context isolation can never crash the app (Android
 * Vitals safety). Offline taps get a toast instead of a browser error page.
 */
fun openLegal(context: Context, type: LegalType) {
    val url = when (type) {
        LegalType.PRIVACY -> LegalConstants.PRIVACY_POLICY_URL
        LegalType.TERMS -> LegalConstants.TERMS_OF_SERVICE_URL
    }
    if (!isOnline(context)) {
        Toast.makeText(context, R.string.legal_offline, Toast.LENGTH_SHORT).show()
        return
    }
    if (url.isBlank()) return
    val uri = Uri.parse(url)
    try {
        val intent = CustomTabsIntent.Builder()
            .setShowTitle(true)
            .build()
        // Safe even if the caller ever passes an application context.
        intent.intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        intent.launchUrl(context, uri)
    } catch (e: Exception) {
        Log.e(TAG, "Custom Tabs failed (${e.message}); falling back to ACTION_VIEW")
        try {
            context.startActivity(
                Intent(Intent.ACTION_VIEW, uri).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
            )
        } catch (fallback: Exception) {
            Log.e(TAG, "No browser available: ${fallback.message}")
            Toast.makeText(context, R.string.legal_no_browser, Toast.LENGTH_SHORT).show()
        }
    }
}

private const val TAG = "LegalNavigation"

private fun isOnline(context: Context): Boolean {
    val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager
        ?: return true // cannot verify — assume online and let the guards handle it
    val caps = cm.getNetworkCapabilities(cm.activeNetwork) ?: return false
    return caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
}
