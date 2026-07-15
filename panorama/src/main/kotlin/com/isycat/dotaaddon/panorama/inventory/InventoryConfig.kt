package com.isycat.dotaaddon.panorama.inventory

/**
 * Client-side tuning for the drop-in inventory-bar module. Kept in the module's own package so the whole
 * `panorama.inventory` package copy-pastes into another addon as a self-contained unit.
 */
object InventoryConfig {
    /** How often the inventory bar re-reads the hero and refreshes each slot against the live inventory. */
    const val REFRESH_SECONDS = 0.1f

    /** Whether inventory slots show their bound key in the corner. */
    const val SHOW_KEYBINDS = true

    /** Carried (castable) slots 0-5 — the only slots with a usable cast keybind. */
    const val CARRIED_SLOT_COUNT = 6

    /** Backpack slots 6-8: items there can't be cast, so they never show a keybind. */
    const val BACKPACK_SLOT_COUNT = 3

    /** The carried slots render as a grid this many columns wide. */
    const val GRID_COLUMNS = 3
}
