package app.tecla.keyboard.model

/** What a key does when it is activated. */
sealed interface KeyAction {
    data class Text(val text: String) : KeyAction

    data class Page(val target: KeyboardPage) : KeyAction

    data object Shift : KeyAction

    data object Delete : KeyAction

    data object Space : KeyAction

    data object Enter : KeyAction

    data object SwitchKeyboard : KeyAction

    /** Empty space between keys; never drawn or hit. */
    data object Gap : KeyAction
}

enum class KeyboardPage { LETTERS, NUMBERS, SYMBOLS }

enum class KeyStyle { CHARACTER, FUNCTION, SPACE, ENTER, GAP }

/** A key, with its width measured in key units (a full row is [UNITS_PER_ROW] units wide). */
data class Key(
    val action: KeyAction,
    val label: String,
    val width: Float = 1f,
    val style: KeyStyle = KeyStyle.CHARACTER
)

/** A row of keys. [inset] is empty space on each side, in key units. */
data class KeyRow(val keys: List<Key>, val inset: Float = 0f)

data class KeyboardLayout(val page: KeyboardPage, val rows: List<KeyRow>)

const val UNITS_PER_ROW = 10f

/** iPhone-style layouts: QWERTY letters plus the "123" and "#+=" pages. */
object Layouts {
    private fun chars(s: String, width: Float = 1f): List<Key> = s.map { Key(KeyAction.Text(it.toString()), it.toString(), width) }

    private val delete = Key(KeyAction.Delete, "delete", 1.5f, KeyStyle.FUNCTION)

    private fun bottomRow(pageKey: Key) = KeyRow(
        listOf(
            pageKey,
            Key(KeyAction.SwitchKeyboard, "globe", 1.25f, KeyStyle.FUNCTION),
            Key(KeyAction.Space, "space", 5f, KeyStyle.SPACE),
            Key(KeyAction.Enter, "return", 2.5f, KeyStyle.ENTER)
        )
    )

    val letters = KeyboardLayout(
        KeyboardPage.LETTERS,
        listOf(
            KeyRow(chars("qwertyuiop")),
            KeyRow(chars("asdfghjkl"), inset = 0.5f),
            KeyRow(
                listOf(Key(KeyAction.Shift, "shift", 1.5f, KeyStyle.FUNCTION)) +
                    chars("zxcvbnm") +
                    listOf(delete)
            ),
            bottomRow(Key(KeyAction.Page(KeyboardPage.NUMBERS), "123", 1.25f, KeyStyle.FUNCTION))
        )
    )

    private fun punctuationRow(pageKey: Key) = KeyRow(
        listOf(pageKey) + chars(".,?!'", 1.4f) + listOf(delete)
    )

    private val abc = Key(KeyAction.Page(KeyboardPage.LETTERS), "ABC", 1.25f, KeyStyle.FUNCTION)

    val numbers = KeyboardLayout(
        KeyboardPage.NUMBERS,
        listOf(
            KeyRow(chars("1234567890")),
            KeyRow(chars("-/:;()$&@\"")),
            punctuationRow(Key(KeyAction.Page(KeyboardPage.SYMBOLS), "#+=", 1.5f, KeyStyle.FUNCTION)),
            bottomRow(abc)
        )
    )

    val symbols = KeyboardLayout(
        KeyboardPage.SYMBOLS,
        listOf(
            KeyRow(chars("[]{}#%^*+=")),
            KeyRow(chars("_\\|~<>€£¥•")),
            punctuationRow(Key(KeyAction.Page(KeyboardPage.NUMBERS), "123", 1.5f, KeyStyle.FUNCTION)),
            bottomRow(abc)
        )
    )

    fun forPage(page: KeyboardPage): KeyboardLayout = when (page) {
        KeyboardPage.LETTERS -> letters
        KeyboardPage.NUMBERS -> numbers
        KeyboardPage.SYMBOLS -> symbols
    }
}
