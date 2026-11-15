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

/**
 * Keys from a physical keyboard that have no character, used on the Fn page and in corners.
 * [hint] is the shorter text shown when the key sits in a corner.
 */
enum class SpecialKey(val label: String, val repeats: Boolean = false, val hint: String = label) {
    ESCAPE("esc"),
    TAB("tab"),
    HOME("home", hint = "|←"),
    END("end", hint = "→|"),
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

/**
 * Whether holding a key with this action keeps typing it: letters, numbers and symbols, plus the
 * Fn page's arrows and forward delete. (Delete has its own repeat that speeds up to whole words.)
 */
val KeyAction.repeatsWhenHeld: Boolean
    get() = this is KeyAction.Text || (this is KeyAction.Special && key.repeats)

/** Where an extra action sits on a key; swipe toward that corner to use it. */
enum class Corner { TOP_LEFT, TOP_RIGHT, BOTTOM_LEFT, BOTTOM_RIGHT }

enum class KeyboardPage { LETTERS, NUMBERS, FN }

enum class KeyStyle { CHARACTER, FUNCTION, SPACE, ENTER, GAP }

/** A key, with its width measured in key units (a full row is [UNITS_PER_ROW] units wide). */
data class Key(
    val action: KeyAction,
    val label: String,
    val width: Float = 1f,
    val style: KeyStyle = KeyStyle.CHARACTER,
    val corners: Map<Corner, KeyAction> = emptyMap()
)

/**
 * What holding this key keeps typing, with the finger in [corner] (null = the key itself), or null
 * if it does not repeat. An accent corner repeats its plain mark (^^^^), since a dead key on its own
 * types nothing.
 */
fun Key.heldAction(corner: Corner?): KeyAction? {
    val target = if (corner == null) action else corners[corner] ?: return null
    return when {
        target is KeyAction.Dead -> KeyAction.Text(target.accent.spacing)
        target.repeatsWhenHeld -> target
        else -> null
    }
}

/** A row of keys. [inset] is empty space on each side, in key units. */
data class KeyRow(val keys: List<Key>, val inset: Float = 0f)

data class KeyboardLayout(val page: KeyboardPage, val rows: List<KeyRow>)

const val UNITS_PER_ROW = 10f

/** iPhone-style layouts with numbers, symbols and accents in the letters' corners (Unexpected Keyboard style). */
object Layouts {
    private val TL = Corner.TOP_LEFT
    private val TR = Corner.TOP_RIGHT
    private val BL = Corner.BOTTOM_LEFT
    private val BR = Corner.BOTTOM_RIGHT

    private fun t(s: String) = KeyAction.Text(s)

    private fun dead(accent: Accent) = KeyAction.Dead(accent)

    /**
     * Swipe-to-corner actions on the letter keys, placed as on Unexpected Keyboard: numbers along
     * the top row's top-right corners, accent dead keys under a s d f g h l (and above u), and the
     * symbols from the old 123 pages around them.
     */
    private val letterCorners: Map<Char, Map<Corner, KeyAction>> = mapOf(
        'q' to mapOf(TR to t("1"), BR to KeyAction.Special(SpecialKey.ESCAPE)),
        'w' to mapOf(TL to t("~"), TR to t("2"), BL to t("@")),
        'e' to mapOf(TL to t("!"), TR to t("3"), BL to t("#"), BR to t("€")),
        'r' to mapOf(TR to t("4"), BL to t("$")),
        't' to mapOf(TR to t("5"), BL to t("%")),
        'y' to mapOf(TR to t("6"), BL to t("^")),
        'u' to mapOf(TL to dead(Accent.DIAERESIS), TR to t("7"), BL to t("&")),
        'i' to mapOf(TR to t("8"), BL to t("*")),
        'o' to mapOf(TR to t("9"), BL to t("("), BR to t(")")),
        'p' to mapOf(TR to t("0")),
        'a' to mapOf(TL to KeyAction.Special(SpecialKey.TAB), TR to t("`"), BR to dead(Accent.GRAVE)),
        's' to mapOf(BL to t("ß"), BR to dead(Accent.ACUTE)),
        'd' to mapOf(BR to dead(Accent.CIRCUMFLEX)),
        'f' to mapOf(BR to dead(Accent.TILDE)),
        'g' to mapOf(TR to t("-"), BL to t("_"), BR to dead(Accent.CEDILLA)),
        'h' to mapOf(TR to t("="), BL to t("+"), BR to dead(Accent.CARON)),
        'j' to mapOf(BL to t("{"), BR to t("}")),
        'k' to mapOf(BL to t("["), BR to t("]")),
        'l' to mapOf(TR to t("|"), BL to t("\\"), BR to dead(Accent.MACRON)),
        'c' to mapOf(TR to t("<"), BL to t(".")),
        'v' to mapOf(TR to t(">"), BL to t(",")),
        'b' to mapOf(TR to t("?"), BL to t("/")),
        'n' to mapOf(TR to t(":"), BL to t(";")),
        'm' to mapOf(TR to t("\""), BL to t("'"))
    )

