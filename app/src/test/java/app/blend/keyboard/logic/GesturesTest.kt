package app.blend.keyboard.logic

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class GesturesTest {
    private fun tracker() = DeleteGestureTracker(threshold = 10f, step = 20f)

    @Test
    fun `small movement is not a gesture`() {
        val t = tracker()
        assertEquals(0, t.update(-5f, 3f))
        assertNull(t.op)
    }

    @Test
    fun `swipe left deletes words and repeats with distance`() {
        val t = tracker()
        assertEquals(1, t.update(-12f, 0f))
        assertEquals(DeleteOp.WORD_BACK, t.op)
        assertEquals(0, t.update(-25f, 0f))
        assertEquals(1, t.update(-31f, 0f))
        assertEquals(2, t.update(-71f, 0f))
    }

    @Test
    fun `dragging back never undoes or repeats`() {
        val t = tracker()
        t.update(-31f, 0f)
        assertEquals(0, t.update(-5f, 0f))
        assertEquals(0, t.update(-31f, 0f))
    }

    @Test
    fun `directions map to delete operations`() {
        assertOp(DeleteOp.WORD_FORWARD, 15f, 2f)
        assertOp(DeleteOp.CHAR_FORWARD, 2f, -15f)
        assertOp(DeleteOp.LINE_BACK, 2f, 15f)
    }

    @Test
    fun `line delete happens only once`() {
        val t = tracker()
        assertEquals(1, t.update(0f, 15f))
        assertEquals(0, t.update(0f, 100f))
    }

    @Test
    fun `direction stays locked once chosen`() {
        val t = tracker()
        t.update(-12f, 0f)
        t.update(0f, 80f)
        assertEquals(DeleteOp.WORD_BACK, t.op)
    }

    private fun assertOp(expected: DeleteOp, dx: Float, dy: Float) {
        val t = tracker()
        assertEquals(1, t.update(dx, dy))
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
    fun `trackpad reset drops the remainder`() {
        val pad = TrackpadAccumulator(stepX = 10f, stepY = 30f)
        pad.move(9f, 0f)
        pad.reset()
        assertEquals(CursorSteps(0, 0), pad.move(5f, 0f))
    }
}
