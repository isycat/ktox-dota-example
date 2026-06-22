package com.isycat.dotaaddon

import com.isycat.dota.types.lua.msg
import com.isycat.dotaaddon.shared.AddonInfo

/**
 * Lua entry point. `addon_game_mode.lua` runs `ktox_require("Main")`, which
 * loads this module and invokes [main]. From here we hand off to the
 * [WaveDefense] game loop.
 */
fun main() {
    msg(AddonInfo.getWelcomeMessage())
    WaveDefense.start()
}
