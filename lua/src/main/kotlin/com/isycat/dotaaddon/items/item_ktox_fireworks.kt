package com.isycat.dotaaddon.items

import com.isycat.dota.types.lua.DotaAbilityBehavior
import com.isycat.dota.types.lua.ItemLua
import com.isycat.dota.types.lua.ParticleAttachment
import com.isycat.dota.types.lua.ParticleManager
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
        "particles/econ/courier/courier_trail_fireworks/courier_trail_fireworks.vpcf",
        "soundevents/ktoxtest_sounds.vsndevts",
    ],
)
class item_ktox_fireworks : ItemLua {
    override fun onSpellStart() {
        val owner = caster
        owner.emitSound(BURST_SOUND)
        // The courier fireworks trail follows the carrier and pops a stream of small bursts. It is
        // fully self-contained — no control points — which is why it's the pick: CP-driven event
        // particles (the CNY booms) render invisibly without their event's control-point setup.
        val show =
            ParticleManager.createParticle(
                FIREWORKS_PARTICLE,
                ParticleAttachment.PATTACH_ABSORIGIN_FOLLOW,
                owner,
            )
        // End the show after a beat (with the particle's own end-caps), then free the index.
        owner.setContextThink(
            THINK_SHOW_END,
            { _ ->
                ParticleManager.destroyParticle(show, false)
                ParticleManager.releaseParticleIndex(show)
                null
            },
            SHOW_SECONDS,
        )
    }

    companion object {
        /** The courier fireworks trail — self-contained (no control points), follows its unit. */
        private const val FIREWORKS_PARTICLE =
            "particles/econ/courier/courier_trail_fireworks/courier_trail_fireworks.vpcf"

        /** Declared in `content/soundevents/ktoxtest_sounds.vsndevts`. */
        private const val BURST_SOUND = "ktox.firework.burst"

        /** Keyed think slot for ending the show (one per carrier; a re-cast just extends the show). */
        private const val THINK_SHOW_END = "ktox_fireworks_show_end"

        /** Show length — under the item's cooldown, so shows never stack. */
        private const val SHOW_SECONDS = 2.5f
    }
}
