package com.isycat.dotaaddon.shared.events

import com.isycat.dota.types.CustomGameEventKey
import com.isycat.ktox.annotations.ReplaceReferencesWithLiteral
import com.isycat.ktox.annotations.externalSource

@ReplaceReferencesWithLiteral("wd_elite")
val WD_ELITE: CustomGameEventKey<EliteAlert> = externalSource()

/**
 * An elite just spawned (server→client); the HUD shows a transient pop-up per alert. [name] is the
 * elite's UNIT name, which doubles as its display-name localization token (standard unit-name
 * localization — the `resource/addon_english.txt` entry is keyed by the unit name).
 */
data class EliteAlert(
    val name: String,
)
