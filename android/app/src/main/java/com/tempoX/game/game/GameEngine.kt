package com.tempoX.game.game

import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt
import kotlin.random.Random

/** Summary handed to the result screen when the 60 seconds run out. */
data class MatchSummary(
    val score: Int,
    val totalCorrect: Int,
    val totalIncorrect: Int,
    val maxCombo: Int,
    val xpGained: Int,
    val completed: Boolean,
)

/** The four TEMPOX challenge types. */
enum class ChallengeType { MEMORY, REFLEX, MATH, ATTENTION }

/** A single quick-test presented during a match. */
sealed class Challenge(val type: ChallengeType, val limitMillis: Long) {
    /** Watch a flashing sequence, then tap the shapes in order. */
    class Memory(val sequence: List<Int>) : Challenge(
        ChallengeType.MEMORY,
        limitMillis = 8000L + sequence.size * 400L // answer window (watch phase is untimed)
    )

    /** Tap the golden target fast; avoid red decoys. */
    class Reflex(val targetCell: Int, val decoyCells: List<Int>, val cols: Int, val rows: Int, limit: Long) :
        Challenge(ChallengeType.REFLEX, limit)

    /** Multiple-choice equation. */
    class Math(val question: String, val options: List<Int>, val correctIndex: Int, limit: Long) :
        Challenge(ChallengeType.MATH, limit)

    /** Grid of identical symbols with one odd-one-out. */
    class Attention(
        val cols: Int, val rows: Int, val oddIndex: Int,
        val baseSymbol: String, val oddSymbol: String, limit: Long
    ) : Challenge(ChallengeType.ATTENTION, limit)
}

object Progression {
    const val MATCH_MILLIS = 60_000L

    fun xpForLevel(level: Int): Int =
        if (level <= 1) 0 else (150.0 * Math.pow(level - 1.0, 1.6)).roundToInt()

    fun levelForXp(totalXp: Int): Int {
        var lvl = 1
        while (xpForLevel(lvl + 1) <= totalXp) lvl++
        return lvl
    }
}

/**
 * Faithful native port of the TEMPOX game engine.
 *
 * Scoring rules (identical to the original):
 *   score += round(100 * s * c * d)
 *     s = level scale      = 1 + (difficultyLevel - 1) * 0.15
 *     c = speed bonus      = max(1, 2.5 - elapsedFraction * 1.5)
 *     d = combo multiplier = 1 + min(2, combo * 0.1)
 *   xp    += round(15 * s * (1 + combo * 0.05))
 * Difficulty rises one level every 5 correct answers (cap 10).
 */
class GameEngine(private val seed: Long = System.currentTimeMillis()) {

    private val rng = Random(seed)

    var timeLeftMillis: Long = Progression.MATCH_MILLIS
        private set

    var finished = false
        private set

    var difficultyLevel = 1
        private set
    var combo = 0
        private set
    var maxCombo = 0
        private set
    var totalCorrect = 0
        private set
    var totalIncorrect = 0
        private set
    var xpGained = 0
        private set
    var score = 0
        private set
    var maxMemorySequence = 0
        private set
    var perfectReflexCount = 0
        private set

    var challenge: Challenge = generate()
        private set
    var challengeElapsedMillis: Long = 0
        private set

    /** Events consumed by the UI layer to fire sounds/animations. */
    enum class Event { CORRECT, WRONG, COMBO_MILESTONE }

    var onEvent: ((Event) -> Unit)? = null

    private fun speedFactor(): Double {
        val frac = challengeElapsedMillis.toDouble() / challenge.limitMillis.coerceAtLeast(1)
        return max(1.0, 2.5 - frac * 1.5)
    }

    private fun levelScale(): Double = 1.0 + (difficultyLevel - 1) * 0.15

    /** Advance the global 60s clock. */
    fun tick(deltaMillis: Long) {
        if (finished) return
        timeLeftMillis = max(0L, timeLeftMillis - deltaMillis)
        if (timeLeftMillis == 0L) finish()
    }

