package app.tecla.keyboard.logic

/**
 * Height of the empty strip under the keys that keeps them above the navigation bar and the
 * keyboard's switch/hide buttons. [visibleBottom] is the bar's inset right now, which drops to 0
 * while the bar is briefly hidden (fullscreen apps, transitions); [barBottom] is its size even when
 * hidden. Using the larger one stops the keys from sinking over the bar.
 */
fun navBarSpace(visibleBottom: Int, barBottom: Int): Int = maxOf(visibleBottom, barBottom)
