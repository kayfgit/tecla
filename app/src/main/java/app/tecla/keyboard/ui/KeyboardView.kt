package app.tecla.keyboard.ui

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Path
import android.graphics.Rect
import android.graphics.RectF
import android.graphics.Typeface
import android.os.Handler
import android.os.Looper
import android.util.TypedValue
import android.view.HapticFeedbackConstants
import android.view.MotionEvent
import android.view.View
import app.tecla.keyboard.logic.DeleteGestureTracker
import app.tecla.keyboard.logic.DeleteOp
import app.tecla.keyboard.logic.ShiftMode
import app.tecla.keyboard.logic.TrackpadAccumulator
import app.tecla.keyboard.logic.cornerOf
import app.tecla.keyboard.model.Box
import app.tecla.keyboard.model.Corner
import app.tecla.keyboard.model.KeyAction
import app.tecla.keyboard.model.KeyStyle
import app.tecla.keyboard.model.KeyboardGeometry
import app.tecla.keyboard.model.KeyboardPage
import app.tecla.keyboard.model.PlacedKey
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min

/**
 * Draws the keyboard and turns touches into keyboard events.
 * The top [keysTop] pixels are a transparent strip used only for key-press balloons.
 */
@SuppressLint("ViewConstructor")
class KeyboardView(context: Context, private val listener: Listener) : View(context) {

    interface Listener {
        /** A key (or one of its corners) was activated. */
        fun onAction(action: KeyAction)
        fun onShiftDown()
        fun onShiftUp()
        fun onDelete(op: DeleteOp, count: Int)
        fun onShowKeyboardPicker()
        fun onCursorMove(dx: Int, dy: Int, selecting: Boolean)
    }

