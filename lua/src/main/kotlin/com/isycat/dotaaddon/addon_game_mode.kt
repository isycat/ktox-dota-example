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
