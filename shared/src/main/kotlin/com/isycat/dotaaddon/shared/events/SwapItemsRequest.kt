package com.isycat.dotaaddon.shared.events

import com.isycat.dota.types.CustomGameEventKey
import com.isycat.ktox.annotations.ReplaceReferencesWithLiteral
import com.isycat.ktox.annotations.externalSource

/**
 * Payload for [WD_SWAP] — the client→server "swap these two inventory slots" signal
 * sent when the player drags one item onto another slot in the custom inventory bar. Carries SLOT
 * indices, not entity indices: the server swaps on the requesting player's own hero, so a forged event
 * can only rearrange that player's inventory (and only within valid slots, re-validated server-side).
 *
 * One transpiled declaration per file so its FQN-derived require path (`shared/events/SwapItemsRequest`)
 * matches an emitted file. See UpgradeRequest.kt.
 */
data class SwapItemsRequest(
    val fromSlot: Int,
    val toSlot: Int,
)

/** Client→server inventory-swap request. Typed key — its generic fixes the [SwapItemsRequest] payload. */
@ReplaceReferencesWithLiteral("wd_swap_items")
val WD_SWAP: CustomGameEventKey<SwapItemsRequest> = externalSource()
