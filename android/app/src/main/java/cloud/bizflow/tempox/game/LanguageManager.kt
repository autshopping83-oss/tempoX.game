package cloud.bizflow.tempox.game

import android.content.Context
import android.content.SharedPreferences
import java.util.Locale

/** User-selectable app language, independent from the OS locale when forced. */
enum class LangMode { SYSTEM, PT, EN }

object LanguageManager {

    private const val KEY = "app_language"

    private fun prefs(context: Context): SharedPreferences =
        context.getSharedPreferences("temprox_settings", Context.MODE_PRIVATE)

    fun load(context: Context): LangMode =
        when (prefs(context).getString(KEY, null)) {
            "pt" -> LangMode.PT
            "en" -> LangMode.EN
            else -> LangMode.SYSTEM
        }

    fun save(context: Context, mode: LangMode) {
        prefs(context).edit()
            .putString(KEY, when (mode) {
                LangMode.PT -> "pt"
                LangMode.EN -> "en"
                LangMode.SYSTEM -> null
            })
            .apply()
    }

    /** Wrap [context] so Compose stringResource() resolves in the chosen language. */
    fun wrap(context: Context, mode: LangMode): Context {
        if (mode == LangMode.SYSTEM) return context
        val locale = if (mode == LangMode.PT) Locale.forLanguageTag("pt-BR") else Locale.ENGLISH
        Locale.setDefault(locale)
        val config = android.content.res.Configuration(context.resources.configuration)
        config.setLocale(locale)
        config.setLocales(android.os.LocaleList(locale))
        return context.createConfigurationContext(config)
    }

    fun next(mode: LangMode): LangMode = when (mode) {
        LangMode.SYSTEM -> LangMode.PT
        LangMode.PT -> LangMode.EN
        LangMode.EN -> LangMode.SYSTEM
    }
}
