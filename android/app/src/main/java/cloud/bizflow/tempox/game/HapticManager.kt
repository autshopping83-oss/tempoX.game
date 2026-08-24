package cloud.bizflow.tempox.game

import android.content.Context
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager

/**
 * Central native haptics for TEMPOX.
 *
 * - VibratorManager on Android 12+ (S), legacy Vibrator below.
 * - Millimetric gamefeel profiles: click 30ms, error double pulse,
 *   win rising triple, game-over long 200ms — never longer than needed
 *   so reflex play stays crisp and battery-friendly.
 * - Respects the user toggle persisted under `vibration_enabled`
 *   (same key the settings UI already uses) and swallows OS-level
 *   blocks (Do Not Disturb / battery saver / missing hardware).
 */
object HapticManager {

    private var vibrator: Vibrator? = null
    private var prefs: android.content.SharedPreferences? = null

    @Volatile private var enabled = true

    fun init(context: Context) {
        if (vibrator != null) return
        val app = context.applicationContext
        prefs = app.getSharedPreferences("temprox_settings", Context.MODE_PRIVATE)
        enabled = prefs!!.getBoolean("vibration_enabled", true)
        vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            (app.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager)?.defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            app.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
        }
    }

    fun isEnabled(): Boolean = enabled

    fun setEnabled(value: Boolean) {
        enabled = value
        // Same key the settings UI has always used — single source of truth.
        prefs?.edit()?.putBoolean("vibration_enabled", value)?.apply()
    }

    /** Short soft tick — correct taps and quick combos. */
    fun click() = oneShot(30)

    /** Double short pulse — wrong answer alert. */
    fun error() = waveform(longArrayOf(0, 50, 60, 50))

    /** Rising triple pulse — combo milestone / record celebration. */
    fun win() = waveform(longArrayOf(0, 40, 70, 40, 70, 90))

    /** One long pulse — match over / time up. */
    fun gameOver() = oneShot(200)

    /** Generic escape hatch preserving legacy call-site patterns. */
    fun pattern(pattern: LongArray) = waveform(pattern)

    private fun oneShot(millis: Long) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            vibrateCompat(VibrationEffect.createOneShot(millis, VibrationEffect.DEFAULT_AMPLITUDE))
        } else {
            @Suppress("DEPRECATION")
            rawVibrate(millis)
        }
    }

    private fun waveform(timings: LongArray) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            vibrateCompat(VibrationEffect.createWaveform(timings, -1))
        } else {
            @Suppress("DEPRECATION")
            rawVibrate(if (timings.isEmpty()) 40 else timings.last().coerceAtMost(120))
        }
    }

    private fun vibrateCompat(effect: VibrationEffect) {
        val vib = vibrator ?: return
        try {
            if (!vib.hasVibrator()) return // hardware without motor or SO-blocked
            vib.vibrate(effect)
        } catch (_: Exception) {
            // DND/battery-saver restrictions must never crash gameplay.
        }
    }

    @Suppress("DEPRECATION")
    private fun rawVibrate(millis: Long) {
        val vib = vibrator ?: return
        try {
            if (!vib.hasVibrator()) return
            vib.vibrate(millis)
        } catch (_: Exception) {
        }
    }
}
