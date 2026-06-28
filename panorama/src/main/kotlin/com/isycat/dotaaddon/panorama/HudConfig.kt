package com.isycat.dotaaddon.panorama

/**
 * Client-side tuning for the reusable HUD modules (ability bar + inventory bar). These are pure
 * Panorama-render concerns (how often the bars poll the engine), separate from gameplay config — they
 * live here, not in the game's [com.isycat.dotaaddon.shared.GameConfig], so the HUD systems are
 * self-contained and copy-pasteable. See `panorama/HUD_MODULES.md`.
 */
object HudConfig {
    /**
     * How often the ability + inventory bars re-read the hero and refresh cooldowns. Fast so the float
     * cooldown countdown + spiral update smoothly; the per-tick cost is kept low by idle slots
     * short-circuiting their DOM writes (see AbilitySlotView/ItemSlotView).
     */
    const val REFRESH_SECONDS = 0.1f

    /**
     * Refresh ticks between full ability-layout scans (see AbilitiesPanel.refresh). Cooldowns refresh
     * every tick; the heavier "did the layout change" scan only every Nth — 5 ticks ≈ 0.5s, far more
     * often than a player can level up, so rebuilds still feel instant.
     */
    const val ABILITY_LAYOUT_SCAN_TICKS = 5
}
