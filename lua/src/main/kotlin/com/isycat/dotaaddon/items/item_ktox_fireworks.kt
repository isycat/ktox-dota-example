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
        "particles/themed_fx/cny_fireworks_boom_a.vpcf",
        "soundevents/ktoxtest_sounds.vsndevts",
    ],
)
class item_ktox_fireworks : ItemLua {
    override fun onSpellStart() {
        val owner = caster
        owner.emitSound(BURST_SOUND)
        // A little volley: staggered bursts scattered above the carrier's head.
        repeat(BURST_COUNT) {
            val burst =
                ParticleManager.createParticle(
                    BURST_PARTICLE,
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
            // The CNY boom TINTS from a control point: uncached it drew opaque red-X fallbacks (the
            // pattern showed), precached it modulates by a colour CP that defaults to black — i.e.
            // invisible. The event's exact colour slot isn't documented, so feed a festive random
            // colour to every common colour CP; the unused ones are simply ignored.
            val tint =
                Vector(
                    randomFloat(TINT_CHANNEL_MIN, TINT_CHANNEL_MAX),
                    randomFloat(TINT_CHANNEL_MIN, TINT_CHANNEL_MAX),
                    randomFloat(TINT_CHANNEL_MIN, TINT_CHANNEL_MAX),
                )
            ParticleManager.setParticleControl(burst, TINT_CP_PRIMARY, tint)
            ParticleManager.setParticleControl(burst, TINT_CP_SECONDARY, tint)
            ParticleManager.setParticleControl(burst, TINT_CP_TERTIARY, tint)
            ParticleManager.setParticleControl(burst, TINT_CP_ECON_A, tint)
            ParticleManager.setParticleControl(burst, TINT_CP_ECON_B, tint)
            ParticleManager.releaseParticleIndex(burst)
        }
    }

    companion object {
        private const val BURST_PARTICLE = "particles/themed_fx/cny_fireworks_boom_a.vpcf"

        /** Declared in `content/soundevents/ktoxtest_sounds.vsndevts`. */
        private const val BURST_SOUND = "ktox.firework.burst"

        private const val BURST_COUNT = 3
        private const val SPREAD_UNITS = 220f
        private const val HEIGHT_UNITS = 280f
        private const val HEIGHT_JITTER_UNITS = 160f

        // The colour control points Valve effects commonly read (1-3 gameplay, 15-16 econ tints).
        private const val TINT_CP_PRIMARY = 1
        private const val TINT_CP_SECONDARY = 2
        private const val TINT_CP_TERTIARY = 3
        private const val TINT_CP_ECON_A = 15
        private const val TINT_CP_ECON_B = 16

        /** Festive tint channels stay bright: dark channels are what made the un-tinted boom invisible. */
        private const val TINT_CHANNEL_MIN = 120f
        private const val TINT_CHANNEL_MAX = 255f
    }
}
