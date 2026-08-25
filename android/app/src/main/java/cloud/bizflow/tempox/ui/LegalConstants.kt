package cloud.bizflow.tempox.ui

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.util.Log
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
    if (!context.isOnline()) {
        Toast.makeText(context, R.string.legal_offline, Toast.LENGTH_SHORT).show()
        return
    }
    val url = when (type) {
        LegalType.PRIVACY -> LegalConstants.PRIVACY_POLICY_URL
        LegalType.TERMS -> LegalConstants.TERMS_OF_SERVICE_URL
    }
    launchWebUrl(context, url)
}

/**
 * Defensive connectivity probe. NEVER throws: a SecurityException (missing
 * ACCESS_NETWORK_STATE) or vendor-ROM quirks (MIUI's ConnectivityService)
 * fail OPEN so the click still reaches the browser, which natively handles
 * offline navigation. Only an explicitly unvalidated network reports false.
 */
fun Context.isOnline(): Boolean {
    return runCatching {
        val connectivityManager = getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager
        val network = connectivityManager?.activeNetwork ?: return@runCatching false
        val capabilities = connectivityManager.getNetworkCapabilities(network) ?: return@runCatching false

        capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) &&
            capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)
    }.getOrElse { exception ->
        // Em caso de erro de permissão ou falha de ROM, permite a tentativa de abertura
        Log.w("LegalConstants", "Falha ao checar estado da rede", exception)
        true
    }
}
