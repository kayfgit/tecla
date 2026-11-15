package app.tecla.keyboard

import android.content.Context
import android.content.SharedPreferences
import app.tecla.keyboard.model.LetterLayout

/** User settings, shared by the setup screen and the keyboard. */
class TeclaSettings(context: Context) {
    val prefs: SharedPreferences = context.getSharedPreferences(FILE, Context.MODE_PRIVATE)

    var letterLayout: LetterLayout
        get() = LetterLayout.fromName(prefs.getString(KEY_LETTER_LAYOUT, null))
        set(value) = prefs.edit().putString(KEY_LETTER_LAYOUT, value.name).apply()

    companion object {
        private const val FILE = "settings"
        const val KEY_LETTER_LAYOUT = "letter_layout"
    }
}
