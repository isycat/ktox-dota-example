@file:KvSource

package com.isycat.dotaaddon.units

import com.isycat.dota.types.lua.DOTATeam
import com.isycat.dota.types.lua.DOTAUnitAttackCapability
import com.isycat.dota.types.lua.DOTAUnitMoveCapability
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
 * Each elite is a different kind of ANCIENT creep (Midas-immune by the engine's native rule), skinned with
 * an ancient neutral-creep model and granted one signature passive ability that applies on spawn. The model
 * + ability live on [EliteKind] (single source of truth — also feeds precache), read here per roster entry.
 */

/** The generated elite unit KeyValues — one [UnitKvSpec] per [ELITE_ROSTER] entry (procedural KV). */
val eliteUnits: List<UnitKvSpec> =
    ELITE_ROSTER.map { kind ->
        UnitKvSpec(
            name = kind.unitName,
            baseClass = listOf(UnitBaseClass.CREATURE),
            model = kind.model,
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
            // Same trap as MovementCapabilities: a custom CREATURE has NO AttackCapabilities by
            // default — it never swings (and never plays an attack animation); only its aura
            // abilities deal damage. Grant melee explicitly.
            attackCapability = listOf(DOTAUnitAttackCapability.CAP_MELEE_ATTACK),
            moveSpeed = 280,
            // A custom CREATURE unit has no MovementCapabilities by default → it can't move. Grant ground
            // movement explicitly so the elite marches on the Ancient like the lane creeps.
            moveCapability = listOf(DOTAUnitMoveCapability.GROUND),
            // Without an acquisition range a custom CREATURE ignores everything and walks straight past the
            // hero to the Ancient. Give it the lane-creep aggro range so it engages what it passes.
            attackAcquisitionRange = 800,
            // ALSO required for aggro: a custom unit with no vision KV is BLIND - it acquires
            // nothing regardless of acquisition range and marches straight past the hero.
            visionDaytimeRange = 1400,
            visionNighttimeRange = 800,
            // A custom CREATURE has NO bounty by default (0 gold on death). Elites are tougher than the
            // stock creeps, so they pay out more — without this, killing them gave nothing.
            bountyGoldMin = 45,
            bountyGoldMax = 60,
            bountyXP = 40,
            abilities = listOf(kind.signatureAbility),
        )
    }
