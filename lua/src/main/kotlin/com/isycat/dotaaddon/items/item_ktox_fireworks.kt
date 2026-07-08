package com.isycat.dotaaddon.items

import com.isycat.dota.types.lua.DotaAbilityBehavior
import com.isycat.dota.types.lua.ItemLua
import com.isycat.dota.types.lua.ParticleAttachment
import com.isycat.dota.types.lua.ParticleManager
import com.isycat.dota.types.lua.Vector
import com.isycat.dota.types.lua.randomFloat
import com.isycat.ktox.dota.Dota2Class
import com.isycat.ktox.dota.ItemKv
import com.isycat.ktox.dota.KvFlag

/**
 * Festival Fireworks — a 2-gold trinket that plays a short firework show around the carrier and
 * does absolutely nothing else. Zero combat effect, sells back for 1 gold; pure celebration.
 *
 * The class name IS the item's entity key (Dota requires the `item_` prefix), the same way a
 * [Dota2Class] modifier's class name is its engine name — hence the non-Kotlin casing.
 * End-to-end item showcase: generated KV (`npc_items_custom.txt`), engine-native precache of the
 * show particle + the addon's sound bank, a custom icon (`content/panorama/images/items/custom/` —
 * PNG + hand-authored `_png.vtex` descriptor, compiled by resourcecompiler), and a Lua behavior on use.
 */
@Suppress("ClassName")
@Dota2Class
@ItemKv(
    behavior = [DotaAbilityBehavior.NO_TARGET],
    textureName = "custom/ktox_fireworks",
    cooldown = 3.0,
    manaCost = 0,
    cost = 2,
    shopTags = "consumable",
    purchasable = KvFlag.YES,
    sellable = KvFlag.YES,
    droppable = KvFlag.YES,
    stackable = KvFlag.NO,
    precache = [
        "particles/econ/courier/courier_trail_fireworks/courier_trail_fireworks_explosion.vpcf",
        "particles/econ/courier/courier_trail_fireworks/courier_trail_fireworks_explosion_b.vpcf",
        "particles/econ/courier/courier_trail_fireworks/courier_trail_fireworks_explosion_c.vpcf",
        "soundevents/ktoxtest_sounds.vsndevts",
    ],
)
class item_ktox_fireworks : ItemLua {
    override fun onSpellStart() {
        val owner = caster
        owner.emitSound(BURST_SOUND)
        // A little volley: one burst of each colour variant, scattered above the carrier's head.
        // These are the courier fireworks' EXPLOSION child systems — the one-shot bursts the courier
        // trail spawns along its path — from a current econ item that renders in matches every day.
        // Each is fully self-contained (CP0 position only); the a/b/c variants ARE the colours, so no
        // tint control points are needed. (The 2014 CNY event booms and the trail parent both proved
        // dead ends: the booms tint from an undocumented event CP, and a trail emits per distance
        // moved — nothing on a stationary carrier.)
        BURST_PARTICLES.forEach { particlePath ->
            val burst =
                ParticleManager.createParticle(
                    particlePath,
                    ParticleAttachment.PATTACH_CUSTOMORIGIN,
                    owner,
                )
            val burstOrigin =
                owner.absOrigin +
                    Vector(
                        randomFloat(-SPREAD_UNITS, SPREAD_UNITS),
                        randomFloat(-SPREAD_UNITS, SPREAD_UNITS),
                        HEIGHT_UNITS + randomFloat(0f, HEIGHT_JITTER_UNITS),
                    )
            ParticleManager.setParticleControl(burst, 0, burstOrigin)
            ParticleManager.releaseParticleIndex(burst)
        }
    }

    companion object {
        /** One burst per entry — the courier fireworks explosion variants are the colour palette. */
        private val BURST_PARTICLES =
            listOf(
                "particles/econ/courier/courier_trail_fireworks/courier_trail_fireworks_explosion.vpcf",
                "particles/econ/courier/courier_trail_fireworks/courier_trail_fireworks_explosion_b.vpcf",
                "particles/econ/courier/courier_trail_fireworks/courier_trail_fireworks_explosion_c.vpcf",
            )

        /** Declared in `content/soundevents/ktoxtest_sounds.vsndevts`. */
        private const val BURST_SOUND = "ktox.firework.burst"

        private const val SPREAD_UNITS = 220f
        private const val HEIGHT_UNITS = 280f
        private const val HEIGHT_JITTER_UNITS = 160f
    }
}
