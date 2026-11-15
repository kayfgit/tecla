package app.tecla.keyboard.model

import org.junit.Assert.assertEquals
import org.junit.Test

class LetterLayoutsTest {
    private fun keyboard(layout: LetterLayout) = Layouts.letters(layout)

    private fun letterRows(layout: LetterLayout): List<List<Key>> =
        keyboard(layout).rows.take(3).map { row -> row.keys.filter { it.style == KeyStyle.CHARACTER } }

    private fun labels(layout: LetterLayout) = letterRows(layout).map { row -> row.joinToString("") { it.label } }

    @Test
    fun `offers the popular layouts`() {
        assertEquals(
            listOf("QWERTY", "QWERTZ", "AZERTY", "Dvorak", "Colemak", "Colemak-DH", "Workman"),
            LetterLayout.entries.map { it.label }
        )
    }

    @Test
    fun `every layout has each letter a to z exactly once`() {
        for (layout in LetterLayout.entries) {
            val letters = labels(layout).joinToString("").filter { it.isLetter() }
            assertEquals(layout.label, ('a'..'z').joinToString(""), letters.toList().sorted().joinToString(""))
        }
    }

    @Test
    fun `every row of every layout fills exactly the keyboard width`() {
        for (layout in LetterLayout.entries) {
            keyboard(layout).rows.forEachIndexed { i, row ->
                val units = row.inset * 2 + row.keys.sumOf { it.width.toDouble() }
                assertEquals("${layout.label} row $i", UNITS_PER_ROW.toDouble(), units, 0.001)
            }
        }
    }

    @Test
    fun `layouts have their real letter rows`() {
        assertEquals(listOf("qwertyuiop", "asdfghjkl", "zxcvbnm"), labels(LetterLayout.QWERTY))
        assertEquals(listOf("qwertzuiop", "asdfghjkl", "yxcvbnm"), labels(LetterLayout.QWERTZ))
        assertEquals(listOf("azertyuiop", "qsdfghjklm", "wxcvbn'"), labels(LetterLayout.AZERTY))
        assertEquals(listOf("',.pyfgcrl", "aoeuidhtns", "qjkxbmwvz"), labels(LetterLayout.DVORAK))
        assertEquals(listOf("qwfpgjluy;", "arstdhneio", "zxcvbkm"), labels(LetterLayout.COLEMAK))
        assertEquals(listOf("qwfpbjluy;", "arstgmneio", "zxcdvkh"), labels(LetterLayout.COLEMAK_DH))
        assertEquals(listOf("qdrwbjfup;", "ashtgyneoi", "zxmcvkl"), labels(LetterLayout.WORKMAN))
    }

    @Test
    fun `numbers, symbols and accents stay in the same spots on every layout`() {
        val qwerty = letterRows(LetterLayout.QWERTY)
        for (layout in LetterLayout.entries) {
            val rows = letterRows(layout)
            for (r in 0 until 3) {
                qwerty[r].forEachIndexed { i, key ->
                    assertEquals("${layout.label} row $r key $i", key.corners, rows[r].getOrNull(i)?.corners)
                }
            }
        }
    }

    @Test
    fun `every layout keeps every number, symbol and accent`() {
        fun cornerActions(layout: LetterLayout) = letterRows(layout).flatten().flatMap { it.corners.values }.toSet()
        val qwerty = cornerActions(LetterLayout.QWERTY)
        for (layout in LetterLayout.entries) assertEquals(layout.label, qwerty, cornerActions(layout))
    }

    @Test
    fun `delete keeps its full width on every layout`() {
        for (layout in LetterLayout.entries) {
            val delete = keyboard(layout).rows[2].keys.first { it.action == KeyAction.Delete }
            assertEquals(layout.label, 1.5f, delete.width)
        }
    }

    @Test
    fun `the default layout is qwerty`() {
        assertEquals(Layouts.letters, Layouts.letters(LetterLayout.QWERTY))
        assertEquals(Layouts.letters, Layouts.forPage(KeyboardPage.LETTERS))
        assertEquals(LetterLayout.QWERTY, LetterLayout.fromName(null))
        assertEquals(LetterLayout.QWERTY, LetterLayout.fromName("NOT_A_LAYOUT"))
        assertEquals(LetterLayout.DVORAK, LetterLayout.fromName("DVORAK"))
    }
}
