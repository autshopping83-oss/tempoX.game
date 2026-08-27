package cloud.bizflow.tempox.game

import android.content.Context
import android.content.SharedPreferences

/** User-selectable app language, independent from the OS locale when forced. */
enum class LangMode { SYSTEM, PT_BR, PT_PT, EN }

object LanguageManager {

    private const val KEY = "app_language"

    private fun prefs(context: Context): SharedPreferences =
        context.getSharedPreferences("temprox_settings", Context.MODE_PRIVATE)

    fun load(context: Context): LangMode =
        when (prefs(context).getString(KEY, null)) {
            "pt-BR" -> LangMode.PT_BR
            "pt-PT" -> LangMode.PT_PT
            "en" -> LangMode.EN
            else -> LangMode.SYSTEM
        }

    fun save(context: Context, mode: LangMode) {
        prefs(context).edit()
            .putString(KEY, when (mode) {
                LangMode.PT_BR -> "pt-BR"
                LangMode.PT_PT -> "pt-PT"
                LangMode.EN -> "en"
                LangMode.SYSTEM -> null
            })
            .commit()
    }
}
