package com.isycat.dotaaddon.shared.events

import com.isycat.dota.types.CustomGameEventKey
import com.isycat.ktox.annotations.ReplaceReferencesWithLiteral
import com.isycat.ktox.annotations.externalSource

@ReplaceReferencesWithLiteral("wd_run_stats")
val WD_RUN_STATS: CustomGameEventKey<RunStats> = externalSource()

/**
 * Server→client end-of-run summary, sent ONCE when the run ends — the game-over screen renders it
 * directly (one typed event carries the whole scoreboard; no per-field messages, no parsing).
 */
data class RunStats(
    val wavesSurvived: Int,
    val score: Int,
    val kills: Int,
    val bossesSlain: Int,
    val goldEarned: Int,
    /** Wall-clock length of the run, in whole seconds (client formats mm:ss). */
    val runSeconds: Int,
)
