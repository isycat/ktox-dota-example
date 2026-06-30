package com.isycat.dotaaddon.shared.events

import com.isycat.dota.types.CustomGameEventKey
import com.isycat.ktox.annotations.ReplaceReferencesWithLiteral
import com.isycat.ktox.annotations.externalSource

/**
 * Payload for [WD_MESSAGE] — a transient HUD announcement string.
 *
 * Kept in its own file so its FQN-derived Lua require path (`shared/events/Announcement`) matches an
 * actual emitted file (one transpiled declaration per file). See WaveState.kt.
 */
data class Announcement(
    val text: String,
)

/** Server→client transient announcement. Typed key — its generic fixes the [Announcement] payload. */
@ReplaceReferencesWithLiteral("wd_message")
val WD_MESSAGE: CustomGameEventKey<Announcement> = externalSource()
