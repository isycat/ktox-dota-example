package com.isycat.dotaaddon.shared.events

import com.isycat.dota.types.CustomGameEventKey
import com.isycat.ktox.annotations.ReplaceReferencesWithLiteral
import com.isycat.ktox.annotations.externalSource

@ReplaceReferencesWithLiteral("wd_elite")
val WD_ELITE: CustomGameEventKey<EliteAlert> = externalSource()

/**
 * The name of an elite enemy that just spawned (server→client); the HUD turns each one into a transient
 * pop-up. Kept alone as the file's one transpiled declaration so its FQN-derived Lua require path matches
 * the emitted file. See WaveState.kt.
 */
data class EliteAlert(
    val name: String,
)
