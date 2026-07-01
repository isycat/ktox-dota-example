package com.isycat.dotaaddon

import com.isycat.dotaaddon.shared.AddonInfo

/** Lua entry point (loaded via `ktox_require("Main")`); hands off to the [WaveDefenseController] game loop. */
fun main() {
    println(AddonInfo.getWelcomeMessage())
    WaveDefenseController.start()
}
