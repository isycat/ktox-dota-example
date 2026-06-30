package com.isycat.dotaaddon.panorama.abilitybar

/**
 * Client-side tuning for the drop-in **ability bar** module. Kept inside the module's own package so the
 * whole `panorama.abilitybar` package copy-pastes into another addon as a self-contained unit (the only
 * addon-specific coupling left is the upgrade event key it imports from the consuming addon's events).
 */
object AbilityBarConfig {
    /**
     * How often the bar re-reads the hero and refreshes cooldowns. Fast so the float cooldown countdown +
     * spiral update smoothly; the per-tick cost is kept low by idle slots short-circuiting their DOM
     * writes (see AbilitySlotView).
     */
    const val REFRESH_SECONDS = 0.1f

    /**
     * Refresh ticks between full ability-layout scans (see AbilitiesPanel.refresh). Cooldowns refresh
     * every tick; the heavier "did the layout change" scan only every Nth — 5 ticks ≈ 0.5s, far more
     * often than a player can level up, so rebuilds still feel instant.
     */
    const val LAYOUT_SCAN_TICKS = 5
}
