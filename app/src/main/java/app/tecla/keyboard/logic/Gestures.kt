package app.tecla.keyboard.logic

import kotlin.math.abs
import kotlin.math.floor
import kotlin.math.max

enum class DeleteOp { CHAR_BACK, CHAR_FORWARD, WORD_BACK, WORD_FORWARD, LINE_BACK }

/**
 * Turns a drag that starts on the delete key into delete operations:
 * left = words backwards, right = words forwards, up = characters forwards, down = to line start.
 * Dragging further repeats the operation every [step] pixels (except line delete, which happens once).
 */
class DeleteGestureTracker(private val threshold: Float, private val step: Float) {
    var op: DeleteOp? = null
        private set

    private var performed = 0

    /** Returns how many new operations to perform for a drag of ([dx], [dy]) from the touch-down point. */
    fun update(dx: Float, dy: Float): Int {
        val current = op ?: lockDirection(dx, dy) ?: return 0
        val distance = when (current) {
            DeleteOp.WORD_BACK -> -dx
            DeleteOp.WORD_FORWARD -> dx
            DeleteOp.CHAR_FORWARD -> -dy
            DeleteOp.LINE_BACK -> dy
            DeleteOp.CHAR_BACK -> 0f
        }
        val wanted = when {
            distance < threshold -> performed
            current == DeleteOp.LINE_BACK -> 1
            else -> 1 + floor((distance - threshold) / step).toInt()
        }
        val fresh = max(0, wanted - performed)
        performed += fresh
        return fresh
    }

    private fun lockDirection(dx: Float, dy: Float): DeleteOp? {
        if (max(abs(dx), abs(dy)) < threshold) return null
        val locked = if (abs(dx) >= abs(dy)) {
            if (dx < 0) DeleteOp.WORD_BACK else DeleteOp.WORD_FORWARD
        } else {
            if (dy < 0) DeleteOp.CHAR_FORWARD else DeleteOp.LINE_BACK
        }
        op = locked
        return locked
    }
}

data class CursorSteps(val x: Int, val y: Int) {
    val isZero get() = x == 0 && y == 0
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
