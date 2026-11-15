package app.tecla.keyboard.logic

import app.tecla.keyboard.model.Corner
import kotlin.math.abs
import kotlin.math.hypot
import kotlin.math.max

/** The corner a swipe of ([dx], [dy]) points at, or null if it is shorter than [threshold]. */
fun cornerOf(dx: Float, dy: Float, threshold: Float): Corner? {
    if (hypot(dx, dy) < threshold) return null
    return when {
        dx < 0 && dy < 0 -> Corner.TOP_LEFT
        dx >= 0 && dy < 0 -> Corner.TOP_RIGHT
        dx < 0 -> Corner.BOTTOM_LEFT
        else -> Corner.BOTTOM_RIGHT
    }
}

enum class DeleteOp { CHAR_BACK, CHAR_FORWARD, WORD_BACK, WORD_FORWARD, LINE_BACK }

/**
 * Turns a swipe that starts on the delete key into a delete operation:
 * left = word backwards, right = word forwards, up = character forwards, down = to line start.
 *
 * The operation happens once when the direction is chosen. Repeating is time-based (handled by the
 * view, like holding a key): it only repeats while the finger stays out past the threshold.
 */
class DeleteGestureTracker(private val threshold: Float) {
    var op: DeleteOp? = null
        private set

    /** True while the finger is past the threshold in the chosen direction. */
    var engaged = false
        private set

    /** Whether holding the swipe should keep repeating the operation. */
    val repeats: Boolean get() = op != null && op != DeleteOp.LINE_BACK

    /** Feeds a drag of ([dx], [dy]) from the touch-down point. Returns true when the direction was just chosen. */
    fun update(dx: Float, dy: Float): Boolean {
        val current = op
        if (current == null) {
            if (max(abs(dx), abs(dy)) < threshold) return false
            op = if (abs(dx) >= abs(dy)) {
                if (dx < 0) DeleteOp.WORD_BACK else DeleteOp.WORD_FORWARD
            } else {
                if (dy < 0) DeleteOp.CHAR_FORWARD else DeleteOp.LINE_BACK
            }
            engaged = true
            return true
        }
        val distance = when (current) {
            DeleteOp.WORD_BACK -> -dx
            DeleteOp.WORD_FORWARD -> dx
            DeleteOp.CHAR_FORWARD -> -dy
            DeleteOp.LINE_BACK -> dy
            DeleteOp.CHAR_BACK -> 0f
        }
        engaged = distance >= threshold
        return false
    }
}

data class CursorSteps(val x: Int, val y: Int) {
    val isZero get() = x == 0 && y == 0
}

/** How far (in dp) the finger travels on the space bar for one cursor step. Smaller is faster. */
object CursorSpeed {
    const val STEP_X_DP = 13f
    const val STEP_Y_DP = 35f
}

/** Space-bar trackpad: converts finger movement into whole cursor steps, carrying the remainder over. */
class TrackpadAccumulator(private val stepX: Float, private val stepY: Float) {
    private var accX = 0f
    private var accY = 0f

    fun move(dx: Float, dy: Float): CursorSteps {
        accX += dx
        accY += dy
        val sx = (accX / stepX).toInt()
        val sy = (accY / stepY).toInt()
        accX -= sx * stepX
        accY -= sy * stepY
        return CursorSteps(sx, sy)
    }

    fun reset() {
        accX = 0f
        accY = 0f
    }
}
