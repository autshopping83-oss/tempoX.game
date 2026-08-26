package cloud.bizflow.tempox.game

import android.content.Context
import android.content.SharedPreferences
import java.util.Locale

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

    /** Wrap [context] so Compose stringResource() resolves in the chosen language. */
    fun wrap(context: Context, mode: LangMode): Context {
        if (mode == LangMode.SYSTEM) return context
        val locale = when (mode) {
            LangMode.PT_BR -> Locale.forLanguageTag("pt-BR")
            LangMode.PT_PT -> Locale.forLanguageTag("pt-PT")
            LangMode.EN -> Locale.ENGLISH
            else -> return context
        }
        Locale.setDefault(locale)
        val config = android.content.res.Configuration(context.resources.configuration)
        config.setLocale(locale)
        config.setLocales(android.os.LocaleList(locale))
        return context.createConfigurationContext(config)
    }

    fun next(mode: LangMode): LangMode = when (mode) {
        LangMode.SYSTEM -> LangMode.PT_BR
        LangMode.PT_BR -> LangMode.PT_PT
        LangMode.PT_PT -> LangMode.EN
        LangMode.EN -> LangMode.SYSTEM
    }
}
