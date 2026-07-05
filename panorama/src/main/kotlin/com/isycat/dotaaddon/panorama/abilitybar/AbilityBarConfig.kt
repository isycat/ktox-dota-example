package com.isycat.dotaaddon.panorama.abilitybar

/**
 * Client-side tuning for the drop-in ability-bar module. Kept in the module's own package so the whole
 * `panorama.abilitybar` package copy-pastes into another addon as a self-contained unit.
 */
object AbilityBarConfig {
    /** How often the bar re-reads the hero and refreshes cooldowns (fast, for a smooth countdown). */
    const val REFRESH_SECONDS = 0.1f

    /** Refresh ticks between full layout scans (5 ≈ 0.5s — far more often than a player levels up). */
    const val LAYOUT_SCAN_TICKS = 5

    /**
     * How the talent tree is revealed from behind its tab. `true` = it shows while the mouse is over the
     * tab (a pure-CSS hover reveal); `false` = the tab click-toggles it. Either way the tree starts hidden.
     */
    const val TALENT_REVEAL_ON_HOVER = true

    /** Whether ability slots show their bound key in the corner. */
    const val SHOW_KEYBINDS = true
}
