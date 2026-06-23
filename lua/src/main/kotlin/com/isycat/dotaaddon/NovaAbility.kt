package com.isycat.dotaaddon

import com.isycat.ktox.dota.Dota2Class

/**
 * Engine-bound ability class, lowered by `@Dota2Class` to the Lua
 * `NovaAbility = class({})` idiom. Kept in its own file (like ExampleAbility):
 * see note below — co-locating it with another top-level declaration hangs the
 * Lua transpiler.
 *
 * The reusable blast logic lives in [Nova.cast], shared with the `"nova"` chat
 * command in [WaveDefense].
 */
@Dota2Class
class NovaAbility {
    fun onSpellStart() {
        // When bound as a real ability (npc_abilities_custom.txt → ability_lua),
        // resolve the caster via self:GetCaster() and call Nova.cast(caster).
    }
}
