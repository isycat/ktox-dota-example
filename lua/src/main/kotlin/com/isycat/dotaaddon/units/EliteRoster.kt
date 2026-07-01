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
    /** Ancient neutral-creep model this elite is skinned with (also precached from the roster). */
    val model: String,
    /** Stock passive ability granted on spawn (its signature aura/effect). */
    val signatureAbility: String,
)

/** The elite kinds, cycled per elite spawned. Their KV units are generated in [EliteUnits]. */
val ELITE_ROSTER =
    listOf(
        // Model FILE names don't always match the dir (e.g. n_creep_golem_a/neutral_creep_golem_a.vmdl).
        EliteKind(
            "Swift Marauder",
            "npc_wd_elite_marauder",
            "models/creeps/neutral_creeps/n_creep_golem_a/neutral_creep_golem_a.vmdl",
            "kobold_taskmaster_speed_aura",
        ),
        EliteKind(
            "Dread Ravager",
            "npc_wd_elite_ravager",
            "models/creeps/neutral_creeps/n_creep_black_dragon/n_creep_black_dragon.vmdl",
            "satyr_hellcaller_unholy_aura",
        ),
        EliteKind(
            "Storm Caller",
            "npc_wd_elite_stormcaller",
            "models/creeps/neutral_creeps/n_creep_thunder_lizard/n_creep_thunder_lizard_big.vmdl",
            "ghost_frost_attack",
        ),
        EliteKind(
            "Bonebreaker",
            "npc_wd_elite_bonebreaker",
            "models/creeps/neutral_creeps/n_creep_golem_b/neutral_creep_golem_b.vmdl",
            "vhoul_assassin_envenomed_weapon",
        ),
    )

/** The [index]-th elite kind, cycling the roster. Indexing lives here (same file as [ELITE_ROSTER]). */
fun eliteKindFor(index: Int): EliteKind = ELITE_ROSTER[index % ELITE_ROSTER.size]
