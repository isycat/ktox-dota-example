package com.isycat.dotaaddon

import com.isycat.dota.types.lua.CScriptPrecacheContext
import com.isycat.dota.types.lua.precacheUnitByNameSync
import com.isycat.dotaaddon.main

/**
 * Engine precache hook. Loads the assets the game spawns at runtime so the
 * first wave doesn't hitch. The `main` import makes the transpiler emit
 * `ktox_require("Main")`, which runs [main] (Main.kt) to boot the game loop.
 */
fun Precache(context: CScriptPrecacheContext) {
    precacheUnitByNameSync("npc_dota_creep_badguys_melee", context, null)
    precacheUnitByNameSync("npc_dota_creep_badguys_ranged", context, null)
}

/**
 * Engine activation hook, called once the game mode is live. This is the
 * earliest point `GameRules:GetGameModeEntity()` is valid, so the wave-spawning
 * think loop is started here rather than from main() (which runs at script load).
 */
fun Activate() {
    WaveDefense.beginThink()
}
