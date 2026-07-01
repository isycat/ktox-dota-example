@file:KvSource

package com.isycat.dotaaddon.units

import com.isycat.dota.types.lua.DOTATeam
import com.isycat.dota.types.lua.UnitBaseClass
import com.isycat.dota.types.lua.UnitHullSize
import com.isycat.ktox.dota.KvFlag
import com.isycat.ktox.dota.KvSource
import com.isycat.ktox.dota.UnitKvSpec

/*
 * PROGRAMMATIC KeyValues: the elite units, generated in a Kotlin loop rather than one static @UnitKv per
 * class. This whole file is `@file:KvSource` — ktox reads its top-level `List<KvSpec>` vals at build time
 * and merges them into npc_units_custom.txt; nothing here is transpiled to Lua. The runtime roster
 * ([ELITE_ROSTER]) lives in its own file so it IS transpiled and available to the game loop.
 *
 * Each elite is a different kind of ANCIENT creep (Midas-immune by the engine's native rule), re-skinned
 * with a hero model and granted one signature passive ability that applies on spawn.
 */

/** Per-unit KV data (model + signature passive ability) keyed by unit name, joined to [ELITE_ROSTER]. */
private val ELITE_KV =
    mapOf(
        "npc_wd_elite_marauder" to ("models/heroes/lycan/lycan.vmdl" to "kobold_taskmaster_speed_aura"),
        "npc_wd_elite_ravager" to ("models/heroes/doom/doom.vmdl" to "satyr_hellcaller_unholy_aura"),
        "npc_wd_elite_stormcaller" to ("models/heroes/razor/razor.vmdl" to "ghost_frost_attack"),
        "npc_wd_elite_bonebreaker" to
            ("models/heroes/clinkz/clinkz.vmdl" to "vhoul_assassin_envenomed_weapon"),
    )

/** The generated elite unit KeyValues — one [UnitKvSpec] per [ELITE_ROSTER] entry (procedural KV). */
val eliteUnits: List<UnitKvSpec> =
    ELITE_ROSTER.map { kind ->
        val (model, ability) = ELITE_KV.getValue(kind.unitName)
        UnitKvSpec(
            name = kind.unitName,
            baseClass = listOf(UnitBaseClass.CREATURE),
            model = model,
            modelScale = 1.15,
            isAncient = KvFlag.YES,
            team = listOf(DOTATeam.BADGUYS),
            boundsHullName = listOf(UnitHullSize.HERO),
            health = 400,
            healthRegen = 0.0,
            armor = 2.0,
            damageMin = 28,
            damageMax = 34,
            attackRate = 1.4,
            attackRange = 100,
            attackAnimationPoint = 0.4,
            moveSpeed = 280,
            abilities = listOf(ability),
        )
    }
