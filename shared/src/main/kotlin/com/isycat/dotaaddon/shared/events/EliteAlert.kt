package com.isycat.dotaaddon.shared.events

import com.isycat.dota.types.CustomGameEventKey
import com.isycat.ktox.annotations.ReplaceReferencesWithLiteral
import com.isycat.ktox.annotations.externalSource

/**
 * Payload for [WD_ELITE] — the name of the elite enemy that just spawned. The HUD
 * turns each one into a transient pop-up. One transpiled declaration per file (its FQN-derived Lua
 * require path must match an emitted file) — see WaveState.kt / Announcement.kt.
 */
data class EliteAlert(
    val name: String,
)

/** Server→client elite-spawned alert. Typed key — its generic fixes the [EliteAlert] payload. */
@ReplaceReferencesWithLiteral("wd_elite")
val WD_ELITE: CustomGameEventKey<EliteAlert> = externalSource()
