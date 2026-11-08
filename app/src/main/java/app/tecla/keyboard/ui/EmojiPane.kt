package app.tecla.keyboard.ui

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Paint
import android.graphics.drawable.GradientDrawable
import android.os.Handler
import android.os.Looper
import android.util.TypedValue
import android.view.Gravity
import android.view.HapticFeedbackConstants
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.widget.BaseAdapter
import android.widget.GridView
import android.widget.LinearLayout
import android.widget.TextView
import app.tecla.keyboard.logic.EmojiData
import app.tecla.keyboard.logic.EmojiGroup

/** Emoji picker shown in place of the keys: a scrolling grid with category tabs, ABC and delete. */
@SuppressLint("ViewConstructor")
class EmojiPane(context: Context, private val listener: Listener) : LinearLayout(context) {

    interface Listener {
        fun onEmoji(emoji: String)
        fun onEmojiDelete()
        fun onEmojiClose()
    }

    private fun dp(v: Float) = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, v, resources.displayMetrics)

    private val prefs = context.getSharedPreferences("emoji", Context.MODE_PRIVATE)
    private var theme = KeyboardTheme.BLACKOUT
    private val groups: List<EmojiGroup> by lazy { loadGroups() }
    private var recent: List<String> = prefs.getString(KEY_RECENT, "").orEmpty().split(' ').filter { it.isNotBlank() }
    private var selected = -1 // -1 = recent
    private var shown: List<String> = emptyList()
    private val tabs = mutableListOf<TextView>()
    private val handler = Handler(Looper.getMainLooper())

    private val adapter = object : BaseAdapter() {
        override fun getCount() = shown.size
        override fun getItem(position: Int) = shown[position]
        override fun getItemId(position: Int) = position.toLong()
        override fun getView(position: Int, convertView: View?, parent: ViewGroup?): View {
            val tv = convertView as? TextView ?: TextView(context).apply {
                gravity = Gravity.CENTER
                setTextSize(TypedValue.COMPLEX_UNIT_SP, 28f)
                layoutParams = ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(48f).toInt())
            }
            tv.text = shown[position]
            return tv
        }
    }

    private val grid = GridView(context).apply {
        columnWidth = dp(46f).toInt()
        numColumns = GridView.AUTO_FIT
        stretchMode = GridView.STRETCH_COLUMN_WIDTH
        isVerticalScrollBarEnabled = false
        adapter = this@EmojiPane.adapter
        setOnItemClickListener { view, _, position, _ ->
            val emoji = shown[position]
            view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
            listener.onEmoji(emoji)
            recent = EmojiData.addRecent(recent, emoji)
            prefs.edit().putString(KEY_RECENT, recent.joinToString(" ")).apply()
        }
    }

    private val bar = LinearLayout(context).apply { orientation = HORIZONTAL }

    init {
        orientation = VERTICAL
        addView(grid, LayoutParams(LayoutParams.MATCH_PARENT, 0, 1f))
        addView(bar, LayoutParams(LayoutParams.MATCH_PARENT, dp(46f).toInt()))
        buildBar()
        applyTheme()
    }

    fun setTheme(newTheme: KeyboardTheme) {
        theme = newTheme
        applyTheme()
    }

    /** Called each time the pane opens: starts on recents if there are any. */
    fun open() {
        select(if (recent.isNotEmpty()) -1 else 0)
    }

    private fun loadGroups(): List<EmojiGroup> {
        val text = context.assets.open("emoji.txt").bufferedReader().use { it.readText() }
        // Hide emoji this phone's font cannot draw (newer than the system emoji font).
        val paint = Paint()
        return EmojiData.parse(text).map { g -> g.copy(emoji = g.emoji.filter { paint.hasGlyph(it) }) }
    }

    private fun tabIcon(name: String) = GROUP_ICONS[name] ?: name.take(1)

    private fun buildBar() {
        bar.addView(barButton("ABC", 1.4f) { listener.onEmojiClose() })
        val recentTab = barButton("🕘", 1f) { select(-1) }
        tabs += recentTab
        bar.addView(recentTab)
        GROUP_ICONS.keys.forEachIndexed { i, name ->
            val tab = barButton(tabIcon(name), 1f) { select(i) }
            tabs += tab
            bar.addView(tab)
        }
        bar.addView(deleteButton())
    }

    private fun barButton(label: String, weight: Float, onClick: () -> Unit) = TextView(context).apply {
        text = label
        gravity = Gravity.CENTER
        setTextSize(TypedValue.COMPLEX_UNIT_SP, if (label.length > 2) 15f else 18f)
        layoutParams = LayoutParams(0, LayoutParams.MATCH_PARENT, weight)
        setOnClickListener {
            it.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
            onClick()
        }
    }

    /** Delete button that repeats while held, like the keyboard's delete key. */
    @SuppressLint("ClickableViewAccessibility")
    private fun deleteButton() = barButton("⌫", 1.4f) {}.apply {
        setOnClickListener(null)
        val repeat = object : Runnable {
            override fun run() {
                listener.onEmojiDelete()
                handler.postDelayed(this, 55L)
            }
        }
        setOnTouchListener { v, event ->
            when (event.actionMasked) {
                MotionEvent.ACTION_DOWN -> {
                    v.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
                    listener.onEmojiDelete()
                    handler.postDelayed(repeat, 400L)
                }
                MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> handler.removeCallbacks(repeat)
            }
            true
        }
    }

    private fun select(index: Int) {
        selected = index
        shown = if (index < 0) recent else groups.firstOrNull { it.name == GROUP_ICONS.keys.elementAt(index) }?.emoji.orEmpty()
        adapter.notifyDataSetChanged()
        grid.setSelection(0)
        applyTheme()
    }

    private fun applyTheme() {
        setBackgroundColor(theme.background)
        bar.setBackgroundColor(theme.background)
        tabs.forEachIndexed { i, tab ->
            val active = i - 1 == selected
            tab.background = if (active) {
                GradientDrawable().apply {
                    setColor(theme.key)
                    cornerRadius = dp(8f)
                }
            } else {
                null
            }
        }
        for (i in 0 until bar.childCount) (bar.getChildAt(i) as? TextView)?.setTextColor(theme.keyText)
    }

    private companion object {
        const val KEY_RECENT = "recent"

        /** Tab icon for each Unicode emoji group, in display order. */
        val GROUP_ICONS = linkedMapOf(
            "Smileys & Emotion" to "😀",
            "People & Body" to "👋",
            "Animals & Nature" to "🐻",
            "Food & Drink" to "🍔",
            "Travel & Places" to "🚗",
            "Activities" to "⚽",
            "Objects" to "💡",
            "Symbols" to "❤️",
            "Flags" to "🏁"
        )
    }
}
