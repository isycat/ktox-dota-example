package com.isycat.dotaaddon.shared.events

import com.isycat.dota.types.CustomGameEventKey
import com.isycat.ktox.annotations.ReplaceReferencesWithLiteral
import com.isycat.ktox.annotations.externalSource

@ReplaceReferencesWithLiteral("wd_swap_items")
val WD_SWAP: CustomGameEventKey<SwapItemsRequest> = externalSource()

/**
 * The client→server "swap these two inventory slots" signal, sent when the player drags one item onto
 * another slot in the custom inventory bar. Carries SLOT indices, not entity indices: the server swaps on
 * the requesting player's own hero (re-validated to valid slots), so a forged event can only rearrange that
 * player's inventory. Kept alone as the file's one transpiled declaration so its FQN-derived Lua require
 * path matches the emitted file.
 */
data class SwapItemsRequest(
    val fromSlot: Int,
    val toSlot: Int,
)
