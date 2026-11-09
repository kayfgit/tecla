package app.tecla.keyboard.logic

/** Pure text rules used by the delete key and the space bar. */
object TextEdits {
    private fun isWordChar(c: Char) = c.isLetterOrDigit() || c == '_' || c == '\'' || c == '’'

    private fun isInlineSpace(c: Char) = c == ' ' || c == '\t'

    /**
     * Characters to delete for one "delete word backwards" (like Ctrl+Backspace):
     * trailing spaces, then one run of word characters or one run of symbols.
     */
    fun wordLengthBefore(text: CharSequence): Int {
        val end = text.length
        if (end == 0) return 0
        if (text[end - 1] == '\n') return 1
        var i = end
        while (i > 0 && isInlineSpace(text[i - 1])) i--
        if (i > 0 && text[i - 1] != '\n') {
            val word = isWordChar(text[i - 1])
            while (i > 0 && !text[i - 1].isWhitespace() && isWordChar(text[i - 1]) == word) i--
        }
        return end - i
    }

    /** Mirror of [wordLengthBefore] for "delete word forwards" (like Ctrl+Delete). */
    fun wordLengthAfter(text: CharSequence): Int {
        if (text.isEmpty()) return 0
        if (text[0] == '\n') return 1
        var i = 0
        while (i < text.length && isInlineSpace(text[i])) i++
        if (i < text.length && text[i] != '\n') {
            val word = isWordChar(text[i])
            while (i < text.length && !text[i].isWhitespace() && isWordChar(text[i]) == word) i++
        }
        return i
    }

    /** Total characters for [count] word deletions backwards, computed in one pass. */
    fun wordsLengthBefore(text: CharSequence, count: Int): Int {
        var remaining = text
        var total = 0
        repeat(count) {
            val n = wordLengthBefore(remaining)
            if (n == 0) return total
            total += n
            remaining = remaining.subSequence(0, remaining.length - n)
        }
        return total
    }

    /** Total characters for [count] word deletions forwards. */
    fun wordsLengthAfter(text: CharSequence, count: Int): Int {
        var remaining = text
        var total = 0
        repeat(count) {
            val n = wordLengthAfter(remaining)
            if (n == 0) return total
            total += n
            remaining = remaining.subSequence(n, remaining.length)
        }
        return total
    }

    /** Characters back to the start of the current line; a bare line break counts as one. */
    fun lineLengthBefore(text: CharSequence): Int {
        if (text.isEmpty()) return 0
        if (text.last() == '\n') return 1
        val lastBreak = text.lastIndexOf('\n')
        return text.length - (lastBreak + 1)
    }
}
