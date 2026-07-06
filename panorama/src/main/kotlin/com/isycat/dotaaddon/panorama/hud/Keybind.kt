package com.isycat.dotaaddon.panorama.hud

/**
 * Compacts the engine's verbose key names into a HUD-sized badge: `"MOUSE 5"` → `"M5"`,
 * `"Numpad 5"` → `"Num5"`, and any remaining internal spaces are dropped (`"PAGE UP"` → `"PAGEUP"`).
 * Shared by the ability bar and the inventory bar (like [CooldownDisplay]).
 */
object Keybind {
    fun short(raw: String): String {
        if (raw == "") return ""
        return raw
            .replace("MOUSE ", "M")
            .replace("Mouse ", "M")
            .replace("Numpad ", "Num")
            .replace("NUMPAD ", "Num")
            .replace(" ", "")
    }
}
