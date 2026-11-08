package app.tecla.keyboard.logic

enum class ShiftMode { OFF, ONCE, LOCKED }

/**
 * Shift cycles on every tap, however fast you tap: off → one capital → caps lock → off.
 * Holding shift while typing capitalizes the letters typed during the hold, then turns shift off.
 */
class ShiftController {
    var mode: ShiftMode = ShiftMode.OFF
        private set

    private var held = false
    private var typedWhileHeld = false

    val isUpperCase: Boolean get() = held || mode != ShiftMode.OFF

    fun onPress() {
        mode = when (mode) {
            ShiftMode.OFF -> ShiftMode.ONCE
            ShiftMode.ONCE -> ShiftMode.LOCKED
            ShiftMode.LOCKED -> ShiftMode.OFF
        }
        held = true
        typedWhileHeld = false
    }

    fun onRelease() {
        held = false
        if (typedWhileHeld) mode = ShiftMode.OFF
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
    }
}