    private fun letters(s: String): List<Key> = s.map { c ->
        Key(KeyAction.Text(c.toString()), c.toString(), corners = letterCorners[c].orEmpty())
    }

    private fun chars(s: String, width: Float = 1f): List<Key> = s.map { Key(KeyAction.Text(it.toString()), it.toString(), width) }

    private fun special(key: SpecialKey, width: Float = 1f) = Key(KeyAction.Special(key), key.label, width, KeyStyle.FUNCTION)

    private val delete = Key(KeyAction.Delete, "delete", 1.5f, KeyStyle.FUNCTION)

    private val ctrlKey = Key(
        KeyAction.Ctrl,
        "ctrl",
        1.25f,
        KeyStyle.FUNCTION,
        corners = mapOf(TL to KeyAction.Special(SpecialKey.HOME), TR to KeyAction.Special(SpecialKey.END))
    )

    private val enterKey = Key(
        KeyAction.Enter,
        "return",
        2f,
        KeyStyle.ENTER,
        corners = mapOf(TL to KeyAction.Special(SpecialKey.PAGE_UP), BL to KeyAction.Special(SpecialKey.PAGE_DOWN))
    )

    private val fnKey = Key(
        KeyAction.Fn,
        "Fn",
        1.25f,
        KeyStyle.FUNCTION,
        corners = mapOf(
            TL to KeyAction.Alt,
            TR to KeyAction.SwitchKeyboard,
            BL to KeyAction.Emoji,
            BR to KeyAction.Settings
        )
    )

    private val bottomRow = KeyRow(
        listOf(
            ctrlKey,
            fnKey,
            Key(KeyAction.Space, "space", 5.5f, KeyStyle.SPACE),
            enterKey
        )
    )

    val letters = KeyboardLayout(
        KeyboardPage.LETTERS,
        listOf(
            KeyRow(letters("qwertyuiop")),
            KeyRow(letters("asdfghjkl"), inset = 0.5f),
            KeyRow(listOf(Key(KeyAction.Shift, "shift", 1.5f, KeyStyle.FUNCTION)) + letters("zxcvbnm") + listOf(delete)),
            bottomRow
        )
    )

    /** Shown on its own for number, phone and date fields; there is no key that switches to it. */
    val numbers = KeyboardLayout(
        KeyboardPage.NUMBERS,
        listOf(
            KeyRow(chars("1234567890")),
            KeyRow(chars("-/:;()$&@\"")),
            KeyRow(chars("+*#.,", 1.7f) + listOf(delete)),
            bottomRow
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
            bottomRow
        )
    )

    fun forPage(page: KeyboardPage): KeyboardLayout = when (page) {
        KeyboardPage.LETTERS -> letters
        KeyboardPage.NUMBERS -> numbers
        KeyboardPage.FN -> fn
    }
}
