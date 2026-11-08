package app.tecla.keyboard.ui

/** iPhone-like keyboard colors (ARGB). */
data class KeyboardTheme(
    val background: Int,
    val key: Int,
    val functionKey: Int,
    val keyText: Int,
    val shadow: Int,
    val accent: Int,
    val accentText: Int,
    val hint: Int
) {
    companion object {
        /** Pure black background with dark keys; the default theme. */
        val BLACKOUT = KeyboardTheme(
            background = 0xFF000000.toInt(),
            key = 0xFF1C1C1E.toInt(),
            functionKey = 0xFF0E0E0F.toInt(),
            keyText = 0xFFFFFFFF.toInt(),
            shadow = 0xFF000000.toInt(),
            accent = 0xFF0A84FF.toInt(),
            accentText = 0xFFFFFFFF.toInt(),
            hint = 0xFF8E8E93.toInt()
        )

        val LIGHT = KeyboardTheme(
            background = 0xFFD1D4DA.toInt(),
            key = 0xFFFFFFFF.toInt(),
            functionKey = 0xFFABB0BC.toInt(),
            keyText = 0xFF000000.toInt(),
            shadow = 0xFF898A8D.toInt(),
            accent = 0xFF007AFF.toInt(),
            accentText = 0xFFFFFFFF.toInt(),
            hint = 0x66000000
        )

        val DARK = KeyboardTheme(
            background = 0xFF232325.toInt(),
            key = 0xFF636366.toInt(),
            functionKey = 0xFF3A3A3C.toInt(),
            keyText = 0xFFFFFFFF.toInt(),
            shadow = 0xFF000000.toInt(),
            accent = 0xFF0A84FF.toInt(),
            accentText = 0xFFFFFFFF.toInt(),
            hint = 0x80FFFFFF.toInt()
        )
    }
}
