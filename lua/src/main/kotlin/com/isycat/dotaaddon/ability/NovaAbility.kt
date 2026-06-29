package com.isycat.dotaaddon.ability

import com.isycat.dota.types.lua.AbilityLua
import com.isycat.dota.types.lua.ApplyDamageOptions
import com.isycat.dota.types.lua.DamageTypes
import com.isycat.dota.types.lua.DotaAbilityBehavior
import com.isycat.dota.types.lua.DotaUnitTargetFlags
import com.isycat.dota.types.lua.DotaUnitTargetTeam
import com.isycat.dota.types.lua.DotaUnitTargetType
import com.isycat.dota.types.lua.FindOrder
import com.isycat.dota.types.lua.ParticleAttachment
import com.isycat.dota.types.lua.ParticleManager
import com.isycat.dota.types.lua.applyDamage
import com.isycat.dota.types.lua.findUnitsInRadius
import com.isycat.dotaaddon.shared.GameConfig
import com.isycat.ktox.dota.AbilityKv
import com.isycat.ktox.dota.Dota2Class

/**
 * Engine-bound ability class — the castable form of the [Nova] blast.
 *
 * `@Dota2Class` lowers it to the Lua `NovaAbility = class({})` idiom; `@AbilityKv` registers it in
 * `npc_abilities_custom.txt` (the generator fills in `BaseClass "ability_lua"` + `ScriptFile`
 * "ability/NovaAbility.lua", and the typed gameplay keys). It extends [AbilityLua], overriding just
 * `onSpellStart` and reading the inherited `caster` (`self:GetCaster()`); the reusable blast logic lives
 * in [Nova.cast]. In its own file in `…dotaaddon.ability` so the transpiled lua lands at
 * `vscripts/ability/NovaAbility.lua`.
 */
@Dota2Class
@AbilityKv(
    behavior = [DotaAbilityBehavior.NO_TARGET],
    cooldown = 6.0,
    manaCost = 75,
    maxLevel = 1, // granted at max, so the abilities bar never offers to "upgrade" it — a click just casts
)
class NovaAbility : AbilityLua {
    // caster is non-null on a BaseAbility (an ability always has an owner) — no null guard needed, unlike
    // a modifier's nullable caster (Buff.caster). The blast logic lives in Nova.cast.
    override fun onSpellStart() {
        println("hello! from NovaAbility")
        findUnitsInRadius(
            caster.teamNumber,
            caster.absOrigin,
            null,
            GameConfig.NOVA_RADIUS,
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
                    damage = GameConfig.NOVA_DAMAGE,
                    damage_type = DamageTypes.MAGICAL,
                    damage_flags = null,
                    ability = null,
                ),
            )
            ParticleManager
                .createParticle(
                    GameConfig.NOVA_PARTICLE,
                    ParticleAttachment.PATTACH_ABSORIGIN_FOLLOW,
                    enemy,
                ).let { ParticleManager.releaseParticleIndex(it) }
        }
    }
}
