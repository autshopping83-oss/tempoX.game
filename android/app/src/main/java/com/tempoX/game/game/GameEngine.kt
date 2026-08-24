package com.tempoX.game.game

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
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
    val coinsEarned: Int = 0,
)

/** The four TEMPOX challenge types. */
enum class ChallengeType { MEMORY, REFLEX, MATH, ATTENTION }

/** One visual-search element: unique id plus indices into the shape/color palettes. */
data class ReflexCell(val id: Long, val shape: Int, val color: Int)

/** Procedural odd-one-out mutation. kind: 0=rotation deg, 1=scale factor, 2=alpha, 3=offset dp. */
data class AttentionMut(val kind: Int, val amount: Float)

/** One attention-grid cell; identity rides on uid, oddness on the flag — never on position. */
data class AttentionSlot(val uid: Long, val odd: Boolean)

/** Figure pool for the attention challenge (existing repository, flattened). */
private val ATTENTION_FIGURES =
    listOf("🔺", "🔻", "⭐", "🌟", "🔵", "🟣", "🟥", "🟧", "🌙", "🌛", "⚡", "🔥")

/**
 * Glyphs whose silhouette makes any tilt read clearly as a difference.
 * Symmetric shapes (circles, squares, stars) would turn a rotation mutation
 * into an invisible change — an unsolvable puzzle — so they are excluded.
 */
private val ROTATION_VISIBLE = setOf("🔺", "🔻", "🌙", "🌛", "⚡")

/** A single quick-test presented during a match. */
sealed class Challenge(val type: ChallengeType, val limitMillis: Long) {
    /** Watch a flashing sequence, then tap the shapes in order. */
    class Memory(val sequence: List<Int>, limit: Long = 0L) : Challenge(
        ChallengeType.MEMORY,
        // Default answer window (watch phase is untimed); Shape Lab passes a
        // tighter survival budget scaled by sequence length.
        limitMillis = if (limit > 0L) limit else 8000L + sequence.size * 400L
    )

    /** Visual-search grid: exactly one cell matches the instructed shape+color. */
    class Reflex(
        val cells: List<ReflexCell>,
        val targetId: Long,
        val cols: Int,
        limit: Long,
    ) : Challenge(ChallengeType.REFLEX, limit) {
        private val target get() = cells.first { it.id == targetId }
        val targetShape: Int get() = target.shape
        val targetColor: Int get() = target.color
    }

    /** Multiple-choice equation. */
    class Math(val question: String, val options: List<Int>, val correctIndex: Int, limit: Long) :
        Challenge(ChallengeType.MATH, limit)

