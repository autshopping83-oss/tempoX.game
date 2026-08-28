package cloud.bizflow.tempox.game

import android.content.Context
import android.content.SharedPreferences

/** Match formats. ARCADE mixes every drill; MATH/SHAPE are specialized and locked. */
enum class GameMode { ARCADE, MATH, SHAPE }

/** Immutable snapshot of the player's progression wallet. */
data class EconomyState(
    val coins: Int,
    val unlockedModes: Set<GameMode>,
)

/**
 * Persistent progression currency backed by SharedPreferences.
 * Score (per match) and coins (permanent) are deliberately separate:
 * coins buy access to the specialized modes or are earned there faster.
 *
 * [coins] and [unlocked] are cached in memory — SharedPreferences is only
 * read once at construction; writes go through the editor AND update the
 * in-memory cache so subsequent reads within the same session never touch
 * disk again.
 */
class EconomyRepository(context: Context) {

    private val prefs: SharedPreferences =
        context.getSharedPreferences("temprox_economy", Context.MODE_PRIVATE)

    // In-memory cache — avoids repeated disk reads within a single session.
    private var coins: Int = prefs.getInt(K_COINS, 0)
    private var unlocked: Set<GameMode> = prefs.getStringSet(K_UNLOCKED, emptySet())
        ?.mapNotNull { runCatching { GameMode.valueOf(it) }.getOrNull() }
        ?.toSet()
        ?: emptySet()

    fun load(): EconomyState = EconomyState(coins = coins, unlockedModes = unlocked)

    /** Credit coins earned by a finished match (or any other source). */
    fun addCoins(amount: Int) {
        if (amount <= 0) return
        coins += amount
        prefs.edit().putInt(K_COINS, coins).apply()
    }

    /**
     * Atomic offline purchase path — always available, no network required.
     * @return true when the balance covered the spend.
     */
    fun trySpendCoins(amount: Int): Boolean {
        if (coins < amount) return false
        coins -= amount
        prefs.edit().putInt(K_COINS, coins).apply()
        return true
    }

    fun unlockMode(mode: GameMode) {
        unlocked = unlocked + mode
        prefs.edit().putStringSet(K_UNLOCKED, unlocked.map { it.name }.toSet()).apply()
    }

    fun isUnlocked(mode: GameMode): Boolean =
        mode == GameMode.ARCADE || mode in unlocked

    companion object {
        const val UNLOCK_COST = 750
        const val RECOVERY_COST = 10
        private const val K_COINS = "coins"
        private const val K_UNLOCKED = "unlockedModes"
    }
}
