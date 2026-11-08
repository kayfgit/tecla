package app.tecla.keyboard.logic

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ShiftControllerTest {
    private val shift = ShiftController()

    private fun tap() {
        shift.onPress()
        shift.onRelease()
    }

    @Test
    fun `one tap capitalizes only the next letter`() {
        tap()
        assertEquals(ShiftMode.ONCE, shift.mode)
        shift.onCharacterTyped()
        assertEquals(ShiftMode.OFF, shift.mode)
    }

    @Test
    fun `two taps lock caps until turned off`() {
        tap()
        tap()
        assertEquals(ShiftMode.LOCKED, shift.mode)
        repeat(5) { shift.onCharacterTyped() }
        assertEquals(ShiftMode.LOCKED, shift.mode)
    }

    @Test
    fun `three taps turn shift off and the cycle repeats`() {
        val seen = (1..7).map {
            tap()
            shift.mode
        }
        assertEquals(
            listOf(
                ShiftMode.ONCE,
                ShiftMode.LOCKED,
                ShiftMode.OFF,
                ShiftMode.ONCE,
                ShiftMode.LOCKED,
                ShiftMode.OFF,
                ShiftMode.ONCE
            ),
            seen
        )
    }

    @Test
    fun `auto caps counts as the first step of the cycle`() {
        shift.applyAutoCaps(true)
        tap()
        assertEquals(ShiftMode.LOCKED, shift.mode)
        tap()
        assertEquals(ShiftMode.OFF, shift.mode)
    }

    @Test
    fun `holding shift capitalizes every letter then turns off`() {
        shift.onPress()
        shift.onCharacterTyped()
        shift.onCharacterTyped()
        assertTrue(shift.isUpperCase)
        shift.onRelease()
        assertEquals(ShiftMode.OFF, shift.mode)
        assertFalse(shift.isUpperCase)
    }

    @Test
    fun `auto caps does not override caps lock`() {
        tap()
        tap()
        shift.applyAutoCaps(false)
        assertEquals(ShiftMode.LOCKED, shift.mode)
    }

    @Test
    fun `auto caps sets and clears one-shot shift`() {
        shift.applyAutoCaps(true)
        assertEquals(ShiftMode.ONCE, shift.mode)
        shift.applyAutoCaps(false)
        assertEquals(ShiftMode.OFF, shift.mode)
    }
}