    /** Grid of identical figures where ONE carries a subtle procedural mutation. */
    class Attention(
        val cols: Int,
        val slots: List<AttentionSlot>,
        val symbol: String,
        val mut: AttentionMut,
        limit: Long,
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
class GameEngine(private val seed: Long = System.currentTimeMillis(), val mode: GameMode = GameMode.ARCADE) {

    private val rng = Random(seed) // wall-clock seeded per match — never a static seed

    /** Monotonic element ids handed to the UI so grids never identify items by index. */
    private var uidSeq = 0L
    private fun nextUid(): Long = ++uidSeq

    /** Last grid spot taken by each challenge type's target (anti-repeat guard). */
    private val lastTargetSpot = HashMap<ChallengeType, Int>()

    /**
     * Global position shuffler: every emitted round grid passes through here so
     * element placement is re-randomized each round and the target never sits
     * on the same spot two rounds of the same type in a row.
     */
    private fun <T> List<T>.toShuffledGrid(type: ChallengeType, isTarget: (T) -> Boolean): List<T> {
        var out = shuffled(rng)
        var tries = 0
        while (tries++ < 8 && out.indexOfFirst(isTarget) == lastTargetSpot[type]) {
            out = shuffled(rng)
        }
        lastTargetSpot[type] = out.indexOfFirst(isTarget)
        return out
    }

    // All gameplay fields are Compose-observable so the UI recomposes
    // every time the tick loop advances the match.
    var timeLeftMillis: Long by mutableStateOf(Progression.MATCH_MILLIS)
        private set

    var finished: Boolean by mutableStateOf(false)
        private set

    var difficultyLevel: Int by mutableStateOf(1)
        private set
    var combo: Int by mutableStateOf(0)
        private set
    var maxCombo: Int by mutableStateOf(0)
        private set
    var totalCorrect: Int by mutableStateOf(0)
        private set
    var totalIncorrect: Int by mutableStateOf(0)
        private set
    var xpGained: Int by mutableStateOf(0)
        private set
    var score: Int by mutableStateOf(0)
        private set
    var maxMemorySequence: Int by mutableStateOf(0)
        private set
    var perfectReflexCount: Int by mutableStateOf(0)
        private set

    var challenge: Challenge by mutableStateOf(generate())
        private set
    var challengeElapsedMillis: Long by mutableStateOf(0L)
        private set

    /** Progression wallet earned THIS match (committed to EconomyRepository at the end). */
    var sessionCoins: Int by mutableStateOf(0)
        private set

    /** True while the recovery modal is up: every clock is frozen. */
    var awaitingRecovery: Boolean by mutableStateOf(false)
        private set

    /** One simulated-ad recovery per match; also doubles the coins earned so far. */
    var recoveryUsed: Boolean by mutableStateOf(false)
        private set

    /** HUD feedback hooks for score penalties (floating red "-N"). */
    var penaltyFlashKey: Int by mutableStateOf(0)
        private set
    var lastPenalty: Int by mutableStateOf(0)
        private set

    /** Speed Math AND Shape Lab run on per-round clocks — the 60s global timer is Arcade-only. */
    val survival: Boolean get() = mode != GameMode.ARCADE

    private fun isSpecialized(): Boolean = mode != GameMode.ARCADE

    /**
     * Visual Difficulty Scaling for Shape Lab survival rounds: the round
     * budget shrinks as the hit streak grows (3.5s -> 3.0s -> 2.5s), while
     * grid density keeps rising via difficultyLevel.
     */
    private fun shapeRoundLimit(): Long = when {
        combo < 6 -> 3500L
        combo < 12 -> 3000L
        else -> 2500L
    }

    /** Events consumed by the UI layer to fire sounds/animations. */
    enum class Event { CORRECT, WRONG, COMBO_MILESTONE }

    var onEvent: ((Event) -> Unit)? = null

    private fun levelScale(): Double = 1.0 + (difficultyLevel - 1) * 0.15

    /** Advance the global 60s clock. */
    /** Advance the global 60s clock (off in Survival Speed Math). */
    fun tick(deltaMillis: Long) {
        if (finished || awaitingRecovery || survival) return
        timeLeftMillis = max(0L, timeLeftMillis - deltaMillis)
        if (timeLeftMillis == 0L) finish()
    }

    /** Advance the per-challenge clock (not called during Memory watch phase). */
    fun tickChallenge(deltaMillis: Long) {
        if (finished || awaitingRecovery) return
        challengeElapsedMillis += deltaMillis
        if (challenge.limitMillis > 0 && challengeElapsedMillis >= challenge.limitMillis) {
            resolve(false, perfect = false, memoryLength = 0)
        }
    }

    /**
     * Player answered the active challenge. Single funnel for taps AND round
     * timeouts — main-thread confined, so the timeout/tap race is impossible:
     * the first call freezes the run before a second event can be observed.
     */
    fun resolve(correct: Boolean, perfect: Boolean = false, memoryLength: Int = 0) {
        if (finished || awaitingRecovery) return
        if (correct) {
            totalCorrect++
            combo++
            if (combo > maxCombo) maxCombo = combo
            if (memoryLength > maxMemorySequence) maxMemorySequence = memoryLength
            if (perfect) perfectReflexCount++

            // Asymmetric economy: Arcade pays little but forgives; specialized
            // modes pay per-combo and cost more to fail.
            if (isSpecialized()) {
                val d = 1.0 + min(2.0, combo * 0.1)
                score += (25.0 * d).roundToInt()
                sessionCoins += 2
            } else {
                score += 10
                sessionCoins += 1
            }
            xpGained += (15.0 * levelScale() * (1.0 + combo * 0.05)).roundToInt()

            onEvent?.invoke(if (combo >= 3 && combo % 3 == 0) Event.COMBO_MILESTONE else Event.CORRECT)
            difficultyLevel = min(10, 1 + totalCorrect / 5)
        } else {
            totalIncorrect++
            combo = 0
            lastPenalty = if (isSpecialized()) 10 else 5
            score = max(0, score - lastPenalty)
            penaltyFlashKey++
            onEvent?.invoke(Event.WRONG)
            if (survival) enterRecoveryOrFail()
        }
        if (!finished && !awaitingRecovery && timeLeftMillis > 0) {
            challenge = generate()
            challengeElapsedMillis = 0
        }
    }

    /** Survival Speed Math / Shape Lab: first failure offers one ad revival; after that, game over. */
    private fun enterRecoveryOrFail() {
        if (!recoveryUsed) awaitingRecovery = true else finish()
    }

    /**
     * Simulated rewarded-ad recovery: refreshes the round clock and doubles
     * the coins earned so far — once per run.
     */
    fun recoverWithAd() {
        if (!awaitingRecovery) return
        awaitingRecovery = false
        challengeElapsedMillis = 0 // fresh round bar
        recoveryUsed = true
        sessionCoins *= 2
    }

    fun giveUpRecovery() {
        if (awaitingRecovery) finish()
    }

    fun finish() {
        finished = true
    }

    // ------------------------------------------------------------------
    // Challenge factory — sizes/timings scale with difficultyLevel.
    // ------------------------------------------------------------------

    private fun generate(): Challenge = when (mode) {
        // Speed Math: strict equation-only stream.
        GameMode.MATH -> genMath()
        // Shape Lab: visual drills only (memory / search / attention).
        GameMode.SHAPE -> when (rng.nextInt(3)) {
            0 -> genMemory()
            1 -> genReflex()
            else -> genAttention()
        }
        // Arcade: the classic mixed 60s run.
        GameMode.ARCADE -> when (rng.nextInt(4)) {
            0 -> genMemory()
            1 -> genReflex()
            2 -> genMath()
            else -> genAttention()
        }
    }

    private fun genMemory(): Challenge.Memory {
        val seq = List(min(9, 3 + (difficultyLevel + 1) / 2)) { rng.nextInt(6) }
        // Sequence order is gameplay semantics — never shuffled. The answer
        // palette layout is re-shuffled UI-side on every new challenge.
        return Challenge.Memory(seq, if (mode == GameMode.SHAPE) shapeRoundLimit() + seq.size * 250 else 0)
    }

    private fun genReflex(): Challenge.Reflex {
        // Grid density scales with difficulty: 15 elements at level 1 -> 35 at cap.
        val count = min(35, 15 + (difficultyLevel - 1) * 3)
        val cols = if (count <= 20) 5 else if (count <= 30) 6 else 7

        // The single unique combo; distractors share EITHER its shape or its
        // color (never both), so it stays findable yet camouflaged in the noise.
        val ts = rng.nextInt(6); val tc = rng.nextInt(6)
        val target = ReflexCell(nextUid(), ts, tc)
        val sameShape = ReflexCell(nextUid(), ts, (tc + 1 + rng.nextInt(5)) % 6)
        val sameColor = ReflexCell(nextUid(), (ts + 1 + rng.nextInt(5)) % 6, tc)
        var wild = ReflexCell(nextUid(), rng.nextInt(6), rng.nextInt(6))
        while (wild.shape == target.shape && wild.color == target.color) {
            wild = ReflexCell(nextUid(), rng.nextInt(6), rng.nextInt(6))
        }
        val noise = listOf(sameShape, sameColor, wild)

        val cells = buildList {
            repeat(count - 1) { add(noise[it % noise.size]) }
            add(target)
        }.toShuffledGrid(ChallengeType.REFLEX) { it.id == target.id }
        // Visual-search pacing: base/floor run ~35-50% above the other drills
        // so the player gets time for instruction reading plus an eye sweep,
        // while the steeper per-level decay keeps the urgency alive.
        val limit =
            if (mode == GameMode.SHAPE) shapeRoundLimit()
            else max(1800L, 3600L - 200L * difficultyLevel)
        return Challenge.Reflex(cells, target.id, cols, limit)
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
        val options = opts.toList().toShuffledGrid(ChallengeType.MATH) { it == answer }
        val limit = max(3000L, 6000L - 250L * lv)
        return Challenge.Math(question, options, options.indexOf(answer), limit)
    }

    private fun genAttention(): Challenge.Attention {
        // Density grows 40 -> 100 cells; the mutation gets subtler as difficulty rises,
        // but every parameter keeps a visibility floor so the odd cell stays legible
        // on low-quality or night-mode-dimmed screens (contrast over subtlety).
        val count = min(100, 40 + (difficultyLevel - 1) * 7)
        val cols = if (count <= 56) 8 else if (count <= 81) 9 else 10
        val t = (difficultyLevel - 1) / 9f // 0 at lv1 -> 1 at lv10
        fun lerp(a: Float, b: Float) = a + (b - a) * t
        var mut = when (rng.nextInt(4)) {
            0 -> AttentionMut(0, lerp(18f, 10f)) // tilt >= 10deg, asymmetric glyphs only
            1 -> // scale within +/-10..14%
                if (rng.nextBoolean()) AttentionMut(1, lerp(1.14f, 1.10f))
                else AttentionMut(1, lerp(0.90f, 0.86f))
            2 -> AttentionMut(2, lerp(0.78f, 0.70f)) // always >= 20% dim
            else -> AttentionMut(3, lerp(5f, 3f)) // offset >= 3dp
        }
        val symbol = ATTENTION_FIGURES[rng.nextInt(ATTENTION_FIGURES.size)]
        if (mut.kind == 0 && symbol !in ROTATION_VISIBLE) {
            // A tilt on a symmetric glyph is invisible — force a fill-strength
            // difference instead so the round always has exactly one real answer.
            mut = AttentionMut(2, lerp(0.78f, 0.70f))
        }
        check(mut.kind != 0 || symbol in ROTATION_VISIBLE) {
            "Odd item must differ visibly from base items"
        }
        val oddAt = rng.nextInt(count)
        val slots = List(count) { AttentionSlot(nextUid(), it == oddAt) }
            .toShuffledGrid(ChallengeType.ATTENTION) { it.odd }
        check(slots.size == count && slots.count { it.odd } == 1) {
            "Attention grid must hold exactly $count base cells and exactly one odd item"
        }
        val limit =
            if (mode == GameMode.SHAPE) shapeRoundLimit()
            else max(2000L, 4500L - 280L * difficultyLevel)
        return Challenge.Attention(cols, slots, symbol, mut, limit)
    }
}
