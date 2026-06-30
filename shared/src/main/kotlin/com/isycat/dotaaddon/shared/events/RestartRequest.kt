package com.isycat.dotaaddon.shared.events

import com.isycat.dota.types.CustomGameEventKey
import com.isycat.ktox.annotations.ReplaceReferencesWithLiteral
import com.isycat.ktox.annotations.externalSource

/**
 * Payload for [WD_RESTART] — the client→server "play again" signal. It carries no
 * data (the server just resets the run), but a typed payload keeps the event API consistent with
 * [WaveState] / [Announcement] and gives the Lua listener a concrete type to receive.
 *
 * One transpiled declaration per file so its FQN-derived Lua require path (`shared/events/RestartRequest`)
 * matches an emitted file. See WaveState.kt / Announcement.kt.
 */
class RestartRequest

/** Client→server "play again" request. Typed key — its generic fixes the [RestartRequest] payload. */
@ReplaceReferencesWithLiteral("wd_restart")
val WD_RESTART: CustomGameEventKey<RestartRequest> = externalSource()
