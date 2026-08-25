package cloud.bizflow.tempox.ui

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.widget.Toast
import cloud.bizflow.tempox.R
import cloud.bizflow.tempox.utils.launchWebUrl

/** Sanitized legal endpoints — never concatenate, always reference as-is. */
object LegalConstants {
    const val PRIVACY_POLICY_URL = "https://tempox.biz-flow.cloud/privacidade"
    const val TERMS_OF_SERVICE_URL = "https://tempox.biz-flow.cloud/termos"
}

enum class LegalType { PRIVACY, TERMS }

/**
 * Opens a legal page via [launchWebUrl] (Chrome Custom Tabs with fallback).
 * Checks connectivity first; offline users see a toast instead of a browser
 * error page.
 */
fun openLegal(context: Context, type: LegalType) {
    if (!isOnline(context)) {
        Toast.makeText(context, R.string.legal_offline, Toast.LENGTH_SHORT).show()
        return
    }
    val url = when (type) {
        LegalType.PRIVACY -> LegalConstants.PRIVACY_POLICY_URL
        LegalType.TERMS -> LegalConstants.TERMS_OF_SERVICE_URL
    }
    launchWebUrl(context, url)
}

private fun isOnline(context: Context): Boolean {
    val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager
        ?: return true
    val caps = cm.getNetworkCapabilities(cm.activeNetwork) ?: return false
    return caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
}
