package app.tecla.keyboard.logic

import android.view.inputmethod.EditorInfo
import org.junit.Assert.assertEquals
import org.junit.Test

class EnterKeyStyleTest {
    @Test
    fun `plain fields get the return icon`() {
        assertEquals(EnterKeyStyle(label = null, accent = false), enterKeyStyle(EditorInfo.IME_ACTION_UNSPECIFIED))
        assertEquals(EnterKeyStyle(label = null, accent = false), enterKeyStyle(EditorInfo.IME_ACTION_NONE))
    }

    @Test
    fun `fields that ask for no enter action get the return icon`() {
        val options = EditorInfo.IME_ACTION_SEARCH or EditorInfo.IME_FLAG_NO_ENTER_ACTION
        assertEquals(EnterKeyStyle(label = null, accent = false), enterKeyStyle(options))
    }

    @Test
    fun `editor actions keep their word, main actions are accented`() {
        assertEquals(EnterKeyStyle("go", true), enterKeyStyle(EditorInfo.IME_ACTION_GO))
        assertEquals(EnterKeyStyle("search", true), enterKeyStyle(EditorInfo.IME_ACTION_SEARCH))
        assertEquals(EnterKeyStyle("send", true), enterKeyStyle(EditorInfo.IME_ACTION_SEND))
        assertEquals(EnterKeyStyle("done", true), enterKeyStyle(EditorInfo.IME_ACTION_DONE))
        assertEquals(EnterKeyStyle("next", false), enterKeyStyle(EditorInfo.IME_ACTION_NEXT))
        assertEquals(EnterKeyStyle("previous", false), enterKeyStyle(EditorInfo.IME_ACTION_PREVIOUS))
    }
}