    /** Advance the per-challenge clock (not called during Memory watch phase). */
    fun tickChallenge(deltaMillis: Long) {
        if (finished) return
        challengeElapsedMillis += deltaMillis
        if (challenge.limitMillis > 0 && challengeElapsedMillis >= challenge.limitMillis) {
            resolve(false, perfect = false, memoryLength = 0)
        }
    }

    /** Player answered the active challenge. */
    fun resolve(correct: Boolean, perfect: Boolean = false, memoryLength: Int = 0) {
        if (finished) return
        if (correct) {
            totalCorrect++
            combo++
            if (combo > maxCombo) maxCombo = combo
            if (memoryLength > maxMemorySequence) maxMemorySequence = memoryLength
            if (perfect) perfectReflexCount++

            val d = 1.0 + min(2.0, combo * 0.1)
            score += (100.0 * levelScale() * speedFactor() * d).roundToInt()
            xpGained += (15.0 * levelScale() * (1.0 + combo * 0.05)).roundToInt()

            onEvent?.invoke(if (combo >= 3 && combo % 3 == 0) Event.COMBO_MILESTONE else Event.CORRECT)
            difficultyLevel = min(10, 1 + totalCorrect / 5)
        } else {
            totalIncorrect++
            combo = 0
            onEvent?.invoke(Event.WRONG)
        }
        if (!finished && timeLeftMillis > 0) {
            challenge = generate()
            challengeElapsedMillis = 0
        }
    }

    fun finish() {
        finished = true
    }

    // ------------------------------------------------------------------
    // Challenge factory — sizes/timings scale with difficultyLevel.
    // ------------------------------------------------------------------

    private fun generate(): Challenge = when (rng.nextInt(4)) {
        0 -> genMemory()
        1 -> genReflex()
        2 -> genMath()
        else -> genAttention()
    }

    private fun genMemory(): Challenge.Memory =
        Challenge.Memory(List(min(9, 3 + (difficultyLevel + 1) / 2)) { rng.nextInt(6) })

    private fun genReflex(): Challenge.Reflex {
        val target = rng.nextInt(9)
        val decoys = (0 until 9).filter { it != target }.shuffled(rng)
            .take(min(3, 1 + difficultyLevel / 3))
        val limit = max(1200L, 2600L - 160L * difficultyLevel)
        return Challenge.Reflex(target, decoys, cols = 3, rows = 3, limit = limit)
    }

    private fun genMath(): Challenge.Math {
        val lv = difficultyLevel
        val question: String
        val answer: Int
        when {
            lv <= 2 -> {
                val a = rng.nextInt(19) + 1
                val b = rng.nextInt(19) + 1
                answer = a + b; question = "$a + $b"
            }
            lv <= 4 -> {
                val a = rng.nextInt(49) + 2
                if (rng.nextBoolean()) {
                    val b = rng.nextInt(a) + 1
                    answer = a - b; question = "$a − $b"
                } else {
                    val b = rng.nextInt(19) + 1
                    answer = a + b; question = "$a + $b"
                }
            }
            else -> {
                val a = (rng.nextInt(11) + 2).let { if (lv >= 7 && rng.nextBoolean()) it * 2 else it }
                val b = rng.nextInt(11) + 2
                answer = a * b; question = "$a × $b"
            }
        }
        val opts = sortedSetOf(answer)
        var guard = 0
        while (opts.size < 4 && guard++ < 60) {
            val delta = rng.nextInt(9) + 1
            opts.add(answer + if (rng.nextBoolean()) delta else -delta)
        }
        val shuffled = opts.toList().shuffled(rng)
        val limit = max(3000L, 6000L - 250L * lv)
        return Challenge.Math(question, shuffled, shuffled.indexOf(answer), limit)
    }

    private fun genAttention(): Challenge.Attention {
        val side = min(6, 3 + difficultyLevel / 2)
        val pairs = listOf(
            "🔺" to "🔻", "⭐" to "🌟", "🔵" to "🟣",
            "🟥" to "🟧", "🌙" to "🌛", "⚡" to "🔥"
        )
        val (base, odd) = pairs[rng.nextInt(pairs.size)]
        val limit = max(2000L, 4500L - 280L * difficultyLevel)
        return Challenge.Attention(side, side, rng.nextInt(side * side), base, odd, limit)
    }
}
