package app.blend.keyboard.logic

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ShiftControllerTest {
    private val shift = ShiftController(doubleTapWindowMs = 300)

    private fun tap(now: Long) {
        shift.onPress(now)
        shift.onRelease()
    }

    @Test
    fun `tap turns on shift for one letter`() {
        tap(0)
        assertEquals(ShiftMode.ONCE, shift.mode)
        shift.onCharacterTyped()
        assertEquals(ShiftMode.OFF, shift.mode)
    }

    @Test
    fun `second slow tap turns shift off`() {
        tap(0)
        tap(1000)
        assertEquals(ShiftMode.OFF, shift.mode)
    }

    @Test
    fun `double tap locks caps`() {
        tap(0)
        tap(200)
        assertEquals(ShiftMode.LOCKED, shift.mode)
        shift.onCharacterTyped()
        shift.onCharacterTyped()
        assertEquals(ShiftMode.LOCKED, shift.mode)
    }

    @Test
    fun `tap after caps lock turns it off and a quick tap does not relock`() {
        tap(0)
        tap(200)
        tap(1000)
        assertEquals(ShiftMode.OFF, shift.mode)
        tap(1100)
        assertEquals(ShiftMode.ONCE, shift.mode)
    }

    @Test
    fun `holding shift capitalizes every letter then turns off`() {
        shift.onPress(0)
        shift.onCharacterTyped()
        shift.onCharacterTyped()
        assertTrue(shift.isUpperCase)
        shift.onRelease()
        assertEquals(ShiftMode.OFF, shift.mode)
        assertFalse(shift.isUpperCase)
    }

    @Test
    fun `auto caps does not override caps lock`() {
        tap(0)
        tap(100)
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
