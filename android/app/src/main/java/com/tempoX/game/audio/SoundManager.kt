package com.tempoX.game.audio

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioManager
import android.media.SoundPool
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import com.tempoX.game.R

/**
 * Native TEMPOX audio engine.
 *
 *  - All nine SFX are premium CC0 WAVs (Kenney interface/jingles packs,
 *    post-processed to 44.1kHz mono 16-bit) bundled in res/raw and loaded
 *    ONCE into a SoundPool (low-latency, USAGE_GAME) at app start.
 *  - Effects are muted while the ringer is in silent/vibrate mode.
 *  - Volume [0..1] is user-configurable (pause menu slider) and persisted.
 *  - Haptic feedback via Vibrator with graceful API fallbacks.
 */
object SoundManager {

    enum class Sfx { CLICK, CONFIRM, CORRECT, WRONG, COMBO, TICK, RECORD, TROPHY, GAME_OVER }

    private var soundPool: SoundPool? = null
    private var audioManager: AudioManager? = null
    private var vibrator: Vibrator? = null
    private var prefs: android.content.SharedPreferences? = null

    private val sampleIds = mutableMapOf<Sfx, Int>()
    private val loaded = mutableSetOf<Int>()

    @Volatile private var enabled: Boolean = true
    @Volatile private var hapticsEnabled: Boolean = true
    @Volatile private var volume: Float = 0.8f

    /** Call once from Application/Activity onCreate. */
    fun init(context: Context) {
        if (soundPool != null) return
        val app = context.applicationContext
        prefs = app.getSharedPreferences("temprox_settings", Context.MODE_PRIVATE)
        enabled = prefs!!.getBoolean("sound_enabled", true)
        hapticsEnabled = prefs!!.getBoolean("vibration_enabled", true)
        volume = prefs!!.getFloat("volume", 0.8f)

        audioManager = app.getSystemService(Context.AUDIO_SERVICE) as AudioManager
        vibrator = app.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator

        val attrs = AudioAttributes.Builder()
            .setUsage(AudioAttributes.USAGE_GAME)
            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
            .build()
        val pool = SoundPool.Builder().setMaxStreams(6).setAudioAttributes(attrs).build()
        pool.setOnLoadCompleteListener { _, sampleId, status ->
            if (status == 0) loaded += sampleId
        }
        soundPool = pool

        sampleIds[Sfx.CLICK] = pool.load(app, R.raw.click, 1)
        sampleIds[Sfx.CONFIRM] = pool.load(app, R.raw.confirm, 1)
        sampleIds[Sfx.CORRECT] = pool.load(app, R.raw.correct, 1)
        sampleIds[Sfx.WRONG] = pool.load(app, R.raw.wrong, 1)
        sampleIds[Sfx.COMBO] = pool.load(app, R.raw.combo, 1)
        sampleIds[Sfx.TICK] = pool.load(app, R.raw.tick, 1)
        sampleIds[Sfx.RECORD] = pool.load(app, R.raw.record, 1)
        sampleIds[Sfx.TROPHY] = pool.load(app, R.raw.trophy, 1)
        sampleIds[Sfx.GAME_OVER] = pool.load(app, R.raw.game_over, 1)
    }

    /** Silent mode (RINGER_MODE_SILENT / VIBRATE) mutes game sounds. */
    private fun ringerSilent(): Boolean =
        audioManager?.ringerMode != AudioManager.RINGER_MODE_NORMAL

    fun play(sfx: Sfx) {
        val id = sampleIds[sfx] ?: return
        if (!enabled || ringerSilent() || id !in loaded) return
        val v = volume.coerceIn(0f, 1f)
        soundPool?.play(id, v, v, 1, 0, 1f)
    }

    fun setEnabled(value: Boolean) {
        enabled = value
        prefs?.edit()?.putBoolean("sound_enabled", value)?.apply()
    }

    fun setHapticsEnabled(value: Boolean) {
        hapticsEnabled = value
        prefs?.edit()?.putBoolean("vibration_enabled", value)?.apply()
    }

    fun setVolume(value: Float) {
        volume = value.coerceIn(0f, 1f)
        prefs?.edit()?.putFloat("volume", volume)?.apply()
    }

    fun isEnabled() = enabled
    fun isHapticsEnabled() = hapticsEnabled
    fun getVolume() = volume

    // ------------------------------------------------------------------
    // Haptics
    // ------------------------------------------------------------------

    fun vibrate(pattern: LongArray = longArrayOf(0, 40)) {
        if (!hapticsEnabled) return
        val vib = vibrator ?: return
        when {
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.O ->
                vib.vibrate(VibrationEffect.createWaveform(pattern, -1))
            else -> @Suppress("DEPRECATION") vib.vibrate(pattern, -1)
        }
    }
}
