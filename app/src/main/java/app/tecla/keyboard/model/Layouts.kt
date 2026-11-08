package app.tecla.keyboard.model

/** Accent dead keys: pick one, then type a letter to get the accented letter (e.g. ´ then e = é). */
enum class Accent(val combining: Char, val spacing: String) {
    GRAVE('̀', "`"),
    ACUTE('́', "´"),
    CIRCUMFLEX('̂', "^"),
    TILDE('̃', "~"),
    CEDILLA('̧', "¸"),
    CARON('̌', "ˇ"),
    MACRON('̄', "¯"),
    DIAERESIS('̈', "¨");

    /** The accent drawn on a dotted circle, like Unexpected Keyboard shows it. */
    val label: String get() = "◌$combining"
}

/** Keys from a physical keyboard that have no character, used on the Fn page. */
enum class SpecialKey(val label: String, val repeats: Boolean = false) {
    ESCAPE("esc"),
    TAB("tab"),
    HOME("home"),
    END("end"),
    PAGE_UP("pg up"),
    PAGE_DOWN("pg dn"),
    INSERT("ins"),
    FORWARD_DELETE("del", repeats = true),
    LEFT("←", repeats = true),
    RIGHT("→", repeats = true),
    UP("↑", repeats = true),
    DOWN("↓", repeats = true),
    F1("F1"),
    F2("F2"),
    F3("F3"),
    F4("F4"),
    F5("F5"),
    F6("F6"),
    F7("F7"),
    F8("F8"),
    F9("F9"),
    F10("F10"),
    F11("F11"),
    F12("F12")
}

/** What a key does when it is activated. */
sealed interface KeyAction {
    data class Text(val text: String) : KeyAction

    data class Page(val target: KeyboardPage) : KeyAction

    data class Dead(val accent: Accent) : KeyAction

    data class Special(val key: SpecialKey) : KeyAction

    data object Shift : KeyAction

    data object Delete : KeyAction

    data object Space : KeyAction

    data object Enter : KeyAction

    data object SwitchKeyboard : KeyAction

    /** One-shot Ctrl modifier for the next key. */
    data object Ctrl : KeyAction

    /** One-shot Alt modifier for the next key. */
    data object Alt : KeyAction

    /** Toggles the Fn page (F-keys, Esc, Tab, arrows, Home/End...). */
    data object Fn : KeyAction

    data object Emoji : KeyAction

    data object Settings : KeyAction

    /** Empty space between keys; never drawn or hit. */
    data object Gap : KeyAction
}

/** Where an extra action sits on a key; swipe toward that corner to use it. */
enum class Corner { TOP_LEFT, TOP_RIGHT, BOTTOM_LEFT, BOTTOM_RIGHT }

enum class KeyboardPage { LETTERS, NUMBERS, SYMBOLS, FN }

enum class KeyStyle { CHARACTER, FUNCTION, SPACE, ENTER, GAP }

/** A key, with its width measured in key units (a full row is [UNITS_PER_ROW] units wide). */
data class Key(
    val action: KeyAction,
    val label: String,
    val width: Float = 1f,
    val style: KeyStyle = KeyStyle.CHARACTER,
    val corners: Map<Corner, KeyAction> = emptyMap()
)

/** A row of keys. [inset] is empty space on each side, in key units. */
data class KeyRow(val keys: List<Key>, val inset: Float = 0f)

data class KeyboardLayout(val page: KeyboardPage, val rows: List<KeyRow>)

const val UNITS_PER_ROW = 10f

/** iPhone-style layouts with Unexpected Keyboard style corner swipes. */
object Layouts {
    /** Accent dead keys under letters, placed as on Unexpected Keyboard. */
    private val accentCorners: Map<Char, Pair<Corner, Accent>> = mapOf(
        'u' to (Corner.TOP_LEFT to Accent.DIAERESIS),
        'a' to (Corner.BOTTOM_RIGHT to Accent.GRAVE),
        's' to (Corner.BOTTOM_RIGHT to Accent.ACUTE),
        'd' to (Corner.BOTTOM_RIGHT to Accent.CIRCUMFLEX),
        'f' to (Corner.BOTTOM_RIGHT to Accent.TILDE),
        'g' to (Corner.BOTTOM_RIGHT to Accent.CEDILLA),
        'h' to (Corner.BOTTOM_RIGHT to Accent.CARON),
        'l' to (Corner.BOTTOM_RIGHT to Accent.MACRON)
    )

