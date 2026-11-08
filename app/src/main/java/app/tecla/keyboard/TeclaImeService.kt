package app.tecla.keyboard

import android.inputmethodservice.InputMethodService
import android.os.Build
import android.os.SystemClock
import android.text.InputType
import android.view.KeyCharacterMap
import android.view.KeyEvent
import android.view.View
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputConnection
import android.view.inputmethod.InputMethodManager
import app.tecla.keyboard.logic.DeleteOp
import app.tecla.keyboard.logic.ShiftController
import app.tecla.keyboard.logic.TextEdits
import app.tecla.keyboard.model.KeyboardPage
import app.tecla.keyboard.model.Layouts
import app.tecla.keyboard.ui.KeyboardTheme
import app.tecla.keyboard.ui.KeyboardView
import kotlin.math.abs

class TeclaImeService :
    InputMethodService(),
    KeyboardView.Listener {

    private var keyboardView: KeyboardView? = null
    private val shift = ShiftController()
    private var page = KeyboardPage.LETTERS
    private var selStart = 0
    private var selEnd = 0

    override fun onCreateInputView(): View = KeyboardView(this, this).also {
        keyboardView = it
        applyTheme()
        render()
    }

    override fun onEvaluateFullscreenMode(): Boolean = false

    override fun onStartInputView(info: EditorInfo, restarting: Boolean) {
        super.onStartInputView(info, restarting)
        page = when (info.inputType and InputType.TYPE_MASK_CLASS) {
            InputType.TYPE_CLASS_NUMBER, InputType.TYPE_CLASS_PHONE, InputType.TYPE_CLASS_DATETIME -> KeyboardPage.NUMBERS
            else -> KeyboardPage.LETTERS
        }
        if (!restarting) shift.reset()
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
        if (isFullscreenMode || !view.isShown) return
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
        window?.window?.navigationBarColor = theme.background
    }

    private fun render() {
        val view = keyboardView ?: return
        val (label, accent) = enterKeyStyle(currentInputEditorInfo)
        view.setState(Layouts.forPage(page), shift.mode, shift.isUpperCase, label, accent)
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

    private fun sendKey(code: Int, meta: Int = 0) {
        val ic = currentInputConnection ?: return
        val now = SystemClock.uptimeMillis()
        val flags = KeyEvent.FLAG_SOFT_KEYBOARD or KeyEvent.FLAG_KEEP_TOUCH_MODE
        ic.sendKeyEvent(KeyEvent(now, now, KeyEvent.ACTION_DOWN, code, 0, meta, KeyCharacterMap.VIRTUAL_KEYBOARD, 0, flags))
        ic.sendKeyEvent(KeyEvent(now, now, KeyEvent.ACTION_UP, code, 0, meta, KeyCharacterMap.VIRTUAL_KEYBOARD, 0, flags))
    }

    override fun onKeyText(text: String) {
        val ic = currentInputConnection ?: return
        val out = if (shift.isUpperCase && page == KeyboardPage.LETTERS) text.uppercase() else text
        ic.commitText(out, 1)
        shift.onCharacterTyped()
        if (page != KeyboardPage.LETTERS && text == "'") page = KeyboardPage.LETTERS
        render()
    }

    override fun onShiftDown() {
        shift.onPress(SystemClock.uptimeMillis())
        render()
    }

    override fun onShiftUp() {
        shift.onRelease()
        render()
    }

    override fun onSpace() {
        val ic = currentInputConnection ?: return
        ic.commitText(" ", 1)
        if (page != KeyboardPage.LETTERS) page = KeyboardPage.LETTERS
        render()
    }

    override fun onEnter() {
        val ic = currentInputConnection ?: return
        val info = currentInputEditorInfo
        val action = info?.imeOptions?.and(EditorInfo.IME_MASK_ACTION) ?: EditorInfo.IME_ACTION_NONE
        val noEnterAction = info == null || info.imeOptions and EditorInfo.IME_FLAG_NO_ENTER_ACTION != 0
        when {
            !noEnterAction && action != EditorInfo.IME_ACTION_NONE && action != EditorInfo.IME_ACTION_UNSPECIFIED ->
                ic.performEditorAction(action)
            info == null || info.inputType == InputType.TYPE_NULL -> sendKey(KeyEvent.KEYCODE_ENTER)
            else -> ic.commitText("\n", 1)
        }
    }

    override fun onDelete(op: DeleteOp, count: Int) {
        val ic = currentInputConnection ?: return
        if (op == DeleteOp.CHAR_BACK) {
            repeat(count) { sendKey(KeyEvent.KEYCODE_DEL) }
            return
        }
        if (op == DeleteOp.CHAR_FORWARD) {
            repeat(count) { sendKey(KeyEvent.KEYCODE_FORWARD_DEL) }
            return
        }
        if (selStart != selEnd) {
            ic.commitText("", 1)
            return
        }
        when (op) {
            DeleteOp.WORD_BACK -> deleteBefore(ic, count, KeyEvent.KEYCODE_DEL) { TextEdits.wordsLengthBefore(it, count) }
            DeleteOp.LINE_BACK -> deleteBefore(ic, 1, KeyEvent.KEYCODE_DEL) { TextEdits.lineLengthBefore(it) }
            DeleteOp.WORD_FORWARD -> {
                val after = ic.getTextAfterCursor(CONTEXT_CHARS, 0)
                if (after == null) {
                    repeat(count) { sendKey(KeyEvent.KEYCODE_FORWARD_DEL, KeyEvent.META_CTRL_ON or KeyEvent.META_CTRL_LEFT_ON) }
                } else {
                    val n = TextEdits.wordsLengthAfter(after, count)
                    if (n > 0) ic.deleteSurroundingText(0, n)
                }
            }
            else -> Unit
        }
    }

    private inline fun deleteBefore(ic: InputConnection, fallbackCount: Int, fallbackKey: Int, length: (CharSequence) -> Int) {
        val before = ic.getTextBeforeCursor(CONTEXT_CHARS, 0)
        if (before == null) {
            // Editors that hide their text (e.g. terminals) still understand Ctrl+Backspace.
            repeat(fallbackCount) { sendKey(fallbackKey, KeyEvent.META_CTRL_ON or KeyEvent.META_CTRL_LEFT_ON) }
            return
        }
        val n = length(before)
        if (n > 0) ic.deleteSurroundingText(n, 0)
    }

    override fun onSwitchKeyboard() {
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

    override fun onPageChange(page: KeyboardPage) {
        this.page = page
        render()
    }

    override fun onCursorMove(dx: Int, dy: Int, selecting: Boolean) {
        val meta = if (selecting) KeyEvent.META_SHIFT_ON or KeyEvent.META_SHIFT_LEFT_ON else 0
        val h = if (dx < 0) KeyEvent.KEYCODE_DPAD_LEFT else KeyEvent.KEYCODE_DPAD_RIGHT
        val v = if (dy < 0) KeyEvent.KEYCODE_DPAD_UP else KeyEvent.KEYCODE_DPAD_DOWN
        repeat(abs(dx)) { sendKey(h, meta) }
        repeat(abs(dy)) { sendKey(v, meta) }
    }

    private companion object {
        const val CONTEXT_CHARS = 2000
    }
}
