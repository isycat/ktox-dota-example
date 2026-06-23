package com.isycat.dotaaddon.shared

/**
 * Payload for `GameConfig.EVENT_MESSAGE` — a transient HUD announcement string.
 *
 * Kept in its own file so its FQN-derived Lua require path (`shared/Announcement`)
 * matches an actual emitted file (one declaration per file). See WaveState.kt.
 */
data class Announcement(val text: String)
