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

    private fun letterKey(c: String) = Layouts.letters.rows.flatMap { it.keys }.first { it.label == c }

    @Test
    fun `accent dead keys sit under letters like unexpected keyboard`() {
        assertEquals(KeyAction.Dead(Accent.ACUTE), letterKey("s").corners[Corner.BOTTOM_RIGHT])
        assertEquals(KeyAction.Dead(Accent.TILDE), letterKey("f").corners[Corner.BOTTOM_RIGHT])
        assertEquals(KeyAction.Dead(Accent.CEDILLA), letterKey("g").corners[Corner.BOTTOM_RIGHT])
        assertEquals(KeyAction.Dead(Accent.DIAERESIS), letterKey("u").corners[Corner.TOP_LEFT])
        assertTrue(letterKey("q").corners.isEmpty())
    }

    @Test
    fun `bottom row has 123 with ctrl and Fn with alt, keyboard, emoji and settings`() {
        for (layout in all) {
            val bottom = layout.rows.last().keys
            assertEquals(KeyAction.Ctrl, bottom[0].corners[Corner.BOTTOM_RIGHT])
            val fn = bottom[1]
            assertEquals(KeyAction.Fn, fn.action)
            assertEquals(
                mapOf(
                    Corner.TOP_LEFT to KeyAction.Alt,
                    Corner.TOP_RIGHT to KeyAction.SwitchKeyboard,
                    Corner.BOTTOM_LEFT to KeyAction.Emoji,
                    Corner.BOTTOM_RIGHT to KeyAction.Settings
                ),
                fn.corners
            )
        }
    }

    @Test
    fun `return key is two units wide and space takes the room it gave up`() {
        for (layout in all) {
            val bottom = layout.rows.last().keys
            assertEquals(2f, bottom.first { it.action == KeyAction.Enter }.width)
            assertEquals(5.5f, bottom.first { it.action == KeyAction.Space }.width)
        }
    }

    @Test
    fun `letters, symbols, arrows and forward delete repeat when held`() {
        assertTrue(KeyAction.Text("a").repeatsWhenHeld)
        assertTrue(KeyAction.Text("?").repeatsWhenHeld)
        assertTrue(KeyAction.Special(SpecialKey.LEFT).repeatsWhenHeld)
        assertTrue(KeyAction.Special(SpecialKey.FORWARD_DELETE).repeatsWhenHeld)
    }

    @Test
    fun `modifiers, page switches and one-off keys do not repeat`() {
        val once = listOf(
            KeyAction.Shift,
            KeyAction.Space,
            KeyAction.Enter,
            KeyAction.Fn,
            KeyAction.Ctrl,
            KeyAction.Page(KeyboardPage.NUMBERS),
            KeyAction.Dead(Accent.ACUTE),
            KeyAction.Special(SpecialKey.ESCAPE),
            KeyAction.Special(SpecialKey.F1)
        )
        for (action in once) assertTrue("$action", !action.repeatsWhenHeld)
    }

    @Test
    fun `fn page has all twelve F keys and the arrows`() {
        val specials = Layouts.fn.rows.flatMap { it.keys }.mapNotNull { (it.action as? KeyAction.Special)?.key }
        assertTrue(specials.containsAll(SpecialKey.entries))
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
    fun `shift and delete are wide keys at the row ends`() {
        // Shift spans 0..150, z 150..250, delete 850..1000.
        assertEquals(KeyAction.Shift, geometry.keyAt(149f, 225f)?.key?.action)
        assertEquals("z", geometry.keyAt(151f, 225f)?.key?.label)
        assertEquals(KeyAction.Delete, geometry.keyAt(851f, 225f)?.key?.action)
    }

    @Test
    fun `gap keys take space but are never hit`() {
        val layout = KeyboardLayout(
            KeyboardPage.LETTERS,
            listOf(
                KeyRow(
                    listOf(Key(KeyAction.Text("a"), "a", 4f), Key(KeyAction.Gap, "", 2f, KeyStyle.GAP), Key(KeyAction.Text("b"), "b", 4f))
                )
            )
        )
        val g = KeyboardGeometry(layout, width = 1000f, top = 0f, sidePadding = 0f, rowHeight = 50f)
        assertEquals("a", g.keyAt(450f, 25f)?.key?.label)
        assertEquals("b", g.keyAt(550f, 25f)?.key?.label)
        assertEquals(600f, g.keys.last().cell.left)
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
