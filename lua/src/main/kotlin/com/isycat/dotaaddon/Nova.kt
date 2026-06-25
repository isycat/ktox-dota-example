package com.isycat.dotaaddon

import com.isycat.dota.types.lua.ApplyDamageOptions
import com.isycat.dota.types.lua.BaseNPC
import com.isycat.dota.types.lua.DamageTypes
import com.isycat.dota.types.lua.DotaUnitTargetFlags
import com.isycat.dota.types.lua.DotaUnitTargetTeam
import com.isycat.dota.types.lua.DotaUnitTargetType
import com.isycat.dota.types.lua.FindOrder
import com.isycat.dota.types.lua.ParticleAttachment
import com.isycat.dota.types.lua.ParticleManager
import com.isycat.dota.types.lua.applyDamage
import com.isycat.dota.types.lua.findUnitsInRadius
import com.isycat.dotaaddon.shared.GameConfig
import com.isycat.ktox.dota.Dota2Class

/**
 * The "Nova" area-of-effect blast — the showcase combat primitive.
 *
 * Demonstrates a spatial query ([findUnitsInRadius]), structured damage
 * ([applyDamage] + [ApplyDamageOptions]), and a particle effect
 * ([ParticleManager]) in one place. The reusable blast logic lives in [cast],
 * shared with the `"nova"` chat command (WaveDefense) and [NovaAbility].
 */
object Nova {
    fun cast(caster: BaseNPC): Int {
        val enemies =
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
            )

        enemies.forEach { enemy ->
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
        }

        val fx =
            ParticleManager.createParticle(
                GameConfig.NOVA_PARTICLE,
                ParticleAttachment.PATTACH_ABSORIGIN_FOLLOW,
                caster,
            )
        ParticleManager.releaseParticleIndex(fx)

        return enemies.size
    }
}

/**
 * Engine-bound ability class, lowered by `@Dota2Class` to the Lua `NovaAbility = class({})` idiom — the
 * example of the `@Dota2Class` feature. Co-located with [Nova] (the colocation transpiler hang it was
 * once split to avoid is fixed). The reusable blast logic lives in [Nova.cast].
 */
@Dota2Class
class NovaAbility {
    fun onSpellStart() {
        // When bound as a real ability (npc_abilities_custom.txt → ability_lua),
        // resolve the caster via self:GetCaster() and call Nova.cast(caster).
    }
}
