package app.tecla.keyboard.logic

import org.junit.Assert.assertTrue
import org.junit.Test

class RepeatTimingTest {
    /** How many repeats fire while a key is held for [holdMs], given the wait after each repeat. */
    private fun repeatsWithin(holdMs: Long, interval: (repeatsSoFar: Int) -> Long): Int {
        var t = RepeatTiming.START_DELAY_MS
        var count = 0
        while (t <= holdMs) {
            count++
            t += interval(count)
        }
        return count
    }

    private fun deletes(holdMs: Long) = repeatsWithin(holdMs, RepeatTiming::deleteInterval)

    private fun letters(holdMs: Long) = repeatsWithin(holdMs) { RepeatTiming.KEY_INTERVAL_MS }

    private fun assertNear(expected: Int, actual: Int, tolerance: Int) {
        assertTrue("expected about $expected, got $actual", actual in expected - tolerance..expected + tolerance)
    }

    @Test
    fun `nothing repeats before the start delay`() {
        assertTrue(deletes(RepeatTiming.START_DELAY_MS - 1) == 0)
        assertTrue(letters(RepeatTiming.START_DELAY_MS - 1) == 0)
    }

    // Samsung Keyboard on a Galaxy A36, holding delete: 1.2s = 45, 2s = 110, 3s = 185 letters
    // (one of them deleted on touch down).
    @Test
    fun `holding delete speeds up like the Samsung keyboard`() {
        assertNear(44, deletes(1200), tolerance = 5)
        assertNear(109, deletes(2000), tolerance = 8)
        assertNear(184, deletes(3000), tolerance = 10)
    }

    @Test
    fun `delete starts at a steady pace before speeding up`() {
        assertTrue(RepeatTiming.deleteInterval(1) > RepeatTiming.deleteInterval(RepeatTiming.DELETE_FAST_AFTER))
    }

    @Test
    fun `holding a letter for 1_2s types clearly more than the old 14`() {
        val count = letters(1200)
        assertTrue("got $count", count in 20..30)
    }
}
