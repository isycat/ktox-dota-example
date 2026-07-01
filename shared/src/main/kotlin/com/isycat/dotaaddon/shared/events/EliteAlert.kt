package com.isycat.dotaaddon.shared.events

import com.isycat.dota.types.CustomGameEventKey
import com.isycat.ktox.annotations.ReplaceReferencesWithLiteral
import com.isycat.ktox.annotations.externalSource

@ReplaceReferencesWithLiteral("wd_elite")
val WD_ELITE: CustomGameEventKey<EliteAlert> = externalSource()

/** The name of an elite that just spawned (server→client); the HUD shows a transient pop-up per alert. */
data class EliteAlert(
    val name: String,
)
