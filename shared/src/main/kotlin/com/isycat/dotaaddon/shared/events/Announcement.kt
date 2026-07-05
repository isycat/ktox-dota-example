package com.isycat.dotaaddon.shared.events

import com.isycat.dota.types.CustomGameEventKey
import com.isycat.ktox.annotations.ReplaceReferencesWithLiteral
import com.isycat.ktox.annotations.externalSource

@ReplaceReferencesWithLiteral("wd_message")
val WD_MESSAGE: CustomGameEventKey<Announcement> = externalSource()

/**
 * A transient HUD announcement (server→client). Carries a LOCALIZATION TOKEN and its placeholder
 * values, never display text — the client binds [value]/[value2] as `{d:...}` dialog variables and
 * [name] (itself a token, e.g. a boss name) as `{s:...}`, then localizes [token]. Token names live
 * in [com.isycat.dotaaddon.shared.WdTokens]; the strings live in `resource/addon_english.txt`.
 */
data class Announcement(
    val token: String,
    val value: Int = 0,
    val value2: Int = 0,
    val name: String = "",
)
