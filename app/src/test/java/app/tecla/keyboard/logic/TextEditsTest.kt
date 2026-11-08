package app.tecla.keyboard.logic

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class TextEditsTest {
    @Test
    fun `word before deletes the last word`() {
        assertEquals(5, TextEdits.wordLengthBefore("hello world"))
    }

    @Test
    fun `word before includes trailing spaces`() {
        assertEquals(8, TextEdits.wordLengthBefore("hello world   "))
    }

    @Test
    fun `word before treats a symbol run as one word`() {
        assertEquals(3, TextEdits.wordLengthBefore("wow!!!"))
    }

    @Test
    fun `word before keeps contractions together`() {
        assertEquals(5, TextEdits.wordLengthBefore("I don't"))
    }

    @Test
    fun `word before deletes a lone line break first`() {
        assertEquals(1, TextEdits.wordLengthBefore("line\n"))
    }

    @Test
    fun `word before stops at a line break`() {
        assertEquals(2, TextEdits.wordLengthBefore("one\n  "))
    }

    @Test
    fun `word before on empty text is zero`() {
        assertEquals(0, TextEdits.wordLengthBefore(""))
    }

    @Test
    fun `word after deletes the next word with leading spaces`() {
        assertEquals(6, TextEdits.wordLengthAfter(" hello world"))
    }

    @Test
    fun `word after deletes a lone line break first`() {
        assertEquals(1, TextEdits.wordLengthAfter("\nnext"))
    }

    @Test
    fun `several words before are summed`() {
        assertEquals(9, TextEdits.wordsLengthBefore("one two three", 2))
    }

    @Test
    fun `several words before stop at the start of the text`() {
        assertEquals(3, TextEdits.wordsLengthBefore("one", 5))
    }

    @Test
    fun `several words after are summed`() {
        assertEquals(7, TextEdits.wordsLengthAfter("one two three", 2))
    }

    @Test
    fun `line before deletes to the line start`() {
        assertEquals(11, TextEdits.lineLengthBefore("first\nsecond line"))
    }

    @Test
    fun `line before with no line break deletes everything`() {
        assertEquals(5, TextEdits.lineLengthBefore("hello"))
    }

    @Test
    fun `line before at a line start deletes the break`() {
        assertEquals(1, TextEdits.lineLengthBefore("first\n"))
    }

    @Test
    fun `double space adds a period after a word`() {
        assertTrue(TextEdits.shouldInsertPeriod("lo "))
        assertTrue(TextEdits.shouldInsertPeriod("(x) "))
    }

    @Test
    fun `double space does not add a period after punctuation or spaces`() {
        assertFalse(TextEdits.shouldInsertPeriod("o. "))
        assertFalse(TextEdits.shouldInsertPeriod("o  "))
        assertFalse(TextEdits.shouldInsertPeriod(" "))
        assertFalse(TextEdits.shouldInsertPeriod(null))
        assertFalse(TextEdits.shouldInsertPeriod("ab"))
    }
}
