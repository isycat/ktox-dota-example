package com.isycat.dotaaddon.shared.events

import com.isycat.dota.types.CustomGameEventKey
import com.isycat.ktox.annotations.ReplaceReferencesWithLiteral
import com.isycat.ktox.annotations.externalSource

@ReplaceReferencesWithLiteral("wd_swap_items")
val WD_SWAP: CustomGameEventKey<SwapItemsRequest> = externalSource()

/**
 * Client→server "swap these two inventory slots" (from a drag). Carries SLOTS, not entity indices, so the
 * server swaps on the requesting player's own hero — a forged event can only rearrange that player's items.
 */
data class SwapItemsRequest(
    val fromSlot: Int,
    val toSlot: Int,
)
