package com.isycat.dotaaddon.shared

/**
 * Payload for [GameConfig.EVENT_SWAP_ITEMS] — the client→server "swap these two inventory slots" signal
 * sent when the player drags one item onto another slot in the custom inventory bar. Carries SLOT
 * indices, not entity indices: the server swaps on the requesting player's own hero, so a forged event
 * can only rearrange that player's inventory (and only within valid slots, re-validated server-side).
 *
 * One declaration per file so its FQN-derived require path (`shared/SwapItemsRequest`) matches an
 * emitted file. See UpgradeRequest.kt.
 */
data class SwapItemsRequest(
    val fromSlot: Int,
    val toSlot: Int,
)
