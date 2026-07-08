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
 * show particle + the addon's sound bank, a custom icon (a raw PNG at
 * `resource/flash3/images/items/ktox_fireworks.png` — item icons use the engine's legacy flash3
 * loader, NOT the compiled panorama-image pipeline), and a Lua behavior on use.
 */
@Suppress("ClassName")
@Dota2Class
@ItemKv(
    behavior = [DotaAbilityBehavior.NO_TARGET],
    textureName = "item_ktox_fireworks",
    cooldown = 3.0,
    manaCost = 0,
    cost = 2,
    shopTags = "consumable",
    purchasable = KvFlag.YES,
    sellable = KvFlag.YES,
    droppable = KvFlag.YES,
    stackable = KvFlag.NO,
    precache = [
        "particles/econ/events/consolation/consolation_fireworks_1.vpcf",
        "soundevents/ktoxtest_sounds.vsndevts",
    ],
)
class item_ktox_fireworks : ItemLua {
    override fun onSpellStart() {
        val owner = caster
        owner.emitSound(BURST_SOUND)
        // Dota's own complete fireworks display (the TI consolation show): a single self-launching
        // system — rockets, trails and coloured bursts all composed by Valve. Spawned at the
        // carrier's feet; the show launches upward from there.
        val show =
            ParticleManager.createParticle(
                FIREWORKS_PARTICLE,
                ParticleAttachment.PATTACH_CUSTOMORIGIN,
                owner,
            )
        ParticleManager.setParticleControl(show, 0, owner.absOrigin)
        ParticleManager.releaseParticleIndex(show)
    }

    companion object {
        /** Valve's complete composed fireworks show — one system, no control points beyond position. */
        private const val FIREWORKS_PARTICLE = "particles/econ/events/consolation/consolation_fireworks_1.vpcf"

        /** Declared in `content/soundevents/ktoxtest_sounds.vsndevts`. */
        private const val BURST_SOUND = "ktox.firework.burst"
    }
}
