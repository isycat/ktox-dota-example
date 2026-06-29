package com.isycat.dotaaddon

import com.isycat.dota.types.lua.LuaModifierType
import com.isycat.dota.types.lua.linkLuaModifier
import com.isycat.dotaaddon.shared.AddonInfo

/**
 * Lua entry point. `addon_game_mode.lua` runs `ktox_require("Main")`, which
 * loads this module and invokes [main]. From here we hand off to the
 * [WaveDefenseController] game loop.
 */
fun main() {
    // Register custom lua modifiers explicitly, the standard Dota way: LinkLuaModifier(name, path, type).
    // ktox's @Dota2Class self-registration only fires if the modifier FILE is loaded, but a modifier
    // referenced only by its name string (AddNewModifier("UnselectableModifier", …)) is never required, so
    // it never registered and AddNewModifier silently no-op'd (the Ancient stayed selectable). Dota loads
    // modifier/UnselectableModifier.lua on demand from this path the first time the modifier is applied.
    linkLuaModifier("UnselectableModifier", "modifier/UnselectableModifier", LuaModifierType.MOTION_NONE)
    println(AddonInfo.getWelcomeMessage())
    WaveDefenseController.start()
}
