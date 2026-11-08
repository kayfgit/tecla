package app.tecla.keyboard.logic

import app.tecla.keyboard.model.Accent
import java.text.Normalizer

object Accents {
    /**
     * Applies a pending accent to the next typed [text], like a dead key on a computer:
     * ´ + e = é, ´ + space = ´, and ´ + a letter with no accented form = ´ followed by the letter.
     */
    fun compose(accent: Accent, text: String): String {
        if (text == " ") return accent.spacing
        if (text.length != 1 || !text[0].isLetter()) return accent.spacing + text
        val composed = Normalizer.normalize(text + accent.combining, Normalizer.Form.NFC)
        return if (composed.length == 1) composed else accent.spacing + text
    }
}
