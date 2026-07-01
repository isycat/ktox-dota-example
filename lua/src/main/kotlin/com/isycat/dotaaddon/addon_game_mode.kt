package com.isycat.dotaaddon

import com.isycat.dota.types.lua.CScriptPrecacheContext
import com.isycat.dota.types.lua.precacheItemByNameSync
import com.isycat.dota.types.lua.precacheResource
import com.isycat.dota.types.lua.precacheUnitByNameSync
import com.isycat.dotaaddon.shared.AddonInfo
import com.isycat.dotaaddon.shared.GameConfig
import com.isycat.dotaaddon.units.ELITE_ROSTER

/** Engine precache hook — loads assets spawned at runtime so the first wave doesn't hitch. */
fun Precache(context: CScriptPrecacheContext) {
    precacheUnitByNameSync("npc_dota_creep_badguys_melee", context, null)
    precacheUnitByNameSync("npc_dota_creep_badguys_ranged", context, null)
    // Each elite is a custom ancient creep (KV generated in EliteUnits). Precache the unit AND its model —
    // precacheUnitByNameSync alone does NOT pull a custom unit's model, so an un-precached ancient renders
    // invisible/late (same reason the Ancient below precaches its model explicitly).
    ELITE_ROSTER.forEach { kind ->
        precacheUnitByNameSync(kind.unitName, context, null)
        precacheResource("model", kind.model, context)
    }
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
    // Boss heroes pull in many assets — precache the whole roster up front so the first boss wave doesn't hitch.
    GameConfig.BOSS_ROSTER.forEach { precacheUnitByNameSync(it.unitName, context, null) }
}

/**
 * Engine activation hook — the addon's real entry point (Dota loads `addon_game_mode.lua` and calls this).
 * Registers every custom-event listener AND starts the think loop here: `Activate` is the earliest point
 * GetGameModeEntity() is valid, and it is the ONLY entry Dota invokes. (There is no separate loaded
 * `main()` — with `generateRootAddonLuaFile = false` the entry-point bootstrap that would `require("Main")`
 * is off, so anything the addon needs at startup must be wired from here.)
 */
fun Activate() {
    println(AddonInfo.getWelcomeMessage())
    WaveDefenseController.start()
    WaveDefenseController.beginThink()
}
