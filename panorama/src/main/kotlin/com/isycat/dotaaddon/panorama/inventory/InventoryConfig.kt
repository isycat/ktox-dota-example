package com.isycat.dotaaddon.panorama.inventory

/**
 * Client-side tuning for the drop-in inventory-bar module. Kept in the module's own package so the whole
 * `panorama.inventory` package copy-pastes into another addon as a self-contained unit.
 */
object InventoryConfig {
    /** How often the inventory bar re-reads the hero and refreshes each slot against the live inventory. */
    const val REFRESH_SECONDS = 0.1f
}
