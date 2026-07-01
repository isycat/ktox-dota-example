package com.isycat.dotaaddon.units

/**
 * One kind of elite: the HUD display name and the custom unit it spawns. This is the RUNTIME roster
 * (transpiled to Lua) that [com.isycat.dotaaddon.WaveDefenseController] cycles through to spawn elites and
 * announce them. The matching KeyValues for each [unitName] are generated programmatically from this same
 * list in [EliteUnits] (a `@KvSource` file), so adding a kind here + a row there is all it takes.
 */
data class EliteKind(
    val displayName: String,
    val unitName: String,
)

/** The elite kinds, cycled per elite spawned. Their KV units are generated in [EliteUnits]. */
val ELITE_ROSTER =
    listOf(
        EliteKind("Swift Marauder", "npc_wd_elite_marauder"),
        EliteKind("Dread Ravager", "npc_wd_elite_ravager"),
        EliteKind("Storm Caller", "npc_wd_elite_stormcaller"),
        EliteKind("Bonebreaker", "npc_wd_elite_bonebreaker"),
    )
