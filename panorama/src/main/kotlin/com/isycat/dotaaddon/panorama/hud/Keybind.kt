package com.isycat.dotaaddon.panorama.hud

/**
 * Compacts the engine's verbose key names into a HUD-sized badge: `"MOUSE 5"` → `"M5"`,
 * `"Numpad 5"` → `"Num5"`. Prefix-matched case-insensitively so any engine spelling variant
 * ("Mouse5", "MOUSE 5", "numpad 5") compacts the same way. Shared by the ability bar and the
 * inventory bar (like [CooldownDisplay]).
 */
object Keybind {
    private const val MOUSE_PREFIX = "MOUSE"
    private const val NUMPAD_PREFIX = "NUMPAD"

    fun short(raw: String): String {
        val key = raw.trim()
        val upper = key.uppercase()
        if (upper.startsWith(MOUSE_PREFIX)) {
            return "M${key.substring(MOUSE_PREFIX.length).trim()}"
        }
        if (upper.startsWith(NUMPAD_PREFIX)) {
            return "Num${key.substring(NUMPAD_PREFIX.length).trim()}"
        }
        return key
    }
}
