package app.tecla.keyboard.logic

import android.view.inputmethod.EditorInfo

/** How the return key looks. A null [label] means the return arrow icon; [accent] highlights main actions. */
data class EnterKeyStyle(val label: String?, val accent: Boolean)

/** Picks the return key's look from a text field's [imeOptions] (see [EditorInfo.imeOptions]). */
fun enterKeyStyle(imeOptions: Int): EnterKeyStyle {
    val icon = EnterKeyStyle(label = null, accent = false)
    if (imeOptions and EditorInfo.IME_FLAG_NO_ENTER_ACTION != 0) return icon
    return when (imeOptions and EditorInfo.IME_MASK_ACTION) {
        EditorInfo.IME_ACTION_GO -> EnterKeyStyle("go", true)
        EditorInfo.IME_ACTION_SEARCH -> EnterKeyStyle("search", true)
        EditorInfo.IME_ACTION_SEND -> EnterKeyStyle("send", true)
        EditorInfo.IME_ACTION_DONE -> EnterKeyStyle("done", true)
        EditorInfo.IME_ACTION_NEXT -> EnterKeyStyle("next", false)
        EditorInfo.IME_ACTION_PREVIOUS -> EnterKeyStyle("previous", false)
        else -> icon
    }
}
