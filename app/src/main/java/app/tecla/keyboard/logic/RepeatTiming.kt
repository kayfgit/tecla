package app.tecla.keyboard.logic

/**
 * Hold-to-repeat timing. Delete follows the Samsung Keyboard (measured on a Galaxy A36): it starts
 * after 400ms, deletes about 30 letters a second, then speeds up to about 75 a second. Letters,
 * symbols and arrows repeat at the steady starting pace so they stay easy to stop.
 */
object RepeatTiming {
    const val START_DELAY_MS = 400L

    /** Letters, symbols, accent marks, arrows and forward delete. */
    const val KEY_INTERVAL_MS = 33L

    const val DELETE_INTERVAL_MS = 33L
    const val DELETE_FAST_INTERVAL_MS = 13L

    /** Number of repeats at the starting pace before delete speeds up (about 400ms worth). */
    const val DELETE_FAST_AFTER = 12

    /** Wait before the next delete, after [repeatsSoFar] repeats. */
    fun deleteInterval(repeatsSoFar: Int): Long = if (repeatsSoFar >= DELETE_FAST_AFTER) DELETE_FAST_INTERVAL_MS else DELETE_INTERVAL_MS
}
