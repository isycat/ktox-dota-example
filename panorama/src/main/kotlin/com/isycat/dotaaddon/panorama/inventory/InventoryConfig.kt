package com.isycat.dotaaddon.panorama.inventory

/**
 * Client-side tuning for the drop-in **inventory bar** module. Kept inside the module's own package so the
 * whole `panorama.inventory` package copy-pastes into another addon as a self-contained unit (the only
 * addon-specific coupling left is the swap/sell event keys it imports from the consuming addon's events).
 */
object InventoryConfig {
    /** How often the inventory bar re-reads the hero and refreshes each slot against the live inventory. */
    const val REFRESH_SECONDS = 0.1f
}
