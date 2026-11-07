package app.blend.keyboard.model

import kotlin.math.abs
import kotlin.math.min

/** Plain rectangle so geometry stays testable without the Android framework. */
data class Box(val left: Float, val top: Float, val right: Float, val bottom: Float) {
    val width get() = right - left
    val height get() = bottom - top
    val centerX get() = (left + right) / 2f
    val centerY get() = (top + bottom) / 2f

    fun contains(x: Float, y: Float) = x in left..right && y in top..bottom
}

/** A key placed on screen. [cell] is its touch area, which includes the gaps around the visible key. */
data class PlacedKey(val key: Key, val cell: Box, val row: Int)

/**
 * Lays out a [KeyboardLayout] into pixel cells and finds the key under a touch.
 * Touches in gaps or past the edges go to the nearest key in the touched row.
 */
class KeyboardGeometry(
    val layout: KeyboardLayout,
    width: Float,
    private val top: Float,
    sidePadding: Float,
    private val rowHeight: Float
) {
    val keys: List<PlacedKey>
    val height: Float = layout.rows.size * rowHeight

    init {
        val unit = (width - 2 * sidePadding) / UNITS_PER_ROW
        val placed = mutableListOf<PlacedKey>()
        layout.rows.forEachIndexed { r, row ->
            var x = sidePadding + row.inset * unit
            val rowTop = top + r * rowHeight
            for (key in row.keys) {
                val w = key.width * unit
                if (key.style != KeyStyle.GAP) {
                    placed += PlacedKey(key, Box(x, rowTop, x + w, rowTop + rowHeight), r)
                }
                x += w
            }
        }
        keys = placed
    }

    fun keyAt(x: Float, y: Float): PlacedKey? {
        if (layout.rows.isEmpty()) return null
        val row = ((y - top) / rowHeight).toInt().coerceIn(0, layout.rows.size - 1)
        return keys.filter { it.row == row }.minByOrNull { horizontalDistance(it.cell, x) }
    }

    private fun horizontalDistance(box: Box, x: Float): Float = if (x in box.left..box.right) {
        0f
    } else {
        min(
            abs(x - box.left),
            abs(x - box.right)
        )
    }
}
