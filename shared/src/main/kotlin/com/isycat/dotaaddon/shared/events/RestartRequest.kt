package com.isycat.dotaaddon.shared.events

import com.isycat.dota.types.CustomGameEventKey
import com.isycat.ktox.annotations.ReplaceReferencesWithLiteral
import com.isycat.ktox.annotations.externalSource

@ReplaceReferencesWithLiteral("wd_restart")
val WD_RESTART: CustomGameEventKey<RestartRequest> = externalSource()

/** The client→server "play again" signal — no data, but a typed payload keeps the event API uniform. */
class RestartRequest
