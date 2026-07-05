package com.isycat.dotaaddon.shared.events

import com.isycat.dota.types.CustomGameEventKey
import com.isycat.ktox.annotations.ReplaceReferencesWithLiteral
import com.isycat.ktox.annotations.externalSource

@ReplaceReferencesWithLiteral("wd_swap_items")
val WD_SWAP: CustomGameEventKey<SwapItemsRequest> = externalSource()

/**
 * Client→server "swap these two inventory slots" (from a drag) on a specific [unit]. Carries the unit so a
 * CONTROLLED non-hero (a Lone Druid bear, a spirit bear, any commandable summon) rearranges its OWN items,
 * not the hero's. The server validates the unit belongs to the local player before swapping, so a forged
 * event can still only rearrange that player's own units.
 */
data class SwapItemsRequest(
    val fromSlot: Int,
    val toSlot: Int,
    /** Entity index of the unit whose inventory to rearrange (the currently-controlled unit). */
    val unit: Int,
)