    private val metrics = resources.displayMetrics
    private fun dp(v: Float) = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, v, metrics)
    private fun sp(v: Float) = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_SP, v, metrics)

    private val previewSpace = dp(64f)
    private val topPadding = dp(8f)
    private val bottomPadding = dp(6f)
    private val rowHeight = dp(54f)
    private val sidePadding = dp(3f)
    private val keyGapH = dp(6f)
    private val keyGapV = dp(11f)
    private val keyRadius = dp(5f)
    private val cornerSwipeThreshold = dp(16f)

    /** Y offset where the keyboard background starts; everything above it is see-through. */
    val keysTop: Int get() = previewSpace.toInt()

    /** Height of the key area below [keysTop]. */
    val keysHeight: Int get() = (topPadding + ROWS * rowHeight + bottomPadding).toInt()

    private var theme = KeyboardTheme.BLACKOUT
    private var state = KeyboardState()
    private var geometry: KeyboardGeometry? = null

    private val fillPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val strokePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = dp(1.5f)
        strokeJoin = Paint.Join.ROUND
        strokeCap = Paint.Cap.ROUND
    }
    private val textPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val textBounds = Rect()
    private val rect = RectF()
    private val path = Path()

    fun setTheme(newTheme: KeyboardTheme) {
        if (theme != newTheme) {
            theme = newTheme
            invalidate()
        }
    }

    fun setState(newState: KeyboardState) {
        if (newState.layout !== state.layout) geometry = null
        state = newState
        invalidate()
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        setMeasuredDimension(MeasureSpec.getSize(widthMeasureSpec), keysTop + keysHeight)
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        geometry = null
    }

    private fun geometry(): KeyboardGeometry = geometry ?: KeyboardGeometry(
        state.layout,
        width.toFloat(),
        previewSpace + topPadding,
        sidePadding,
        rowHeight
    ).also { geometry = it }

    // ---------------------------------------------------------------- drawing

    override fun onDraw(canvas: Canvas) {
        fillPaint.color = theme.background
        canvas.drawRect(0f, previewSpace, width.toFloat(), height.toFloat(), fillPaint)
        val cursorMode = pointers.values.any { it.mode == Mode.CURSOR }
        for (pk in geometry().keys) drawKey(canvas, pk, cursorMode)
        for (p in pointers.values) {
            if (p.mode == Mode.PRESS && p.key.key.style == KeyStyle.CHARACTER) drawBalloon(canvas, p)
        }
    }

    private fun keyRect(cell: Box): RectF {
        rect.set(cell.left + keyGapH / 2, cell.top + keyGapV / 2, cell.right - keyGapH / 2, cell.bottom - keyGapV / 2)
        return rect
    }

    private fun isPressed(pk: PlacedKey) = pointers.values.any { it.key === pk && it.mode != Mode.DONE }

    private fun letterCase(label: String) = if (state.upperCase && state.layout.page == KeyboardPage.LETTERS) label.uppercase() else label

    private fun drawKey(canvas: Canvas, pk: PlacedKey, cursorMode: Boolean) {
        val key = pk.key
        val pressed = isPressed(pk)
        val shiftActive = key.action == KeyAction.Shift && state.shiftMode != ShiftMode.OFF
        val accent = key.style == KeyStyle.ENTER && state.enterAccent
        val bg = when {
            accent -> if (pressed) theme.functionKey else theme.accent
            shiftActive -> theme.keyText
            key.style == KeyStyle.CHARACTER -> theme.key
            key.style == KeyStyle.SPACE -> if (pressed && !cursorMode) theme.functionKey else theme.key
            else -> if (pressed) theme.key else theme.functionKey
        }
        val r = keyRect(pk.cell)
        fillPaint.color = theme.shadow
        r.offset(0f, dp(1f))
        canvas.drawRoundRect(r, keyRadius, keyRadius, fillPaint)
        r.offset(0f, -dp(1f))
        fillPaint.color = bg
        canvas.drawRoundRect(r, keyRadius, keyRadius, fillPaint)
        if (cursorMode) return

        val fg = if (accent) theme.accentText else theme.keyText
        val cx = r.centerX()
        val cy = r.centerY()
        when (val action = key.action) {
            KeyAction.Shift -> drawShift(canvas, cx, cy, fg)
            KeyAction.Delete -> drawDelete(canvas, r, fg, pressed)
            KeyAction.Enter -> drawLabel(canvas, state.enterLabel, cx, cy, sp(16f), fg)
            KeyAction.Fn -> drawLabel(canvas, key.label, cx, cy, sp(16f), if (state.layout.page == KeyboardPage.FN) theme.accent else fg)
            is KeyAction.Text -> drawLabel(canvas, letterCase(key.label), cx, cy, sp(23f), fg)
            is KeyAction.Special -> drawLabel(canvas, key.label, cx, cy, sp(if (action.key.label.length > 3) 13f else 17f), fg)
            else -> drawLabel(canvas, key.label, cx, cy, sp(16f), fg)
        }
        for ((corner, cornerAction) in key.corners) drawCorner(canvas, r, corner, cornerAction)
    }

    private fun cornerLabel(action: KeyAction): String = when (action) {
        is KeyAction.Text -> action.text
        is KeyAction.Dead -> action.accent.label
        KeyAction.Ctrl -> "ctrl"
        KeyAction.Alt -> "alt"
        KeyAction.SwitchKeyboard -> "⌨︎"
        KeyAction.Emoji -> "☺︎"
        KeyAction.Settings -> "⚙︎"
        else -> ""
    }

    private fun isCornerActive(action: KeyAction) = when (action) {
        is KeyAction.Dead -> action.accent == state.pendingAccent
        KeyAction.Ctrl -> state.ctrl
        KeyAction.Alt -> state.alt
        else -> false
    }

    /** Small hint for a corner swipe, Unexpected Keyboard style. Active modifiers light up. */
    private fun drawCorner(canvas: Canvas, r: RectF, corner: Corner, action: KeyAction) {
        if (action is KeyAction.Dead) {
            drawAccentHint(canvas, r, corner, action)
            return
        }
        val label = cornerLabel(action)
        val word = label.length > 2
        val size = sp(if (word) 9.5f else 11f)
        val color = if (isCornerActive(action)) theme.accent else theme.hint
        val left = corner == Corner.TOP_LEFT || corner == Corner.BOTTOM_LEFT
        val top = corner == Corner.TOP_LEFT || corner == Corner.TOP_RIGHT
        val inset = dp(4f)
        val x = if (left) r.left + inset else r.right - inset
        val y = if (top) r.top + dp(8f) else r.bottom - dp(8f)
        drawLabel(canvas, label, x, y, size, color, align = if (left) Paint.Align.LEFT else Paint.Align.RIGHT)
    }

    /**
     * Accent hints are drawn as large, bold, bright marks (´ ` ^ ~ ¸ ˇ ¯ ¨) centered on their visible
     * shape, so they are easy to tell apart at arm's length.
     */
    private fun drawAccentHint(canvas: Canvas, r: RectF, corner: Corner, action: KeyAction.Dead) {
        val mark = action.accent.spacing
        textPaint.textSize = sp(20f)
        textPaint.typeface = Typeface.DEFAULT_BOLD
        textPaint.textAlign = Paint.Align.LEFT
        textPaint.color = if (isCornerActive(action)) theme.accent else theme.accentHint
        textPaint.getTextBounds(mark, 0, mark.length, textBounds)
        val left = corner == Corner.TOP_LEFT || corner == Corner.BOTTOM_LEFT
        val top = corner == Corner.TOP_LEFT || corner == Corner.TOP_RIGHT
        val cx = if (left) r.left + dp(8f) else r.right - dp(8f)
        val cy = if (top) r.top + dp(8f) else r.bottom - dp(8f)
        canvas.drawText(mark, cx - textBounds.exactCenterX(), cy - textBounds.exactCenterY(), textPaint)
    }

    private fun drawLabel(
        canvas: Canvas,
        text: String,
        x: Float,
        cy: Float,
        size: Float,
        color: Int,
        bold: Boolean = false,
        align: Paint.Align = Paint.Align.CENTER
    ) {
        textPaint.textSize = size
        textPaint.color = color
        textPaint.textAlign = align
        textPaint.typeface = if (bold) Typeface.DEFAULT_BOLD else Typeface.DEFAULT
        val fm = textPaint.fontMetrics
        canvas.drawText(text, x, cy - (fm.ascent + fm.descent) / 2f, textPaint)
    }

    private fun drawShift(canvas: Canvas, cx: Float, cy: Float, color: Int) {
        val s = dp(18f)
        val mode = state.shiftMode
        val y = if (mode == ShiftMode.LOCKED) cy - dp(2f) else cy
        path.reset()
        path.moveTo(cx, y - s / 2)
        path.lineTo(cx + s / 2, y)
        path.lineTo(cx + s / 4.5f, y)
        path.lineTo(cx + s / 4.5f, y + s / 2)
        path.lineTo(cx - s / 4.5f, y + s / 2)
        path.lineTo(cx - s / 4.5f, y)
        path.lineTo(cx - s / 2, y)
        path.close()
        val shapeColor = if (mode == ShiftMode.OFF) color else theme.background
        if (mode == ShiftMode.OFF) {
            strokePaint.color = shapeColor
            canvas.drawPath(path, strokePaint)
        } else {
            fillPaint.color = shapeColor
            canvas.drawPath(path, fillPaint)
        }
        if (mode == ShiftMode.LOCKED) {
            fillPaint.color = shapeColor
            canvas.drawRect(cx - s / 4.5f, y + s / 2 + dp(2.5f), cx + s / 4.5f, y + s / 2 + dp(4f), fillPaint)
        }
    }

    /**
     * Delete key in the Unexpected Keyboard style: a big backspace in the middle and small icons on
     * each side showing the swipe directions (word back, word forward, letter forward, line back).
     */
    private fun drawDelete(canvas: Canvas, r: RectF, color: Int, pressed: Boolean) {
        val cx = r.centerX()
        val cy = r.centerY()
        val oldWidth = strokePaint.strokeWidth

        val w = min(dp(21f), r.width() * 0.4f)
        val h = w * 0.7f
        tagPath(cx, cy, w, h, pointLeft = true)
        if (pressed) {
            fillPaint.color = color
            canvas.drawPath(path, fillPaint)
        }
        strokePaint.color = color
        canvas.drawPath(path, strokePaint)
        drawCross(canvas, cx + h * 0.2f, cy, h * 0.2f, if (pressed) theme.key else color)

        strokePaint.strokeWidth = dp(1f)
        val sw = dp(12f)
        val sh = dp(9f)
        val hint = theme.hint
        strokePaint.color = hint
        val leftX = r.left + dp(3f) + sw / 2
        tagPath(leftX, cy, sw, sh, pointLeft = true)
        canvas.drawPath(path, strokePaint)
        drawLabel(canvas, "W", leftX + sh * 0.15f, cy, sp(6.5f), hint, bold = true)

        val rightX = r.right - dp(3f) - sw / 2
        tagPath(rightX, cy, sw, sh, pointLeft = false)
        canvas.drawPath(path, strokePaint)
        drawLabel(canvas, "W", rightX - sh * 0.15f, cy, sp(6.5f), hint, bold = true)

        val topY = r.top + dp(6.5f)
        tagPath(cx, topY, dp(11f), dp(8f), pointLeft = false)
        canvas.drawPath(path, strokePaint)
        drawCross(canvas, cx - dp(1.2f), topY, dp(1.6f), hint)

        val botY = r.bottom - dp(6.5f)
        canvas.drawLine(cx - dp(5f), botY - dp(3.5f), cx - dp(5f), botY + dp(3.5f), strokePaint)
        canvas.drawLine(cx - dp(3.5f), botY, cx + dp(5f), botY, strokePaint)
        canvas.drawLine(cx - dp(3.5f), botY, cx - dp(1f), botY - dp(2.5f), strokePaint)
        canvas.drawLine(cx - dp(3.5f), botY, cx - dp(1f), botY + dp(2.5f), strokePaint)
        strokePaint.strokeWidth = oldWidth
    }

    /** Builds a backspace-style tag shape (a box with one pointed end) into [path]. */
    private fun tagPath(cx: Float, cy: Float, w: Float, h: Float, pointLeft: Boolean) {
        val l = cx - w / 2
        val r = cx + w / 2
        val t = cy - h / 2
        val b = cy + h / 2
        val tip = h / 2
        path.reset()
        if (pointLeft) {
            path.moveTo(l, cy)
            path.lineTo(l + tip, t)
            path.lineTo(r, t)
            path.lineTo(r, b)
            path.lineTo(l + tip, b)
        } else {
            path.moveTo(r, cy)
            path.lineTo(r - tip, t)
            path.lineTo(l, t)
            path.lineTo(l, b)
            path.lineTo(r - tip, b)
        }
        path.close()
    }

    private fun drawCross(canvas: Canvas, cx: Float, cy: Float, d: Float, color: Int) {
        val old = strokePaint.color
        strokePaint.color = color
        canvas.drawLine(cx - d, cy - d, cx + d, cy + d, strokePaint)
        canvas.drawLine(cx - d, cy + d, cx + d, cy - d, strokePaint)
        strokePaint.color = old
    }

    /** iPhone-style magnified key above the pressed key; shows the corner character when swiping to one. */
    private fun drawBalloon(canvas: Canvas, p: Pointer) {
        val pk = p.key
        val k = RectF(keyRect(pk.cell))
        val kh = k.height()
        val extra = dp(10f)
        val headW = k.width() + 2 * extra
        var headLeft = k.centerX() - headW / 2
        headLeft = headLeft.coerceIn(dp(2f), width - dp(2f) - headW)
        val headRight = headLeft + headW
        val headBottom = k.top - kh * 0.1f
        val headTop = max(dp(2f), headBottom - kh * 1.15f)
        val neckEnd = k.top + kh * 0.35f
        val rad = keyRadius * 1.6f
        val n = (neckEnd - headBottom) / 2

        path.reset()
        path.moveTo(headLeft + rad, headTop)
        path.lineTo(headRight - rad, headTop)
        path.quadTo(headRight, headTop, headRight, headTop + rad)
        path.lineTo(headRight, headBottom)
        path.cubicTo(headRight, headBottom + n, k.right, neckEnd - n, k.right, neckEnd)
        path.lineTo(k.right, k.bottom - keyRadius)
        path.quadTo(k.right, k.bottom, k.right - keyRadius, k.bottom)
        path.lineTo(k.left + keyRadius, k.bottom)
        path.quadTo(k.left, k.bottom, k.left, k.bottom - keyRadius)
        path.lineTo(k.left, neckEnd)
        path.cubicTo(k.left, neckEnd - n, headLeft, headBottom + n, headLeft, headBottom)
        path.lineTo(headLeft, headTop + rad)
        path.quadTo(headLeft, headTop, headLeft + rad, headTop)
        path.close()

        fillPaint.color = theme.key
        canvas.drawPath(path, fillPaint)
        strokePaint.color = theme.hint
        val oldWidth = strokePaint.strokeWidth
        strokePaint.strokeWidth = dp(0.75f)
        canvas.drawPath(path, strokePaint)
        strokePaint.strokeWidth = oldWidth

        val cornerAction = p.corner?.let { pk.key.corners[it] }
        val label = if (cornerAction != null) cornerLabel(cornerAction) else letterCase(pk.key.label)
        val color = if (cornerAction != null) theme.accent else theme.keyText
        drawLabel(canvas, label, (headLeft + headRight) / 2, (headTop + headBottom) / 2, sp(34f), color)
    }

    // ---------------------------------------------------------------- touch

    private enum class Mode { PRESS, CURSOR, DELETE_GESTURE, DELETE_REPEAT, KEY_REPEAT, DONE }

    private inner class Pointer(val id: Int, val key: PlacedKey, val startX: Float, val startY: Float) {
        var lastX = startX
        var lastY = startY
        var mode = Mode.PRESS
        var corner: Corner? = null
        val deleteTracker = DeleteGestureTracker(dp(18f))
        val trackpad = TrackpadAccumulator(dp(15f), dp(40f))
        var repeatCount = 0
        var pending: Runnable? = null
    }

    private val pointers = LinkedHashMap<Int, Pointer>()
    private val ignoredPointers = HashSet<Int>()
    private var selecting = false
    private val handler = Handler(Looper.getMainLooper())

    @SuppressLint("ClickableViewAccessibility")
    override fun onTouchEvent(event: MotionEvent): Boolean {
        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN, MotionEvent.ACTION_POINTER_DOWN -> {
                val i = event.actionIndex
                pointerDown(event.getPointerId(i), event.getX(i), event.getY(i))
            }
            MotionEvent.ACTION_MOVE -> for (i in 0 until event.pointerCount) {
                pointerMove(event.getPointerId(i), event.getX(i), event.getY(i))
            }
            MotionEvent.ACTION_UP, MotionEvent.ACTION_POINTER_UP -> pointerUp(event.getPointerId(event.actionIndex))
            MotionEvent.ACTION_CANCEL -> cancelAll()
        }
        invalidate()
        return true
    }

    private fun pointerDown(id: Int, x: Float, y: Float) {
        // A second finger while using the space-bar trackpad toggles text selection (like iPhone).
        if (pointers.values.any { it.mode == Mode.CURSOR }) {
            selecting = !selecting
            ignoredPointers += id
            haptic(HapticFeedbackConstants.CLOCK_TICK)
            return
        }
        // Fast two-thumb typing: commit any key still held before handling the new one.
        for (p in pointers.values) {
            if (p.mode == Mode.PRESS && p.key.key.style == KeyStyle.CHARACTER) {
                fire(p)
                p.mode = Mode.DONE
            }
        }
        val pk = geometry().keyAt(x, y) ?: return
        val p = Pointer(id, pk, x, y)
        pointers[id] = p
        haptic(HapticFeedbackConstants.KEYBOARD_TAP)
        when (val action = pk.key.action) {
            KeyAction.Shift -> listener.onShiftDown()
            KeyAction.Delete -> schedule(p, REPEAT_DELAY_MS) { deleteRepeat(p) }
            KeyAction.Space -> schedule(p, SPACE_LONG_PRESS_MS) {
                if (p.mode == Mode.PRESS) {
                    enterCursorMode(p)
                    haptic(HapticFeedbackConstants.LONG_PRESS)
                }
            }
            KeyAction.Fn -> schedule(p, LONG_PRESS_MS) {
                if (p.mode == Mode.PRESS && p.corner == null) {
                    p.mode = Mode.DONE
                    haptic(HapticFeedbackConstants.LONG_PRESS)
                    listener.onShowKeyboardPicker()
                }
            }
            is KeyAction.Special -> if (action.key.repeats) {
                schedule(p, REPEAT_DELAY_MS) { keyRepeat(p, action) }
            }
            else -> Unit
        }
    }

    private fun pointerMove(id: Int, x: Float, y: Float) {
        val p = pointers[id] ?: return
        when (p.mode) {
            Mode.PRESS -> when (p.key.key.action) {
                KeyAction.Space -> if (abs(x - p.startX) > dp(12f)) {
                    cancelPending(p)
                    enterCursorMode(p)
                }
                KeyAction.Delete -> if (p.deleteTracker.update(x - p.startX, y - p.startY)) {
                    // Swipe chosen: delete once now, then repeat like a held key while the finger stays out.
                    cancelPending(p)
                    p.mode = Mode.DELETE_GESTURE
                    p.deleteTracker.op?.let { performDelete(it, 1) }
                    if (p.deleteTracker.repeats) schedule(p, REPEAT_DELAY_MS) { swipeRepeat(p) }
                }
                else -> if (p.key.key.corners.isNotEmpty()) {
                    val corner = cornerOf(x - p.startX, y - p.startY, cornerSwipeThreshold)
                    p.corner = corner?.takeIf { it in p.key.key.corners }
                }
            }
            Mode.CURSOR -> {
                val steps = p.trackpad.move(x - p.lastX, y - p.lastY)
                if (!steps.isZero) listener.onCursorMove(steps.x, steps.y, selecting)
            }
            Mode.DELETE_GESTURE -> p.deleteTracker.update(x - p.startX, y - p.startY)
            else -> Unit
        }
        p.lastX = x
        p.lastY = y
    }

    private fun pointerUp(id: Int) {
        if (ignoredPointers.remove(id)) return
        val p = pointers.remove(id) ?: return
        cancelPending(p)
        when (p.mode) {
            Mode.PRESS -> fire(p)
            Mode.CURSOR -> if (pointers.values.none { it.mode == Mode.CURSOR }) selecting = false
            else -> Unit
        }
        if (p.key.key.action == KeyAction.Shift) listener.onShiftUp()
    }

    /** Releases every touch without firing keys, e.g. when the keyboard is hidden. */
    fun cancelAll() {
        for (p in pointers.values) {
            cancelPending(p)
            if (p.key.key.action == KeyAction.Shift) listener.onShiftUp()
        }
        pointers.clear()
        ignoredPointers.clear()
        selecting = false
        invalidate()
    }

    private fun fire(p: Pointer) {
        val action = p.corner?.let { p.key.key.corners[it] } ?: p.key.key.action
        when (action) {
            KeyAction.Shift -> Unit
            KeyAction.Delete -> listener.onDelete(DeleteOp.CHAR_BACK, 1)
            else -> listener.onAction(action)
        }
    }

    private fun performDelete(op: DeleteOp, count: Int) {
        if (count <= 0) return
        listener.onDelete(op, count)
        haptic(HapticFeedbackConstants.CLOCK_TICK)
    }

    private fun enterCursorMode(p: Pointer) {
        p.mode = Mode.CURSOR
        p.trackpad.reset()
        invalidate()
    }

    /** Holding delete repeats characters, then switches to whole words (like iPhone). */
    private fun deleteRepeat(p: Pointer) {
        if (p.mode != Mode.PRESS && p.mode != Mode.DELETE_REPEAT) return
        p.mode = Mode.DELETE_REPEAT
        p.repeatCount++
        val words = p.repeatCount > WORD_REPEAT_AFTER
        listener.onDelete(if (words) DeleteOp.WORD_BACK else DeleteOp.CHAR_BACK, 1)
        if (words) haptic(HapticFeedbackConstants.CLOCK_TICK)
        schedule(p, if (words) WORD_REPEAT_INTERVAL_MS else CHAR_REPEAT_INTERVAL_MS) { deleteRepeat(p) }
    }

    /** A held delete swipe repeats with the same timing as holding the delete key. */
    private fun swipeRepeat(p: Pointer) {
        val op = p.deleteTracker.op ?: return
        if (p.deleteTracker.engaged) performDelete(op, 1)
        val interval = if (op == DeleteOp.CHAR_FORWARD) CHAR_REPEAT_INTERVAL_MS else WORD_REPEAT_INTERVAL_MS
        schedule(p, interval) { swipeRepeat(p) }
    }

    /** Arrow keys and forward delete on the Fn page repeat while held. */
    private fun keyRepeat(p: Pointer, action: KeyAction) {
        if (p.mode != Mode.PRESS && p.mode != Mode.KEY_REPEAT) return
        p.mode = Mode.KEY_REPEAT
        listener.onAction(action)
        schedule(p, CHAR_REPEAT_INTERVAL_MS) { keyRepeat(p, action) }
    }

    private fun schedule(p: Pointer, delayMs: Long, block: () -> Unit) {
        cancelPending(p)
        val r = Runnable {
            p.pending = null
            if (pointers[p.id] === p) {
                block()
                invalidate()
            }
        }
        p.pending = r
        handler.postDelayed(r, delayMs)
    }

    private fun cancelPending(p: Pointer) {
        p.pending?.let { handler.removeCallbacks(it) }
        p.pending = null
    }

    private fun haptic(constant: Int) {
        performHapticFeedback(constant)
    }

    private companion object {
        const val ROWS = 4
        const val REPEAT_DELAY_MS = 400L
        const val CHAR_REPEAT_INTERVAL_MS = 55L
        const val WORD_REPEAT_INTERVAL_MS = 230L
        const val WORD_REPEAT_AFTER = 18
        const val SPACE_LONG_PRESS_MS = 350L
        const val LONG_PRESS_MS = 450L
    }
}
