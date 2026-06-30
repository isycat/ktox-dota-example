package com.isycat.dotaaddon.shared.events

import com.isycat.dota.types.CustomGameEventKey
import com.isycat.ktox.annotations.ReplaceReferencesWithLiteral
import com.isycat.ktox.annotations.externalSource

@ReplaceReferencesWithLiteral("wd_state")
val WD_STATE: CustomGameEventKey<WaveState> = externalSource()

/**
 * Server→client per-tick snapshot of the match, produced on the Lua side and consumed verbatim by the
 * Panorama HUD. As a `data class` in the shared module, the transpiler emits a matching table (Lua) /
 * object (JS), so the HUD reads `state.wave`, `state.score`, ... with no manual parsing. Kept alone as the
 * file's one transpiled declaration so its FQN-derived Lua require path matches the emitted file (the
 * [WD_STATE] key is an external — it transpiles to nothing).
 */
data class WaveState(
    val wave: Int,
    val score: Int,
    val enemiesAlive: Int,
    val secondsToNext: Int,
    val gameOver: Boolean,
    // True once a run has actually begun (the pre-battle countdown for wave 1 is ticking). Before this
    // — during hero selection / pre-init — there is no real "next wave" value, so the HUD keeps the top
    // stats strip hidden until it flips true.
    val running: Boolean,
    // Boss bar (shown only on boss waves). HP is server-computed so the client needs no entity
    // handle; it just renders what it's told.
    val bossActive: Boolean,
    val bossHpPercent: Int,
    val bossName: String,
)
