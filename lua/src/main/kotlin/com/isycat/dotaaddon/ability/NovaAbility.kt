package com.isycat.dotaaddon.ability

import com.isycat.dota.types.lua.AbilityLua
import com.isycat.dota.types.lua.DotaAbilityBehavior
import com.isycat.dotaaddon.Nova
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
)
class NovaAbility : AbilityLua {
    override fun onSpellStart() {
        val c = caster ?: return
        Nova.cast(c)
    }
}
