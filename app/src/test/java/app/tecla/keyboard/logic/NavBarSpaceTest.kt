package app.tecla.keyboard.logic

import org.junit.Assert.assertEquals
import org.junit.Test

class NavBarSpaceTest {
    @Test
    fun `keeps room for the navigation bar while it is briefly hidden`() {
        // A fullscreen app or a transition hides the bar: visible inset 0, real size 126px.
        assertEquals(126, navBarSpace(visibleBottom = 0, barBottom = 126))
    }

    @Test
    fun `uses the visible inset when it is the larger one`() {
        assertEquals(126, navBarSpace(visibleBottom = 126, barBottom = 126))
        assertEquals(140, navBarSpace(visibleBottom = 140, barBottom = 126))
    }

    @Test
    fun `no navigation bar means no space`() {
        assertEquals(0, navBarSpace(visibleBottom = 0, barBottom = 0))
    }
}
