package app.tecla.keyboard

import android.content.Intent
import android.inputmethodservice.InputMethodService
import android.os.Build
import android.os.SystemClock
import android.text.InputType
import android.view.KeyCharacterMap
import android.view.KeyEvent
import android.view.View
import android.view.ViewGroup
import android.view.WindowInsets
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputConnection
import android.view.inputmethod.InputMethodManager
import android.widget.FrameLayout
import android.widget.LinearLayout
import app.tecla.keyboard.logic.Accents
import app.tecla.keyboard.logic.DeleteOp
import app.tecla.keyboard.logic.ShiftController
import app.tecla.keyboard.logic.TextEdits
import app.tecla.keyboard.model.Accent
import app.tecla.keyboard.model.KeyAction
import app.tecla.keyboard.model.KeyboardPage
import app.tecla.keyboard.model.Layouts
import app.tecla.keyboard.model.SpecialKey
import app.tecla.keyboard.ui.EmojiPane
import app.tecla.keyboard.ui.KeyboardState
import app.tecla.keyboard.ui.KeyboardTheme
import app.tecla.keyboard.ui.KeyboardView
import kotlin.math.abs

class TeclaImeService :
    InputMethodService(),
    KeyboardView.Listener,
    EmojiPane.Listener {

    private var keyboardView: KeyboardView? = null
    private var emojiPane: EmojiPane? = null
    private var navFiller: View? = null
    private val shift = ShiftController()
    private var page = KeyboardPage.LETTERS
    private var selStart = 0
    private var selEnd = 0
    private var pendingAccent: Accent? = null
    private var ctrl = false
    private var alt = false

    override fun onCreateInputView(): View {
        val keys = FrameLayout(this)
        val keyboard = KeyboardView(this, this)
        keys.addView(keyboard, FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT))
        val pane = EmojiPane(this, this)
        val paneParams = FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, keyboard.keysHeight).apply {
            topMargin = keyboard.keysTop
        }
        keys.addView(pane, paneParams)
        pane.visibility = View.GONE

        // From Android 15 the keyboard window is drawn behind the navigation bar: fill that area with
        // the theme background so the keys sit above the gesture bar and the switch/hide buttons.
        val filler = View(this)
        val root = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        root.addView(keys, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT))
        root.addView(filler, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0))
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.VANILLA_ICE_CREAM) {
            root.setOnApplyWindowInsetsListener { _, insets ->
                // systemBars covers both the gesture bar (navigationBars) and the bar with the keyboard's
                // own switch/hide buttons, which Android reports as a caption bar.
                val bottom = insets.getInsets(WindowInsets.Type.systemBars()).bottom
                val params = filler.layoutParams
                if (params.height != bottom) {
                    params.height = bottom
                    filler.layoutParams = params
                }
                insets
            }
        }

        keyboardView = keyboard
        emojiPane = pane
        navFiller = filler
        applyTheme()
        render()
        return root
    }

    override fun onEvaluateFullscreenMode(): Boolean = false

    override fun onStartInputView(info: EditorInfo, restarting: Boolean) {
        super.onStartInputView(info, restarting)
        page = when (info.inputType and InputType.TYPE_MASK_CLASS) {
            InputType.TYPE_CLASS_NUMBER, InputType.TYPE_CLASS_PHONE, InputType.TYPE_CLASS_DATETIME -> KeyboardPage.NUMBERS
            else -> KeyboardPage.LETTERS
        }
        if (!restarting) {
            shift.reset()
            pendingAccent = null
            ctrl = false
            alt = false
            showEmoji(false)
        }
        selStart = info.initialSelStart
        selEnd = info.initialSelEnd
        applyTheme()
        refreshAutoCaps()
        render()
    }

    override fun onFinishInputView(finishingInput: Boolean) {
        keyboardView?.cancelAll()
        super.onFinishInputView(finishingInput)
    }

    override fun onUpdateSelection(
        oldSelStart: Int,
        oldSelEnd: Int,
        newSelStart: Int,
        newSelEnd: Int,
        candidatesStart: Int,
        candidatesEnd: Int
    ) {
        super.onUpdateSelection(oldSelStart, oldSelEnd, newSelStart, newSelEnd, candidatesStart, candidatesEnd)
        selStart = newSelStart
        selEnd = newSelEnd
        refreshAutoCaps()
        render()
    }

    /** Keep the app visible and touchable behind the transparent balloon strip above the keys. */
    override fun onComputeInsets(outInsets: Insets) {
        super.onComputeInsets(outInsets)
        val view = keyboardView ?: return
        val decor = window?.window?.decorView ?: return
        // Check the container, not the keys: the keys are INVISIBLE (but still laid out) while emoji are shown.
        val root = view.parent as? View ?: return
        if (isFullscreenMode || !root.isShown) return
        val loc = IntArray(2)
        view.getLocationInWindow(loc)
        val top = loc[1] + view.keysTop
        outInsets.contentTopInsets = top
        outInsets.visibleTopInsets = top
        outInsets.touchableInsets = Insets.TOUCHABLE_INSETS_REGION
        outInsets.touchableRegion.set(0, top, decor.width, decor.height)
    }

    // ---------------------------------------------------------------- state

    private fun applyTheme() {
        val theme = KeyboardTheme.BLACKOUT
        keyboardView?.setTheme(theme)
        emojiPane?.setTheme(theme)
        navFiller?.setBackgroundColor(theme.background)
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.VANILLA_ICE_CREAM) setLegacyNavigationBarColor(theme.background)
    }

    /** Before Android 15 the system draws the navigation bar under the keyboard; color it to match. */
    @Suppress("DEPRECATION")
    private fun setLegacyNavigationBarColor(color: Int) {
        window?.window?.navigationBarColor = color
    }

    private fun render() {
        val view = keyboardView ?: return
        val (label, accent) = enterKeyStyle(currentInputEditorInfo)
        view.setState(
            KeyboardState(
                layout = Layouts.forPage(page),
                shiftMode = shift.mode,
                upperCase = shift.isUpperCase,
                enterLabel = label,
                enterAccent = accent,
                pendingAccent = pendingAccent,
                ctrl = ctrl,
                alt = alt
            )
        )
    }

    private fun showEmoji(show: Boolean) {
        val pane = emojiPane ?: return
        if (show) pane.open()
        pane.visibility = if (show) View.VISIBLE else View.GONE
        // Keep the keyboard laid out (INVISIBLE, not GONE) so the window keeps its height.
        keyboardView?.visibility = if (show) View.INVISIBLE else View.VISIBLE
        keyboardView?.cancelAll()
    }

    private fun isTextField(info: EditorInfo?): Boolean =
        info != null && (info.inputType and InputType.TYPE_MASK_CLASS) == InputType.TYPE_CLASS_TEXT

    private fun isProseField(info: EditorInfo?): Boolean {
        if (!isTextField(info)) return false
        return when (info!!.inputType and InputType.TYPE_MASK_VARIATION) {
            InputType.TYPE_TEXT_VARIATION_PASSWORD,
            InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD,
            InputType.TYPE_TEXT_VARIATION_WEB_PASSWORD,
            InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS,
            InputType.TYPE_TEXT_VARIATION_WEB_EMAIL_ADDRESS,
            InputType.TYPE_TEXT_VARIATION_URI
            -> false
            else -> true
        }
    }

    private fun refreshAutoCaps() {
        val info = currentInputEditorInfo
        val ic = currentInputConnection
        val caps = isProseField(info) && ic != null && selStart == selEnd && ic.getCursorCapsMode(info.inputType) != 0
        shift.applyAutoCaps(caps)
    }

    private fun enterKeyStyle(info: EditorInfo?): Pair<String, Boolean> {
        if (info == null || info.imeOptions and EditorInfo.IME_FLAG_NO_ENTER_ACTION != 0) return "return" to false
        return when (info.imeOptions and EditorInfo.IME_MASK_ACTION) {
            EditorInfo.IME_ACTION_GO -> "go" to true
            EditorInfo.IME_ACTION_SEARCH -> "search" to true
            EditorInfo.IME_ACTION_SEND -> "send" to true
            EditorInfo.IME_ACTION_DONE -> "done" to true
            EditorInfo.IME_ACTION_NEXT -> "next" to false
            EditorInfo.IME_ACTION_PREVIOUS -> "previous" to false
            else -> "return" to false
        }
    }

    // ---------------------------------------------------------------- key events

    private fun sendKeyEvent(action: Int, code: Int, meta: Int) {
        val ic = currentInputConnection ?: return
        val now = SystemClock.uptimeMillis()
        val flags = KeyEvent.FLAG_SOFT_KEYBOARD or KeyEvent.FLAG_KEEP_TOUCH_MODE
        ic.sendKeyEvent(KeyEvent(now, now, action, code, 0, meta, KeyCharacterMap.VIRTUAL_KEYBOARD, 0, flags))
    }

    private fun sendKey(code: Int, meta: Int = 0) {
        sendKeyEvent(KeyEvent.ACTION_DOWN, code, meta)
        sendKeyEvent(KeyEvent.ACTION_UP, code, meta)
    }

    /** Returns the meta state for the latched Ctrl/Alt modifiers and releases them (they are one-shot). */
    private fun consumeModifiers(): Int {
        var meta = 0
        if (ctrl) meta = meta or KeyEvent.META_CTRL_ON or KeyEvent.META_CTRL_LEFT_ON
        if (alt) meta = meta or KeyEvent.META_ALT_ON or KeyEvent.META_ALT_LEFT_ON
        ctrl = false
        alt = false
        return meta
    }

    private fun keyCodeFor(text: String): Int? {
        if (text.length != 1) return null
        val c = text[0].lowercaseChar()
        return when (c) {
            in 'a'..'z' -> KeyEvent.KEYCODE_A + (c - 'a')
            in '0'..'9' -> KeyEvent.KEYCODE_0 + (c - '0')
            ' ' -> KeyEvent.KEYCODE_SPACE
            else -> KeyCharacterMap.load(KeyCharacterMap.VIRTUAL_KEYBOARD).getEvents(charArrayOf(c))
                ?.firstOrNull { it.action == KeyEvent.ACTION_DOWN && !KeyEvent.isModifierKey(it.keyCode) }
                ?.keyCode
        }
    }

    override fun onAction(action: KeyAction) {
        when (action) {
            is KeyAction.Text -> typeText(action.text)
            is KeyAction.Dead -> pendingAccent = if (pendingAccent == action.accent) {
                // Same accent twice types the accent itself.
                currentInputConnection?.commitText(action.accent.spacing, 1)
                null
            } else {
                action.accent
            }
            is KeyAction.Special -> sendKey(keyCodeFor(action.key), consumeModifiers())
            is KeyAction.Page -> page = action.target
            KeyAction.Space -> typeSpace()
            KeyAction.Enter -> enter()
            KeyAction.Ctrl -> ctrl = !ctrl
            KeyAction.Alt -> alt = !alt
            KeyAction.Fn -> page = if (page == KeyboardPage.FN) KeyboardPage.LETTERS else KeyboardPage.FN
            KeyAction.Emoji -> showEmoji(true)
            KeyAction.Settings -> openSettings()
            KeyAction.SwitchKeyboard -> switchKeyboard()
            KeyAction.Shift, KeyAction.Delete, KeyAction.Gap -> Unit
        }
        render()
    }

    private fun keyCodeFor(key: SpecialKey): Int = when (key) {
        SpecialKey.ESCAPE -> KeyEvent.KEYCODE_ESCAPE
        SpecialKey.TAB -> KeyEvent.KEYCODE_TAB
        SpecialKey.HOME -> KeyEvent.KEYCODE_MOVE_HOME
        SpecialKey.END -> KeyEvent.KEYCODE_MOVE_END
        SpecialKey.PAGE_UP -> KeyEvent.KEYCODE_PAGE_UP
        SpecialKey.PAGE_DOWN -> KeyEvent.KEYCODE_PAGE_DOWN
        SpecialKey.INSERT -> KeyEvent.KEYCODE_INSERT
        SpecialKey.FORWARD_DELETE -> KeyEvent.KEYCODE_FORWARD_DEL
        SpecialKey.LEFT -> KeyEvent.KEYCODE_DPAD_LEFT
        SpecialKey.RIGHT -> KeyEvent.KEYCODE_DPAD_RIGHT
        SpecialKey.UP -> KeyEvent.KEYCODE_DPAD_UP
        SpecialKey.DOWN -> KeyEvent.KEYCODE_DPAD_DOWN
        else -> KeyEvent.KEYCODE_F1 + (key.ordinal - SpecialKey.F1.ordinal)
    }

    private fun typeText(text: String) {
        val ic = currentInputConnection ?: return
        var out = if (shift.isUpperCase && page == KeyboardPage.LETTERS) text.uppercase() else text
        val meta = consumeModifiers()
        val code = if (meta != 0) keyCodeFor(text) else null
        if (code != null) {
            sendKey(code, meta)
        } else {
            pendingAccent?.let { out = Accents.compose(it, out) }
            ic.commitText(out, 1)
        }
        pendingAccent = null
        shift.onCharacterTyped()
        if ((page == KeyboardPage.NUMBERS || page == KeyboardPage.SYMBOLS) && text == "'") page = KeyboardPage.LETTERS
    }

    private fun typeSpace() {
        val ic = currentInputConnection ?: return
        val meta = consumeModifiers()
        val accent = pendingAccent
        when {
            accent != null -> ic.commitText(accent.spacing, 1)
            meta != 0 -> sendKey(KeyEvent.KEYCODE_SPACE, meta)
            else -> ic.commitText(" ", 1)
        }
        pendingAccent = null
        if (page == KeyboardPage.NUMBERS || page == KeyboardPage.SYMBOLS) page = KeyboardPage.LETTERS
    }

    private fun enter() {
        val ic = currentInputConnection ?: return
        val info = currentInputEditorInfo
        pendingAccent = null
        val meta = consumeModifiers()
        if (meta != 0) {
            sendKey(KeyEvent.KEYCODE_ENTER, meta)
            return
        }
        val action = info?.imeOptions?.and(EditorInfo.IME_MASK_ACTION) ?: EditorInfo.IME_ACTION_NONE
        val noEnterAction = info == null || info.imeOptions and EditorInfo.IME_FLAG_NO_ENTER_ACTION != 0
        when {
            !noEnterAction && action != EditorInfo.IME_ACTION_NONE && action != EditorInfo.IME_ACTION_UNSPECIFIED ->
                ic.performEditorAction(action)
            info == null || info.inputType == InputType.TYPE_NULL -> sendKey(KeyEvent.KEYCODE_ENTER)
            else -> ic.commitText("\n", 1)
        }
    }

    override fun onShiftDown() {
        shift.onPress()
        render()
    }

    override fun onShiftUp() {
        shift.onRelease()
        render()
    }

    override fun onDelete(op: DeleteOp, count: Int) {
        val ic = currentInputConnection ?: return
        if (pendingAccent != null) {
            // Delete cancels a pending accent instead of deleting text.
            pendingAccent = null
            render()
            return
        }
        when (op) {
            DeleteOp.CHAR_BACK -> {
                val meta = consumeModifiers()
                repeat(count) { sendKey(KeyEvent.KEYCODE_DEL, meta) }
                render()
                return
            }
            DeleteOp.CHAR_FORWARD -> {
                repeat(count) { sendKey(KeyEvent.KEYCODE_FORWARD_DEL) }
                return
            }
            else -> Unit
        }
        if (selStart != selEnd) {
            ic.commitText("", 1)
            return
        }
        when (op) {
            DeleteOp.WORD_BACK -> deleteBefore(ic, count) { TextEdits.wordsLengthBefore(it, count) }
            DeleteOp.LINE_BACK -> deleteBefore(ic, 1) { TextEdits.lineLengthBefore(it) }
            DeleteOp.WORD_FORWARD -> {
                val after = ic.getTextAfterCursor(CONTEXT_CHARS, 0)
                if (after == null) {
                    repeat(count) { sendKey(KeyEvent.KEYCODE_FORWARD_DEL, CTRL_META) }
                } else {
                    val n = TextEdits.wordsLengthAfter(after, count)
                    if (n > 0) ic.deleteSurroundingText(0, n)
                }
            }
            else -> Unit
        }
    }

    private inline fun deleteBefore(ic: InputConnection, fallbackCount: Int, length: (CharSequence) -> Int) {
        val before = ic.getTextBeforeCursor(CONTEXT_CHARS, 0)
        if (before == null) {
            // Editors that hide their text (e.g. terminals) still understand Ctrl+Backspace.
            repeat(fallbackCount) { sendKey(KeyEvent.KEYCODE_DEL, CTRL_META) }
            return
        }
        val n = length(before)
        if (n > 0) ic.deleteSurroundingText(n, 0)
    }

    private fun switchKeyboard() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            if (!switchToNextInputMethod(false)) showPicker()
        } else {
            showPicker()
        }
    }

    override fun onShowKeyboardPicker() = showPicker()

    private fun showPicker() {
        getSystemService(InputMethodManager::class.java)?.showInputMethodPicker()
    }

    private fun openSettings() {
        startActivity(Intent(this, SetupActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
    }

    override fun onCursorMove(dx: Int, dy: Int, selecting: Boolean) {
        val meta = if (selecting) SHIFT_META else 0
        val h = if (dx < 0) KeyEvent.KEYCODE_DPAD_LEFT else KeyEvent.KEYCODE_DPAD_RIGHT
        val v = if (dy < 0) KeyEvent.KEYCODE_DPAD_UP else KeyEvent.KEYCODE_DPAD_DOWN
        // Text fields only extend the selection while they see Shift held down (a Shift flag on the
        // arrow key alone is ignored), so press and release a real Shift key around the arrows.
        if (selecting) sendKeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_SHIFT_LEFT, SHIFT_META)
        repeat(abs(dx)) { sendKey(h, meta) }
        repeat(abs(dy)) { sendKey(v, meta) }
        if (selecting) sendKeyEvent(KeyEvent.ACTION_UP, KeyEvent.KEYCODE_SHIFT_LEFT, 0)
    }

    // ---------------------------------------------------------------- emoji pane

    override fun onEmoji(emoji: String) {
        currentInputConnection?.commitText(emoji, 1)
    }

    override fun onEmojiDelete() {
        sendKey(KeyEvent.KEYCODE_DEL)
    }

    override fun onEmojiClose() {
        showEmoji(false)
        render()
    }

    private companion object {
        const val CONTEXT_CHARS = 2000
        const val CTRL_META = KeyEvent.META_CTRL_ON or KeyEvent.META_CTRL_LEFT_ON
        const val SHIFT_META = KeyEvent.META_SHIFT_ON or KeyEvent.META_SHIFT_LEFT_ON
    }
}
