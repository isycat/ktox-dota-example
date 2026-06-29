package com.isycat.dotaaddon

import com.isycat.dota.types.lua.CScriptPrecacheContext
import com.isycat.dota.types.lua.precacheItemByNameSync
import com.isycat.dota.types.lua.precacheResource
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
    // the model it is given at runtime. PrecacheResource("model", …) (as real mods like PetriReborn do)
    // is what actually works in the Precache context; PrecacheModel no-ops here and left it the pink
    // "error" placeholder.
    precacheUnitByNameSync("npc_dota_creep_goodguys_melee", context, null)
    precacheResource("model", GameConfig.ANCIENT_MODEL, context)
    // The arena-wide shop (universal shop mode) lets players buy Observer/Sentry wards. A placed ward is
    // a unit using a default ward model that a CUSTOM game does not auto-load — so without precaching it
    // the engine asserts ("nonresident asset models/items/wards/f2p_ward/f2p_ward.vmdl") and STALLS the
    // server the instant a ward is planted. Precache the exact model the way the Ancient model is done
    // above (the form that works in this context), and the ward items so the sentry's variant is covered
    // too without hard-coding its model path.
    precacheResource("model", "models/items/wards/f2p_ward/f2p_ward.vmdl", context)
    precacheItemByNameSync("item_ward_observer", context)
    precacheItemByNameSync("item_ward_sentry", context)
    // Whirling Death's whirl particle (the granted WhirlingDeath ability).
    precacheResource("particle", GameConfig.WHIRLING_DEATH_PARTICLE, context)
    // Boss waves spawn real heroes that cast their spells — precache the whole roster up front.
    WaveDefenseController.precacheBossHeroes(context)
}

/**
 * Engine activation hook, called once the game mode is live. This is the
 * earliest point `GameRules:GetGameModeEntity()` is valid, so the wave-spawning
 * think loop is started here rather than from main() (which runs at script load).
 */
fun Activate() {
    WaveDefenseController.beginThink()
}
