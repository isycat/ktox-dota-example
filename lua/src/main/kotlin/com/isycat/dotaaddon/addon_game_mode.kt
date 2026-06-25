package com.isycat.dotaaddon

import com.isycat.dota.types.lua.CScriptPrecacheContext
import com.isycat.dota.types.lua.precacheUnitByNameSync
import com.isycat.dotaaddon.main
import com.isycat.dotaaddon.shared.GameConfig

/**
 * Engine precache hook. Loads the assets the game spawns at runtime so the
 * first wave doesn't hitch. The `main` import makes the transpiler emit
 * `ktox_require("Main")`, which runs [main] (Main.kt) to boot the game loop.
 */
fun Precache(context: CScriptPrecacheContext) {
    precacheUnitByNameSync("npc_dota_creep_badguys_melee", context, null)
    precacheUnitByNameSync("npc_dota_creep_badguys_ranged", context, null)
    // The Ancient is a goodguys creep re-skinned with a building model — precache both the unit and
    // the model it is given at runtime, otherwise the model renders as the pink "error" placeholder.
    precacheUnitByNameSync("npc_dota_creep_goodguys_melee", context, null)
    context.addResource(GameConfig.ANCIENT_MODEL)
}

/**
 * Engine activation hook, called once the game mode is live. This is the
 * earliest point `GameRules:GetGameModeEntity()` is valid, so the wave-spawning
 * think loop is started here rather than from main() (which runs at script load).
 */
fun Activate() {
    WaveDefense.beginThink()
}
