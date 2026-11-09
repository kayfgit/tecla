package app.tecla.keyboard.logic

import org.junit.Assert.assertEquals
import org.junit.Test

class EmojiDataTest {
    @Test
    fun `parses groups and emoji`() {
        val groups = EmojiData.parse("Smileys & Emotion\t😀 😃\nFlags\t🏁\n")
        assertEquals(listOf("Smileys & Emotion", "Flags"), groups.map { it.name })
        assertEquals(listOf("😀", "😃"), groups[0].emoji)
    }

    @Test
    fun `keeps multi-code-point emoji whole`() {
        val groups = EmojiData.parse("People & Body\t👨‍👩‍👧 🏳️‍🌈\n")
        assertEquals(listOf("👨‍👩‍👧", "🏳️‍🌈"), groups[0].emoji)
    }

    @Test
    fun `skips blank and malformed lines and empty groups`() {
        val groups = EmojiData.parse("\nnot a group\nEmpty\t \nObjects\t💡\n")
        assertEquals(listOf("Objects"), groups.map { it.name })
    }

    @Test
    fun `recent puts the newest first without duplicates`() {
        assertEquals(listOf("b", "a", "c"), EmojiData.addRecent(listOf("a", "b", "c"), "b"))
    }

    @Test
    fun `recent is capped`() {
        assertEquals(listOf("x", "a"), EmojiData.addRecent(listOf("a", "b"), "x", max = 2))
    }

    @Test
    fun `bundled emoji file parses into the nine unicode groups`() {
        val file = listOf("src/main/assets/emoji.txt", "app/src/main/assets/emoji.txt").map { java.io.File(it) }.first { it.exists() }
        val groups = EmojiData.parse(file.readText())
        assertEquals(9, groups.size)
        assertEquals("Smileys & Emotion", groups.first().name)
    }
}
