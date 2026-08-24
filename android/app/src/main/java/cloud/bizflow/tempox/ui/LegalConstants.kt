package cloud.bizflow.tempox.ui

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.net.Uri
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
 * ACTION_VIEW when CCT is unavailable. Offline taps are ignored with a toast
 * instead of showing the browser's error page.
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
    val uri = Uri.parse(url)
    try {
        CustomTabsIntent.Builder()
            .setShowTitle(true)
            .build()
            .launchUrl(context, uri)
    } catch (_: Exception) {
        // No Custom Tabs provider — hand the URL to any browser.
        try {
            context.startActivity(Intent(Intent.ACTION_VIEW, uri))
        } catch (_: ActivityNotFoundException) {
            // Device without any browser: nothing safe to do.
        }
    }
}

private fun isOnline(context: Context): Boolean {
    val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager
        ?: return true // cannot verify — assume online and let the browser fail gracefully
    val caps = cm.getNetworkCapabilities(cm.activeNetwork) ?: return false
    return caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
}
