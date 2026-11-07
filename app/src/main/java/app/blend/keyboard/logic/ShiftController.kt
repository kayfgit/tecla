package app.blend.keyboard.logic

enum class ShiftMode { OFF, ONCE, LOCKED }

/**
 * iPhone-style shift: tap for one capital, double-tap for caps lock, tap again to turn off.
 * Holding shift while typing capitalizes every letter typed during the hold.
 */
class ShiftController(private val doubleTapWindowMs: Long = 350L) {
    var mode: ShiftMode = ShiftMode.OFF
        private set

    private var held = false
    private var typedWhileHeld = false
    private var lastPressAt: Long? = null
    private var lastPressUnlocked = false

    val isUpperCase: Boolean get() = held || mode != ShiftMode.OFF

    fun onPress(now: Long) {
        val previous = lastPressAt
        val isDoubleTap = previous != null && now - previous <= doubleTapWindowMs && !lastPressUnlocked
        lastPressUnlocked = false
        mode = when {
            isDoubleTap -> ShiftMode.LOCKED
            mode == ShiftMode.OFF -> ShiftMode.ONCE
            mode == ShiftMode.LOCKED -> {
                lastPressUnlocked = true
                ShiftMode.OFF
            }
            else -> ShiftMode.OFF
        }
        lastPressAt = now
        held = true
        typedWhileHeld = false
    }

    fun onRelease() {
        held = false
        if (typedWhileHeld && mode != ShiftMode.LOCKED) mode = ShiftMode.OFF
        typedWhileHeld = false
    }

    fun onCharacterTyped() {
        if (held) {
            typedWhileHeld = true
        } else if (mode == ShiftMode.ONCE) {
            mode = ShiftMode.OFF
        }
    }

    /** Applies the editor's auto-capitalization hint unless the user is controlling shift. */
    fun applyAutoCaps(capitalize: Boolean) {
        if (held || mode == ShiftMode.LOCKED) return
        mode = if (capitalize) ShiftMode.ONCE else ShiftMode.OFF
    }

    fun reset() {
        mode = ShiftMode.OFF
        held = false
        typedWhileHeld = false
        lastPressAt = null
        lastPressUnlocked = false
    }
}
