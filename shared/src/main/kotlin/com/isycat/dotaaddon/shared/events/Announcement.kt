package com.isycat.dotaaddon.shared.events

import com.isycat.dota.types.CustomGameEventKey
import com.isycat.ktox.annotations.ReplaceReferencesWithLiteral
import com.isycat.ktox.annotations.externalSource

@ReplaceReferencesWithLiteral("wd_message")
val WD_MESSAGE: CustomGameEventKey<Announcement> = externalSource()

/** A transient HUD announcement string (server→client). */
data class Announcement(
    val text: String,
)
