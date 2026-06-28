package com.isycat.dotaaddon.shared

/**
 * The client↔server event contract for the reusable HUD modules (the ability bar and the inventory bar).
 *
 * These are the custom-game-event names the Panorama HUD fires and the lua backend listens for. They live
 * here — co-located with their payloads ([UpgradeRequest], [SwapItemsRequest]) and away from the game's
 * [GameConfig] — so the HUD systems are self-contained: to lift the ability/inventory bar into another
 * ktox-dota project you copy the panel + slot-view files, these event names + their payloads, the matching
 * SCSS partial, and implement the listener documented in `panorama/HUD_MODULES.md`. The string values are
 * arbitrary (any unique id works); only that client and server agree matters, which referencing this one
 * object from both guarantees.
 */
object HudEvents {
    /** Inventory bar → server: "swap these two inventory slots" (payload [SwapItemsRequest]). */
    const val SWAP_ITEMS = "wd_swap_items"

    /** Ability bar → server: "level up this ability" (payload [UpgradeRequest]). */
    const val UPGRADE_ABILITY = "wd_upgrade_ability"
}
