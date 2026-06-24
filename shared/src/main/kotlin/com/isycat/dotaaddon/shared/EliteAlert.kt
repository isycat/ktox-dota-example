package com.isycat.dotaaddon.shared

/**
 * Payload for [GameConfig.EVENT_ELITE] — the name of the elite enemy that just spawned. The HUD
 * turns each one into a transient pop-up. One declaration per file (its FQN-derived Lua require
 * path must match an emitted file) — see WaveState.kt / Announcement.kt.
 */
data class EliteAlert(
    val name: String,
)
