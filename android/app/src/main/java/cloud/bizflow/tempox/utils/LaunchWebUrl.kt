package cloud.bizflow.tempox.utils

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.util.Log
import android.widget.Toast
import androidx.browser.customtabs.CustomTabsIntent
import cloud.bizflow.tempox.R

private const val TAG = "LaunchWebUrl"

/**
 * Opens a URL safely via Chrome Custom Tabs, falling back to any browser
 * through ACTION_VIEW when CCT is unavailable. Every path is exception-
 * guarded so a missing browser or bad context can never crash the app
 * (Android Vitals safety).
 *
 * - Automatically prefixes [rawUrl] with `https://` when no scheme is present.
 * - Uses [CustomTabsIntent] to keep the game Activity alive in RAM.
 * - Catches [Exception] at both layers and shows a user-friendly [Toast].
 */
fun launchWebUrl(context: Context, rawUrl: String) {
    if (rawUrl.isBlank()) return

    val formattedUrl = if (!rawUrl.startsWith("http://") && !rawUrl.startsWith("https://")) {
        "https://$rawUrl"
    } else {
        rawUrl
    }

    val uri = Uri.parse(formattedUrl)

    // Primary path — Chrome Custom Tabs (overlay, game stays in memory)
    runCatching {
        val customTabsIntent = CustomTabsIntent.Builder()
            .setShowTitle(true)
            .build()
        customTabsIntent.intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        customTabsIntent.launchUrl(context, uri)
    }.onFailure { cctError ->
        Log.w(TAG, "Custom Tabs failed (${cctError.message}); falling back to ACTION_VIEW")
        // Fallback — any installed browser
        runCatching {
            val intent = Intent(Intent.ACTION_VIEW, uri).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
        }.onFailure { fallbackError ->
            Log.e(TAG, "No browser available: ${fallbackError.message}")
            Toast.makeText(
                context,
                context.getString(R.string.launch_web_unavailable),
                Toast.LENGTH_SHORT,
            ).show()
        }
    }
}
