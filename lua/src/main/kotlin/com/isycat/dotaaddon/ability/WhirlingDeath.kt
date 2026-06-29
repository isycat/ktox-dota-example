package com.isycat.dotaaddon.ability

import com.isycat.dota.types.lua.ApplyDamageOptions
import com.isycat.dota.types.lua.AbilityLua
import com.isycat.dota.types.lua.DamageTypes
import com.isycat.dota.types.lua.DotaUnitTargetFlags
import com.isycat.dota.types.lua.DotaUnitTargetTeam
import com.isycat.dota.types.lua.DotaUnitTargetType
import com.isycat.dota.types.lua.FindOrder
import com.isycat.dota.types.lua.GridNav
import com.isycat.dota.types.lua.ParticleAttachment
import com.isycat.dota.types.lua.ParticleManager
import com.isycat.dota.types.lua.applyDamage
import com.isycat.dota.types.lua.findUnitsInRadius
import com.isycat.dotaaddon.shared.GameConfig
import com.isycat.ktox.dota.Dota2Class

/**
 * A faithful, all-Kotlin → Lua re-creation of Timbersaw's Whirling Death: a no-target whirl that shreds
 * trees and deals pure damage to enemies in a radius, dealing bonus damage per tree felled.
 *
 * `@Dota2Class` lowers it to `WhirlingDeath = class({})`. Its KeyValues (behaviour, per-level cooldown /
 * damage, and the `AbilityValues` — radius, tree bonus, stat-loss %, durations — plus the icon, sound and
 * `ScriptFile`) are authored faithfully in `scripts/npc/npc_abilities_custom.txt`, NOT via `@AbilityKv`,
 * so every value is real and shows in the tooltip. The Lua reads them back at runtime — `abilityDamage`
 * (`GetAbilityDamage`) and `getSpecialValueFor(...)` — so the spell and the tooltip can never disagree.
 */
@Dota2Class
class WhirlingDeath : AbilityLua {
    override fun onSpellStart() {
        val origin = caster.absOrigin
        val radius = getSpecialValueFor("radius")
        val treeBonus = getSpecialValueFor("tree_bonus_damage")

        // Shred every tree in range; each felled tree adds bonus damage (the ability's signature).
        val felled = GridNav.getAllTreesAroundPoint(origin, radius, false).size
        GridNav.destroyTreesAroundPoint(origin, radius, false)
        val totalDamage = abilityDamage.toFloat() + treeBonus * felled

        ParticleManager
            .createParticle(GameConfig.WHIRLING_DEATH_PARTICLE, ParticleAttachment.PATTACH_ABSORIGIN_FOLLOW, caster)
            .let { ParticleManager.releaseParticleIndex(it) }

        findUnitsInRadius(
            caster.teamNumber,
            origin,
            null,
            radius,
            DotaUnitTargetTeam.ENEMY,
            DotaUnitTargetType.BASIC,
            DotaUnitTargetFlags.NONE,
            FindOrder.ANY_ORDER,
            false,
        ).forEach { enemy ->
            applyDamage(
                ApplyDamageOptions(
                    victim = enemy,
                    attacker = caster,
                    damage = totalDamage,
                    damage_type = DamageTypes.PURE,
                    damage_flags = null,
                    ability = this,
                ),
            )
        }
    }
}
