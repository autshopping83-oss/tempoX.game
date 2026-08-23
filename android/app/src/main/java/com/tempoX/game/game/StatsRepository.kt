package com.tempoX.game.game

import android.content.Context
import android.content.SharedPreferences
import com.tempoX.game.R

/** Achievement ids — stable keys also used by the string resources. */
object Achievements {
    const val ELEPHANT = "elefante"
    const val REFLEX = "reflexo"
    const val UNSTOPPABLE = "imparavel"
    const val SURVIVOR = "sobrevivente"
    const val RECORDIST = "recordista"

    val ALL = listOf(ELEPHANT, REFLEX, UNSTOPPABLE, SURVIVOR, RECORDIST)

    fun titleRes(id: String): Int = when (id) {
        ELEPHANT -> R.string.ach_elephant_title
        REFLEX -> R.string.ach_reflex_title
        UNSTOPPABLE -> R.string.ach_unstoppable_title
        SURVIVOR -> R.string.ach_survivor_title
        else -> R.string.ach_recordist_title
    }

    fun descRes(id: String): Int = when (id) {
        ELEPHANT -> R.string.ach_elephant_desc
        REFLEX -> R.string.ach_reflex_desc
        UNSTOPPABLE -> R.string.ach_unstoppable_desc
        SURVIVOR -> R.string.ach_survivor_desc
        else -> R.string.ach_recordist_desc
    }
}

/** Immutable snapshot of the player's lifetime stats. */
data class PlayerStats(
    val highScore: Int,
    val totalXp: Int,
    val gamesPlayed: Int,
    val totalCorrect: Int,
    val totalIncorrect: Int,
    val maxComboEver: Int,
    val maxMemorySequence: Int,
    val perfectReflexCount: Int,
    val achievements: Set<String>,
)

/**
 * Lifetime persistence backed by SharedPreferences.
 * Replaces the web build's localStorage key `60s_game_stats`.
 */
class StatsRepository(context: Context) {

    private val prefs: SharedPreferences =
        context.getSharedPreferences("temprox_stats", Context.MODE_PRIVATE)

    fun load(): PlayerStats = PlayerStats(
        highScore = prefs.getInt(K_HIGH_SCORE, 0),
        totalXp = prefs.getInt(K_TOTAL_XP, 0),
        gamesPlayed = prefs.getInt(K_GAMES, 0),
        totalCorrect = prefs.getInt(K_CORRECT, 0),
        totalIncorrect = prefs.getInt(K_INCORRECT, 0),
        maxComboEver = prefs.getInt(K_MAX_COMBO, 0),
        maxMemorySequence = prefs.getInt(K_MAX_MEM, 0),
        perfectReflexCount = prefs.getInt(K_PERFECT_REFLEX, 0),
        achievements = prefs.getStringSet(K_ACH, emptySet()) ?: emptySet(),
    )

    /**
     * Merge a finished match into lifetime stats.
     * @return ids of trophies unlocked by THIS match (for the celebration UI).
     */
    fun commitMatch(summary: MatchSummary, engine: GameEngine): List<String> {
        val old = load()
        val unlocked = mutableListOf<String>()

        val newHigh = summary.score > old.highScore
        if (newHigh && old.highScore > 0 && Achievements.RECORDIST !in old.achievements) {
            unlocked += Achievements.RECORDIST
        }
        if (engine.maxMemorySequence >= 7 && Achievements.ELEPHANT !in old.achievements) {
            unlocked += Achievements.ELEPHANT
        }
        if (engine.perfectReflexCount >= 1 && Achievements.REFLEX !in old.achievements) {
            unlocked += Achievements.REFLEX
        }
        if (summary.maxCombo >= 10 && Achievements.UNSTOPPABLE !in old.achievements) {
            unlocked += Achievements.UNSTOPPABLE
        }
        if (summary.completed && Achievements.SURVIVOR !in old.achievements) {
            unlocked += Achievements.SURVIVOR
        }

        prefs.edit()
            .putInt(K_HIGH_SCORE, maxOf(old.highScore, summary.score))
            .putInt(K_TOTAL_XP, old.totalXp + summary.xpGained)
            .putInt(K_GAMES, old.gamesPlayed + 1)
            .putInt(K_CORRECT, old.totalCorrect + summary.totalCorrect)
            .putInt(K_INCORRECT, old.totalIncorrect + summary.totalIncorrect)
            .putInt(K_MAX_COMBO, maxOf(old.maxComboEver, summary.maxCombo))
            .putInt(K_MAX_MEM, maxOf(old.maxMemorySequence, engine.maxMemorySequence))
            .putInt(K_PERFECT_REFLEX, old.perfectReflexCount + engine.perfectReflexCount)
            .putStringSet(K_ACH, (old.achievements + unlocked).toSet())
            .apply()
        return unlocked
    }

    companion object {
        private const val K_HIGH_SCORE = "highScore"
        private const val K_TOTAL_XP = "totalXP"
        private const val K_GAMES = "gamesPlayed"
        private const val K_CORRECT = "totalCorrect"
        private const val K_INCORRECT = "totalIncorrect"
        private const val K_MAX_COMBO = "maxCombo"
        private const val K_MAX_MEM = "maxMemorySequence"
        private const val K_PERFECT_REFLEX = "perfectReflexCount"
        private const val K_ACH = "achievements"
    }
}
