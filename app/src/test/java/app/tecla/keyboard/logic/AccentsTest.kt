package app.tecla.keyboard.logic

import app.tecla.keyboard.model.Accent
import org.junit.Assert.assertEquals
import org.junit.Test

class AccentsTest {
    @Test
    fun `accents combine with letters`() {
        assertEquals("é", Accents.compose(Accent.ACUTE, "e"))
        assertEquals("à", Accents.compose(Accent.GRAVE, "a"))
        assertEquals("ê", Accents.compose(Accent.CIRCUMFLEX, "e"))
        assertEquals("ã", Accents.compose(Accent.TILDE, "a"))
        assertEquals("ñ", Accents.compose(Accent.TILDE, "n"))
        assertEquals("ç", Accents.compose(Accent.CEDILLA, "c"))
        assertEquals("č", Accents.compose(Accent.CARON, "c"))
        assertEquals("ā", Accents.compose(Accent.MACRON, "a"))
        assertEquals("ü", Accents.compose(Accent.DIAERESIS, "u"))
    }

    @Test
    fun `accents keep capital letters`() {
        assertEquals("É", Accents.compose(Accent.ACUTE, "E"))
        assertEquals("Ç", Accents.compose(Accent.CEDILLA, "C"))
    }

    @Test
    fun `space types the accent itself`() {
        assertEquals("´", Accents.compose(Accent.ACUTE, " "))
        assertEquals("~", Accents.compose(Accent.TILDE, " "))
    }

    @Test
    fun `letters with no accented form keep the accent before them`() {
        assertEquals("´q", Accents.compose(Accent.ACUTE, "q"))
    }

    @Test
    fun `non letters keep the accent before them`() {
        assertEquals("^1", Accents.compose(Accent.CIRCUMFLEX, "1"))
    }

    @Test
    fun `every accent label is a dotted circle with the mark`() {
        for (a in Accent.entries) assertEquals("◌${a.combining}", a.label)
    }
}
