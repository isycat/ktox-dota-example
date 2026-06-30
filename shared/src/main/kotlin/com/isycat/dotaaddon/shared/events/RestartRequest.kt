package com.isycat.dotaaddon.shared.events

import com.isycat.dota.types.CustomGameEventKey
import com.isycat.ktox.annotations.ReplaceReferencesWithLiteral
import com.isycat.ktox.annotations.externalSource

@ReplaceReferencesWithLiteral("wd_restart")
val WD_RESTART: CustomGameEventKey<RestartRequest> = externalSource()

/**
 * The client→server "play again" signal. It carries no data (the server just resets the run), but a typed
 * payload keeps the event API consistent and gives the Lua listener a concrete type to receive. Kept alone
 * as the file's one transpiled declaration so its FQN-derived Lua require path matches the emitted file.
 */
class RestartRequest
