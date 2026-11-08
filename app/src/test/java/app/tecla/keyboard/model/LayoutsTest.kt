package app.tecla.keyboard.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class LayoutsTest {
    private val all = KeyboardPage.entries.map { Layouts.forPage(it) }

    @Test
    fun `every row fills exactly the keyboard width`() {
        for (layout in all) {
            layout.rows.forEachIndexed { i, row ->
                val units = row.inset * 2 + row.keys.sumOf { it.width.toDouble() }
                assertEquals("${layout.page} row $i", UNITS_PER_ROW.toDouble(), units, 0.001)
            }
        }
    }

    @Test
    fun `letters page is qwerty`() {
        val labels = Layouts.letters.rows.take(3).map { row ->
            row.keys.filter { it.style == KeyStyle.CHARACTER }.joinToString("") { it.label }
        }
        assertEquals(listOf("qwertyuiop", "asdfghjkl", "zxcvbnm"), labels)
    }

    @Test
    fun `every page has delete, space and enter`() {
        for (layout in all) {
            val actions = layout.rows.flatMap { it.keys }.map { it.action }
            assertTrue(KeyAction.Delete in actions)
            assertTrue(KeyAction.Space in actions)
            assertTrue(KeyAction.Enter in actions)
        }
    }

    private val geometry = KeyboardGeometry(Layouts.letters, width = 1000f, top = 100f, sidePadding = 0f, rowHeight = 50f)

    @Test
    fun `touch inside a key hits that key`() {
        val q = geometry.keyAt(50f, 125f)
        assertEquals("q", q?.key?.label)
    }

    @Test
    fun `touch in the second row inset goes to the nearest key`() {
        assertEquals("a", geometry.keyAt(10f, 175f)?.key?.label)
        assertEquals("l", geometry.keyAt(995f, 175f)?.key?.label)
    }

    @Test
    fun `touch in the gap next to shift picks the closer key`() {
        // Shift spans 0..130, gap 130..150, z starts at 150.
        assertEquals(KeyAction.Shift, geometry.keyAt(135f, 225f)?.key?.action)
        assertEquals("z", geometry.keyAt(147f, 225f)?.key?.label)
    }

    @Test
    fun `touches above or below the keyboard clamp to the edge rows`() {
        assertEquals("q", geometry.keyAt(50f, 0f)?.key?.label)
        assertEquals(KeyAction.Space, geometry.keyAt(500f, 999f)?.key?.action)
    }

    @Test
    fun `gaps are not placed as keys`() {
        assertTrue(geometry.keys.none { it.key.style == KeyStyle.GAP })
    }
}
