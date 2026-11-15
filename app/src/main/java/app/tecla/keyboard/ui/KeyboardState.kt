package app.tecla.keyboard.ui

import app.tecla.keyboard.logic.ShiftMode
import app.tecla.keyboard.model.Accent
import app.tecla.keyboard.model.KeyboardLayout
import app.tecla.keyboard.model.Layouts

/** Everything the keyboard view needs to know to draw itself. */
data class KeyboardState(
    val layout: KeyboardLayout = Layouts.letters,
    val shiftMode: ShiftMode = ShiftMode.OFF,
    val upperCase: Boolean = false,
    /** Word on the return key, or null for the return arrow icon. */
    val enterLabel: String? = null,
    val enterAccent: Boolean = false,
    val pendingAccent: Accent? = null,
    val ctrl: Boolean = false,
    val alt: Boolean = false
)
