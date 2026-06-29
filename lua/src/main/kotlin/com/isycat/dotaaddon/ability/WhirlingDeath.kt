package com.isycat.dotaaddon.ability

import com.isycat.dota.types.lua.AbilityLua
import com.isycat.dota.types.lua.ApplyDamageOptions
import com.isycat.dota.types.lua.DamageTypes
import com.isycat.dota.types.lua.DotaAbilityBehavior
import com.isycat.dota.types.lua.DotaUnitTargetFlags
import com.isycat.dota.types.lua.DotaUnitTargetTeam
import com.isycat.dota.types.lua.DotaUnitTargetType
import com.isycat.dota.types.lua.FindOrder
import com.isycat.dota.types.lua.applyDamage
import com.isycat.dota.types.lua.findUnitsInRadius
import com.isycat.dotaaddon.shared.GameConfig
import com.isycat.ktox.dota.AbilityKv
import com.isycat.ktox.dota.Dota2Class

/**
 * A simple, all-Kotlin → Lua ability modelled on Timbersaw's Whirling Death: a no-target whirl that
 * deals pure damage to every enemy around the caster.
 *
 * `@Dota2Class` lowers it to `WhirlingDeath = class({})`; `@AbilityKv` registers it in
 * `npc_abilities_custom.txt` with `BaseClass "ability_lua"` + `ScriptFile "ability/WhirlingDeath.lua"`
 * and the gameplay keys. The damage is read from the KV (`abilityDamage` → `GetAbilityDamage`) rather
 * than hard-coded, so the in-game tooltip's DAMAGE value and what the spell actually deals can never
 * disagree.
 */
@Dota2Class
@AbilityKv(
    behavior = [DotaAbilityBehavior.NO_TARGET],
    cooldown = 8.0,
    manaCost = 90,
    damage = 240,
    maxLevel = 1,
)
class WhirlingDeath : AbilityLua {
    override fun onSpellStart() {
        findUnitsInRadius(
            caster.teamNumber,
            caster.absOrigin,
            null,
            GameConfig.WHIRLING_DEATH_RADIUS,
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
                    damage = abilityDamage.toFloat(),
                    damage_type = DamageTypes.PURE,
                    damage_flags = null,
                    ability = this,
                ),
            )
        }
    }
}
