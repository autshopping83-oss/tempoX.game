package cloud.bizflow.tempox.game

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch

/**
 * Lookahead Session Queue: pre-generates and validates upcoming rounds on a
 * serialized background dispatcher so the UI never computes layouts at tap time.
 *
 * Pipeline per candidate round: [produce] -> validate against the currently
 * buffered sessions (visual anti-repeat, contrast guarantee, spatial history
 * via [isValid]) -> buffered. Invalid candidates are discarded and regenerated
 * silently in the background, never reaching the screen.
 *
 * Underflow safety: [poll] may legitimately return null when an extremely fast
 * player drains the buffer mid-refill — the caller falls back to synchronous
 * generation for that single round.
 */
class LookaheadSessionQueue(
    private val capacity: Int = QUEUE_CAPACITY,
    private val produce: () -> Challenge,
    private val isValid: (candidate: Challenge, buffered: List<Challenge>) -> Boolean,
    private val onConsume: (Challenge) -> Unit = {},
) {
    // Single-threaded producer context: generation helpers keep internal
    // mutation order without extra locking inside the pipeline itself.
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default.limitedParallelism(1))
    private val lock = Any()
    private val buffer = ArrayDeque<Challenge>()

    @Volatile private var closed = false

    /** Seed the buffer up to [capacity] validated rounds in the background. */
    fun prime(count: Int = capacity) = repeat(count) { refill() }

    /** Schedule one background replacement round (called after each consume). */
    fun refill() {
        if (closed) return
        scope.launch {
            val candidate = produceValidated()
            synchronized(lock) {
                if (!closed && buffer.size < capacity) buffer.addLast(candidate)
                // Late arrivals beyond capacity (or after close) are dropped silently.
            }
        }
    }

    /** Pop the next ready round, notifying [onConsume]; null when drained. */
    fun poll(): Challenge? = synchronized(lock) {
        buffer.removeFirstOrNull()?.also(onConsume)
    }

    /** Consistent view of the buffered rounds for out-of-band validation. */
    fun snapshot(): List<Challenge> = synchronized(lock) { buffer.toList() }

    /** Cancel every pending producer job and drop buffered sessions. */
    fun clear() {
        closed = true
        synchronized(lock) { buffer.clear() }
        scope.cancel()
    }

    private fun produceValidated(): Challenge {
        var candidate = produce()
        var tries = 0
        while (tries++ < MAX_VALIDATE_TRIES) {
            val snapshot = synchronized(lock) { buffer.toList() }
            if (isValid(candidate, snapshot)) break
            candidate = produce() // failed a rule — regenerate instead of showing it
        }
        return candidate
    }

    companion object {
        const val QUEUE_CAPACITY = 3
        const val MAX_VALIDATE_TRIES = 8
    }
}
