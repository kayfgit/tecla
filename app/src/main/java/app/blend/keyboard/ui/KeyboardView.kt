package app.blend.keyboard.ui

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import android.graphics.Typeface
import android.os.Handler
import android.os.Looper
import android.util.TypedValue
import android.view.HapticFeedbackConstants
import android.view.MotionEvent
import android.view.View
import app.blend.keyboard.logic.DeleteGestureTracker
import app.blend.keyboard.logic.DeleteOp
import app.blend.keyboard.logic.ShiftMode
import app.blend.keyboard.logic.TrackpadAccumulator
import app.blend.keyboard.model.Box
import app.blend.keyboard.model.KeyAction
import app.blend.keyboard.model.KeyStyle
import app.blend.keyboard.model.KeyboardGeometry
import app.blend.keyboard.model.KeyboardLayout
import app.blend.keyboard.model.KeyboardPage
import app.blend.keyboard.model.Layouts
import app.blend.keyboard.model.PlacedKey
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
        fun onKeyText(text: String)
        fun onShiftDown()
        fun onShiftUp()
        fun onDelete(op: DeleteOp, count: Int)
        fun onSpace()
        fun onEnter()
        fun onSwitchKeyboard()
        fun onShowKeyboardPicker()
        fun onPageChange(page: KeyboardPage)
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

    /** Y offset where the keyboard background starts; everything above it is see-through. */
    val keysTop: Int get() = previewSpace.toInt()

    private var theme = KeyboardTheme.LIGHT
    private var layout: KeyboardLayout = Layouts.letters
    private var geometry: KeyboardGeometry? = null
    private var shiftMode = ShiftMode.OFF
    private var upperCase = false
    private var enterLabel = "return"
    private var enterAccent = false

    private val fillPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val strokePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = dp(1.5f)
        strokeJoin = Paint.Join.ROUND
        strokeCap = Paint.Cap.ROUND
    }
    private val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { textAlign = Paint.Align.CENTER }
    private val rect = RectF()
    private val path = Path()

    fun setTheme(newTheme: KeyboardTheme) {
        if (theme != newTheme) {
            theme = newTheme
            invalidate()
        }
    }

    fun setState(layout: KeyboardLayout, shiftMode: ShiftMode, upperCase: Boolean, enterLabel: String, enterAccent: Boolean) {
        if (layout !== this.layout) {
            this.layout = layout
            geometry = null
        }
        this.shiftMode = shiftMode
        this.upperCase = upperCase
        this.enterLabel = enterLabel
        this.enterAccent = enterAccent
        invalidate()
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val width = MeasureSpec.getSize(widthMeasureSpec)
        val height = previewSpace + topPadding + layout.rows.size * rowHeight + bottomPadding
        setMeasuredDimension(width, height.toInt())
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        geometry = null
    }

    private fun geometry(): KeyboardGeometry = geometry ?: KeyboardGeometry(
        layout,
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
            if (p.mode == Mode.PRESS && p.key.key.style == KeyStyle.CHARACTER) drawBalloon(canvas, p.key)
        }
    }

    private fun keyRect(cell: Box): RectF {
        rect.set(cell.left + keyGapH / 2, cell.top + keyGapV / 2, cell.right - keyGapH / 2, cell.bottom - keyGapV / 2)
        return rect
    }

    private fun isPressed(pk: PlacedKey) = pointers.values.any { it.key === pk && it.mode != Mode.DONE }

    private fun drawKey(canvas: Canvas, pk: PlacedKey, cursorMode: Boolean) {
        val key = pk.key
        val pressed = isPressed(pk)
        val shiftActive = key.action == KeyAction.Shift && shiftMode != ShiftMode.OFF
        val accent = key.style == KeyStyle.ENTER && enterAccent
        val bg = when {
            accent -> if (pressed) theme.functionKey else theme.accent
            shiftActive -> theme.key
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
        when (key.action) {
            KeyAction.Shift -> drawShift(canvas, cx, cy, fg)
            KeyAction.Delete -> drawDelete(canvas, r, fg, pressed)
            KeyAction.SwitchKeyboard -> drawGlobe(canvas, cx, cy, fg)
            KeyAction.Enter -> drawLabel(canvas, enterLabel, cx, cy, sp(16f), fg, false)
            is KeyAction.Text -> {
                val label = if (upperCase && layout.page == KeyboardPage.LETTERS) key.label.uppercase() else key.label
                drawLabel(canvas, label, cx, cy, sp(23f), fg, false)
            }
            else -> drawLabel(canvas, key.label, cx, cy, sp(16f), fg, false)
        }
    }

    private fun drawLabel(canvas: Canvas, text: String, cx: Float, cy: Float, size: Float, color: Int, bold: Boolean) {
        textPaint.textSize = size
        textPaint.color = color
        textPaint.typeface = if (bold) Typeface.DEFAULT_BOLD else Typeface.DEFAULT
        val fm = textPaint.fontMetrics
        canvas.drawText(text, cx, cy - (fm.ascent + fm.descent) / 2f, textPaint)
    }

    private fun drawShift(canvas: Canvas, cx: Float, cy: Float, color: Int) {
        val s = dp(18f)
        val y = if (shiftMode == ShiftMode.LOCKED) cy - dp(2f) else cy
        path.reset()
        path.moveTo(cx, y - s / 2)
        path.lineTo(cx + s / 2, y)
        path.lineTo(cx + s / 4.5f, y)
        path.lineTo(cx + s / 4.5f, y + s / 2)
        path.lineTo(cx - s / 4.5f, y + s / 2)
        path.lineTo(cx - s / 4.5f, y)
        path.lineTo(cx - s / 2, y)
        path.close()
        if (shiftMode == ShiftMode.OFF) {
            strokePaint.color = color
            canvas.drawPath(path, strokePaint)
        } else {
            fillPaint.color = color
            canvas.drawPath(path, fillPaint)
        }
        if (shiftMode == ShiftMode.LOCKED) {
            fillPaint.color = color
            canvas.drawRect(cx - s / 4.5f, y + s / 2 + dp(2.5f), cx + s / 4.5f, y + s / 2 + dp(4f), fillPaint)
        }
    }

    private fun drawDelete(canvas: Canvas, r: RectF, color: Int, pressed: Boolean) {
        val w = min(dp(24f), r.width() * 0.55f)
        val h = w * 0.72f
        val cx = r.centerX()
        val cy = r.centerY()
        val left = cx - w / 2
        val right = cx + w / 2
        path.reset()
        path.moveTo(left, cy)
        path.lineTo(left + h / 2, cy - h / 2)
        path.lineTo(right, cy - h / 2)
        path.lineTo(right, cy + h / 2)
        path.lineTo(left + h / 2, cy + h / 2)
        path.close()
        strokePaint.color = color
        if (pressed) {
            fillPaint.color = color
            canvas.drawPath(path, fillPaint)
            strokePaint.color = theme.key
        } else {
            canvas.drawPath(path, strokePaint)
        }
        val xc = left + h / 2 + (right - left - h / 2) / 2
        val d = h * 0.2f
        canvas.drawLine(xc - d, cy - d, xc + d, cy + d, strokePaint)
        canvas.drawLine(xc - d, cy + d, xc + d, cy - d, strokePaint)
        // Faint hints for the swipe directions: words back / words forward.
        drawLabel(canvas, "«", r.left + dp(7f), cy, sp(11f), theme.hint, false)
        drawLabel(canvas, "»", r.right - dp(7f), cy, sp(11f), theme.hint, false)
    }

    private fun drawGlobe(canvas: Canvas, cx: Float, cy: Float, color: Int) {
        val rad = dp(9f)
        strokePaint.color = color
        val oldWidth = strokePaint.strokeWidth
        strokePaint.strokeWidth = dp(1.3f)
        canvas.drawCircle(cx, cy, rad, strokePaint)
        rect.set(cx - rad * 0.45f, cy - rad, cx + rad * 0.45f, cy + rad)
        canvas.drawOval(rect, strokePaint)
        canvas.drawLine(cx - rad, cy, cx + rad, cy, strokePaint)
        val chord = rad * 0.87f
        canvas.drawLine(cx - chord, cy - rad * 0.5f, cx + chord, cy - rad * 0.5f, strokePaint)
        canvas.drawLine(cx - chord, cy + rad * 0.5f, cx + chord, cy + rad * 0.5f, strokePaint)
        strokePaint.strokeWidth = oldWidth
    }

    /** iPhone-style magnified key that pops up above the pressed key. */
    private fun drawBalloon(canvas: Canvas, pk: PlacedKey) {
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

        fillPaint.color = theme.shadow
        canvas.save()
        canvas.translate(0f, dp(1f))
        canvas.drawPath(path, fillPaint)
        canvas.restore()
        fillPaint.color = theme.key
        canvas.drawPath(path, fillPaint)

        val label = pk.key.label.let { if (upperCase && layout.page == KeyboardPage.LETTERS) it.uppercase() else it }
        drawLabel(canvas, label, (headLeft + headRight) / 2, (headTop + headBottom) / 2, sp(34f), theme.keyText, false)
    }

    // ---------------------------------------------------------------- touch

    private enum class Mode { PRESS, CURSOR, DELETE_GESTURE, DELETE_REPEAT, DONE }

    private inner class Pointer(val id: Int, var key: PlacedKey, val startX: Float, val startY: Float) {
        var lastX = startX
        var lastY = startY
        var mode = Mode.PRESS
        val deleteTracker = DeleteGestureTracker(dp(18f), dp(32f))
        val trackpad = TrackpadAccumulator(dp(9f), dp(34f))
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
            is KeyAction.Page -> {
                p.mode = Mode.DONE
                listener.onPageChange(action.target)
            }
            KeyAction.Delete -> schedule(p, DELETE_REPEAT_DELAY_MS) { deleteRepeat(p) }
            KeyAction.Space -> schedule(p, SPACE_LONG_PRESS_MS) {
                if (p.mode == Mode.PRESS) {
                    enterCursorMode(p)
                    haptic(HapticFeedbackConstants.LONG_PRESS)
                }
            }
            KeyAction.SwitchKeyboard -> schedule(p, GLOBE_LONG_PRESS_MS) {
                if (p.mode == Mode.PRESS) {
                    p.mode = Mode.DONE
                    listener.onShowKeyboardPicker()
                }
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
                KeyAction.Delete -> {
                    val n = p.deleteTracker.update(x - p.startX, y - p.startY)
                    val op = p.deleteTracker.op
                    if (op != null) {
                        cancelPending(p)
                        p.mode = Mode.DELETE_GESTURE
                        performDelete(op, n)
                    }
                }
                is KeyAction.Text -> {
                    val pk = geometry().keyAt(x, y)
                    if (pk != null && pk.key.style == KeyStyle.CHARACTER) p.key = pk
                }
                else -> Unit
            }
            Mode.CURSOR -> {
                val steps = p.trackpad.move(x - p.lastX, y - p.lastY)
                if (!steps.isZero) listener.onCursorMove(steps.x, steps.y, selecting)
            }
            Mode.DELETE_GESTURE -> {
                val op = p.deleteTracker.op ?: return
                performDelete(op, p.deleteTracker.update(x - p.startX, y - p.startY))
            }
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
        when (val action = p.key.key.action) {
            is KeyAction.Text -> listener.onKeyText(action.text)
            KeyAction.Delete -> listener.onDelete(DeleteOp.CHAR_BACK, 1)
            KeyAction.Space -> listener.onSpace()
            KeyAction.Enter -> listener.onEnter()
            KeyAction.SwitchKeyboard -> listener.onSwitchKeyboard()
            else -> Unit
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
        invalidate()
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
        const val DELETE_REPEAT_DELAY_MS = 400L
        const val CHAR_REPEAT_INTERVAL_MS = 55L
        const val WORD_REPEAT_INTERVAL_MS = 230L
        const val WORD_REPEAT_AFTER = 18
        const val SPACE_LONG_PRESS_MS = 350L
        const val GLOBE_LONG_PRESS_MS = 450L
    }
}