    private fun letters(s: String): List<Key> = s.map { c ->
        val corners = accentCorners[c]?.let { (corner, accent) -> mapOf(corner to KeyAction.Dead(accent)) }.orEmpty()
        Key(KeyAction.Text(c.toString()), c.toString(), corners = corners)
    }

    private fun chars(s: String, width: Float = 1f): List<Key> = s.map { Key(KeyAction.Text(it.toString()), it.toString(), width) }

    private fun special(key: SpecialKey, width: Float = 1f) = Key(KeyAction.Special(key), key.label, width, KeyStyle.FUNCTION)

    private val delete = Key(KeyAction.Delete, "delete", 1.5f, KeyStyle.FUNCTION)

    private val ctrlCorner = mapOf(Corner.BOTTOM_RIGHT to KeyAction.Ctrl)

    private val fnKey = Key(
        KeyAction.Fn,
        "Fn",
        1.25f,
        KeyStyle.FUNCTION,
        corners = mapOf(
            Corner.TOP_LEFT to KeyAction.Alt,
            Corner.TOP_RIGHT to KeyAction.SwitchKeyboard,
            Corner.BOTTOM_LEFT to KeyAction.Emoji,
            Corner.BOTTOM_RIGHT to KeyAction.Settings
        )
    )

    private fun pageKey(label: String, target: KeyboardPage) = Key(KeyAction.Page(target), label, 1.25f, KeyStyle.FUNCTION, ctrlCorner)

    private fun bottomRow(pageKey: Key) = KeyRow(
        listOf(
            pageKey,
            fnKey,
            Key(KeyAction.Space, "space", 5f, KeyStyle.SPACE),
            Key(KeyAction.Enter, "return", 2.5f, KeyStyle.ENTER)
        )
    )

    val letters = KeyboardLayout(
        KeyboardPage.LETTERS,
        listOf(
            KeyRow(letters("qwertyuiop")),
            KeyRow(letters("asdfghjkl"), inset = 0.5f),
            KeyRow(listOf(Key(KeyAction.Shift, "shift", 1.5f, KeyStyle.FUNCTION)) + letters("zxcvbnm") + listOf(delete)),
            bottomRow(pageKey("123", KeyboardPage.NUMBERS))
        )
    )

    private fun punctuationRow(pageKey: Key) = KeyRow(listOf(pageKey) + chars(".,?!'", 1.4f) + listOf(delete))

    private fun abc() = pageKey("ABC", KeyboardPage.LETTERS)

    val numbers = KeyboardLayout(
        KeyboardPage.NUMBERS,
        listOf(
            KeyRow(chars("1234567890")),
            KeyRow(chars("-/:;()$&@\"")),
            punctuationRow(Key(KeyAction.Page(KeyboardPage.SYMBOLS), "#+=", 1.5f, KeyStyle.FUNCTION)),
            bottomRow(abc())
        )
    )

    val symbols = KeyboardLayout(
        KeyboardPage.SYMBOLS,
        listOf(
            KeyRow(chars("[]{}#%^*+=")),
            KeyRow(chars("_\\|~<>€£¥•")),
            punctuationRow(Key(KeyAction.Page(KeyboardPage.NUMBERS), "123", 1.5f, KeyStyle.FUNCTION)),
            bottomRow(abc())
        )
    )

    val fn = KeyboardLayout(
        KeyboardPage.FN,
        listOf(
            KeyRow((1..10).map { special(SpecialKey.valueOf("F$it")) }),
            KeyRow(
                listOf(
                    SpecialKey.ESCAPE,
                    SpecialKey.TAB,
                    SpecialKey.HOME,
                    SpecialKey.END,
                    SpecialKey.PAGE_UP,
                    SpecialKey.PAGE_DOWN,
                    SpecialKey.INSERT,
                    SpecialKey.F11,
                    SpecialKey.F12
                ).map { special(it) },
                inset = 0.5f
            ),
            KeyRow(
                listOf(SpecialKey.LEFT, SpecialKey.DOWN, SpecialKey.UP, SpecialKey.RIGHT).map { special(it, 1.5f) } +
                    listOf(special(SpecialKey.FORWARD_DELETE, 2.5f), delete)
            ),
            bottomRow(abc())
        )
    )

    fun forPage(page: KeyboardPage): KeyboardLayout = when (page) {
        KeyboardPage.LETTERS -> letters
        KeyboardPage.NUMBERS -> numbers
        KeyboardPage.SYMBOLS -> symbols
        KeyboardPage.FN -> fn
    }
}
