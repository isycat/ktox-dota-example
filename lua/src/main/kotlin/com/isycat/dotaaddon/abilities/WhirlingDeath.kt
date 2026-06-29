package com.isycat.dotaaddon.abilities

import com.isycat.dota.types.lua.ApplyDamageOptions
import com.isycat.dota.types.lua.AbilityLua
import com.isycat.dota.types.lua.DamageTypes
import com.isycat.dota.types.lua.DotaAbilityBehavior
import com.isycat.dota.types.lua.DotaUnitTargetFlags
import com.isycat.dota.types.lua.DotaUnitTargetTeam
import com.isycat.dota.types.lua.DotaUnitTargetType
import com.isycat.dota.types.lua.FindOrder
import com.isycat.dota.types.lua.GameActivity
import com.isycat.dota.types.lua.GridNav
import com.isycat.dota.types.lua.SpellImmunityTypes
import com.isycat.dota.types.lua.ParticleAttachment
import com.isycat.dota.types.lua.ParticleManager
import com.isycat.dota.types.lua.Vector
import com.isycat.dota.types.lua.applyDamage
import com.isycat.dota.types.lua.findUnitsInRadius
import com.isycat.dotaaddon.shared.GameConfig
import com.isycat.ktox.dota.AbilityKv
import com.isycat.ktox.dota.AbilityValue
import com.isycat.ktox.dota.Dota2Class

/**
 * A faithful, all-Kotlin → Lua re-creation of Timbersaw's Whirling Death: a no-target whirl that shreds
 * trees and deals pure damage to enemies in a radius, dealing bonus damage per tree felled.
 *
 * `@Dota2Class` lowers it to `WhirlingDeath = class({})`; `@AbilityKv` generates its full KeyValues into
 * `npc_abilities_custom.txt` — every value (per-level cooldown / mana / damage, and the `AbilityValues`:
 * radius, tree bonus, stat-loss %, durations) is a Kotlin number array, the single source of truth. The
 * Lua reads them back at runtime — `abilityDamage` (`GetAbilityDamage`) and `getSpecialValueFor(...)` — so
 * the spell and the tooltip can never disagree.
 */
@Dota2Class
@AbilityKv(
    behavior = [DotaAbilityBehavior.NO_TARGET, DotaAbilityBehavior.AOE],
    maxLevel = 4,
    unitDamageType = [DamageTypes.PURE],
    spellImmunity = [SpellImmunityTypes.ENEMIES_NO],
    castAnimation = [GameActivity.DOTA_CAST_ABILITY_1],
    textureName = "shredder_whirling_death",
    sound = "Hero_Shredder.WhirlingDeath",
    castPoint = [0.0, 0.0, 0.0, 0.0],
    cooldown = [7.5, 7.0, 6.5, 6.0],
    manaCost = [100, 100, 100, 100],
    damage = [60, 120, 180, 240],
    values = [
        AbilityValue("radius", [325.0]),
        AbilityValue("tree_bonus_damage", [9.0, 16.0, 23.0, 30.0]),
        AbilityValue("stat_loss_pct", [13.0]),
        AbilityValue("stat_loss_universal_pct", [5.0]),
        AbilityValue("stat_loss_duration", [7.0, 9.0, 11.0, 13.0]),
    ],
)
class WhirlingDeath : AbilityLua {
    override fun onSpellStart() {
        val origin = caster.absOrigin
        val radius = getSpecialValueFor("radius")
        val treeBonus = getSpecialValueFor("tree_bonus_damage")

        // Shred every tree in range; each felled tree adds bonus damage (the ability's signature).
        val felled = GridNav.getAllTreesAroundPoint(origin, radius, false).size
        GridNav.destroyTreesAroundPoint(origin, radius, false)
        val totalDamage = abilityDamage.toFloat() + treeBonus * felled

        // The whirl sound + particle (stock Shredder assets). The particle reads its size from control
        // point 1 — without it the whirl renders as a dot — so set CP1 to the radius before releasing.
        caster.emitSound("Hero_Shredder.WhirlingDeath")
        val whirl =
            ParticleManager.createParticle(
                GameConfig.WHIRLING_DEATH_PARTICLE,
                ParticleAttachment.PATTACH_ABSORIGIN_FOLLOW,
                caster,
            )
        ParticleManager.setParticleControl(whirl, 1, Vector(radius, radius, radius))
        ParticleManager.releaseParticleIndex(whirl)

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
