package com.isycat.dotaaddon

import com.isycat.dota.types.lua.CScriptPrecacheContext
import com.isycat.dota.types.lua.precacheItemByNameSync
import com.isycat.dota.types.lua.precacheResource
import com.isycat.dota.types.lua.precacheUnitByNameSync
import com.isycat.dotaaddon.shared.GameConfig
import com.isycat.dotaaddon.units.ELITE_ROSTER
import com.isycat.dotaaddon.units.EliteKind

/** Engine precache hook — loads assets spawned at runtime so the first wave doesn't hitch. */
fun Precache(context: CScriptPrecacheContext) {
    precacheUnitByNameSync("npc_dota_creep_badguys_melee", context, null)
    precacheUnitByNameSync("npc_dota_creep_badguys_ranged", context, null)
    // Each elite is a custom ancient creep (KV generated in EliteUnits) — precache the whole roster.
    // Explicit `kind: EliteKind` param so `kind.unitName` doesn't mis-resolve to `GetUnitName()` (implicit
    // `it` element-type inference is a tracked ktox gap).
    ELITE_ROSTER.forEach { kind: EliteKind -> precacheUnitByNameSync(kind.unitName, context, null) }
    // The Ancient is a re-skinned creep — precache the unit AND the building model it's given at runtime.
    // precacheResource("model", …) is the form that works in this context (PrecacheModel no-ops here).
    precacheUnitByNameSync("npc_dota_creep_goodguys_melee", context, null)
    precacheResource("model", GameConfig.ANCIENT_MODEL, context)
    // Placed wards use a model a custom game doesn't auto-load; without precaching, planting one stalls
    // the server. Precache the model + both ward items so the sentry variant is covered too.
    precacheResource("model", "models/items/wards/f2p_ward/f2p_ward.vmdl", context)
    precacheItemByNameSync("item_ward_observer", context)
    precacheItemByNameSync("item_ward_sentry", context)
    // Whirling Death's whirl particle (the granted WhirlingDeath ability).
    precacheResource("particle", GameConfig.WHIRLING_DEATH_PARTICLE, context)
    WaveDefenseController.precacheBossHeroes(context)
}

/**
 * Engine activation hook. Earliest point GetGameModeEntity() is valid, so the think loop starts here
 * rather than from main() (which runs at script load).
 */
fun Activate() {
    WaveDefenseController.beginThink()
}
