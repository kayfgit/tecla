package app.tecla.keyboard.logic

import app.tecla.keyboard.model.Corner
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class GesturesTest {
    private fun tracker() = DeleteGestureTracker(threshold = 10f)

    @Test
    fun `small movement is not a gesture`() {
        val t = tracker()
        assertFalse(t.update(-5f, 3f))
        assertNull(t.op)
        assertFalse(t.engaged)
    }

    @Test
    fun `swipe left chooses word back exactly once`() {
        val t = tracker()
        assertTrue(t.update(-12f, 0f))
        assertEquals(DeleteOp.WORD_BACK, t.op)
        // Dragging further never fires again by distance; repeating is time-based.
        assertFalse(t.update(-80f, 0f))
        assertFalse(t.update(-200f, 0f))
    }

    @Test
    fun `engaged follows whether the finger is still out past the threshold`() {
        val t = tracker()
        t.update(-12f, 0f)
        assertTrue(t.engaged)
        t.update(-4f, 0f)
        assertFalse(t.engaged)
        t.update(-30f, 0f)
        assertTrue(t.engaged)
    }

    @Test
    fun `directions map to delete operations`() {
        assertOp(DeleteOp.WORD_FORWARD, 15f, 2f)
        assertOp(DeleteOp.CHAR_FORWARD, 2f, -15f)
        assertOp(DeleteOp.LINE_BACK, 2f, 15f)
    }

    @Test
    fun `line delete does not repeat but the others do`() {
        val line = tracker().apply { update(0f, 15f) }
        assertFalse(line.repeats)
        val word = tracker().apply { update(-15f, 0f) }
        assertTrue(word.repeats)
        assertFalse(tracker().repeats)
    }

    @Test
    fun `direction stays locked once chosen`() {
        val t = tracker()
        t.update(-12f, 0f)
        t.update(0f, 80f)
        assertEquals(DeleteOp.WORD_BACK, t.op)
        assertFalse(t.engaged)
    }

    private fun assertOp(expected: DeleteOp, dx: Float, dy: Float) {
        val t = tracker()
        assertTrue(t.update(dx, dy))
        assertEquals(expected, t.op)
    }

    @Test
    fun `trackpad emits whole steps and keeps the remainder`() {
        val pad = TrackpadAccumulator(stepX = 10f, stepY = 30f)
        assertEquals(CursorSteps(0, 0), pad.move(6f, 0f))
        assertEquals(CursorSteps(1, 0), pad.move(6f, 0f))
        assertEquals(CursorSteps(-1, 1), pad.move(-12f, 31f))
    }

    @Test
    fun `short swipes point at no corner`() {
        assertNull(cornerOf(5f, 5f, threshold = 10f))
    }

    @Test
    fun `swipes point at the matching corner`() {
        assertEquals(Corner.TOP_LEFT, cornerOf(-10f, -10f, threshold = 10f))
        assertEquals(Corner.TOP_RIGHT, cornerOf(10f, -10f, threshold = 10f))
        assertEquals(Corner.BOTTOM_LEFT, cornerOf(-10f, 10f, threshold = 10f))
        assertEquals(Corner.BOTTOM_RIGHT, cornerOf(12f, 2f, threshold = 10f))
    }

    @Test
    fun `trackpad reset drops the remainder`() {
        val pad = TrackpadAccumulator(stepX = 10f, stepY = 30f)
        pad.move(9f, 0f)
        pad.reset()
        assertEquals(CursorSteps(0, 0), pad.move(5f, 0f))
    }

    @Test
    fun `space-bar cursor is 10 to 20 percent faster than the old 15dp and 40dp steps`() {
        val speedupX = 15f / CursorSpeed.STEP_X_DP
        val speedupY = 40f / CursorSpeed.STEP_Y_DP
        assertTrue("x speedup $speedupX", speedupX in 1.1f..1.2f)
        assertTrue("y speedup $speedupY", speedupY in 1.1f..1.2f)
    }
}
