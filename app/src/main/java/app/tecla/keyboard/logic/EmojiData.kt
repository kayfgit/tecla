package app.tecla.keyboard.logic

data class EmojiGroup(val name: String, val emoji: List<String>)

object EmojiData {
    /** Parses the bundled emoji.txt: one group per line, "name<TAB>emoji emoji ...". */
    fun parse(text: String): List<EmojiGroup> = text.lineSequence()
        .filter { '\t' in it }
        .map { line ->
            val (name, list) = line.split('\t', limit = 2)
            EmojiGroup(name.trim(), list.split(' ').filter { it.isNotBlank() })
        }
        .filter { it.emoji.isNotEmpty() }
        .toList()

    /** Puts [emoji] first in the recent list, without duplicates, keeping at most [max] entries. */
    fun addRecent(recent: List<String>, emoji: String, max: Int = 40): List<String> =
        (listOf(emoji) + recent.filter { it != emoji }).take(max)
}
