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
 */
class EconomyRepository(context: Context) {

    private val prefs: SharedPreferences =
        context.getSharedPreferences("temprox_economy", Context.MODE_PRIVATE)

    fun load(): EconomyState = EconomyState(
        coins = prefs.getInt(K_COINS, 0),
        unlockedModes = prefs.getStringSet(K_UNLOCKED, emptySet())
            ?.mapNotNull { runCatching { GameMode.valueOf(it) }.getOrNull() }
            ?.toSet()
            ?: emptySet(),
    )

    /** Credit coins earned by a finished match (or any other source). */
    fun addCoins(amount: Int) {
        if (amount <= 0) return
        prefs.edit().putInt(K_COINS, load().coins + amount).apply()
    }

    /**
     * Atomic offline purchase path — always available, no network required.
     * @return true when the balance covered the spend.
     */
    fun trySpendCoins(amount: Int): Boolean {
        val balance = load().coins
        if (balance < amount) return false
        prefs.edit().putInt(K_COINS, balance - amount).apply()
        return true
    }

    fun unlockMode(mode: GameMode) {
        val current = load().unlockedModes
        prefs.edit().putStringSet(K_UNLOCKED, (current + mode).map { it.name }.toSet()).apply()
    }

    fun isUnlocked(mode: GameMode): Boolean =
        mode == GameMode.ARCADE || mode in load().unlockedModes

    companion object {
        const val UNLOCK_COST = 150
        private const val K_COINS = "coins"
        private const val K_UNLOCKED = "unlockedModes"
    }
}
