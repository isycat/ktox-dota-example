package com.isycat.dotaaddon.shared.events

import com.isycat.dota.types.CustomGameEventKey
import com.isycat.ktox.annotations.ReplaceReferencesWithLiteral
import com.isycat.ktox.annotations.externalSource

@ReplaceReferencesWithLiteral("wd_state")
val WD_STATE: CustomGameEventKey<WaveState> = externalSource()

/** Server→client per-tick match snapshot; the HUD reads its fields directly (no parsing). */
data class WaveState(
    val wave: Int,
    val score: Int,
    val enemiesAlive: Int,
    val secondsToNext: Int,
    val gameOver: Boolean,
    /** True once a run has begun; the HUD hides its stats strip until then. */
    val running: Boolean,
    // Boss bar (boss waves only); HP is server-computed so the client needs no entity handle.
    val bossActive: Boolean,
    val bossHpPercent: Int,
    val bossName: String,
)
