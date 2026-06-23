package com.isycat.dotaaddon.shared

/**
 * Immutable snapshot of the match, produced on the Lua side every tick and
 * consumed verbatim by the Panorama HUD. As a `data class` in the shared module,
 * the transpiler emits a matching table (Lua) / object (JS), so the HUD reads
 * `state.wave`, `state.score`, ... with no manual parsing.
 *
 * Kept in its own file: ktox derives a class's Lua require path from its FQN
 * (`shared/WaveState`) but emits one Lua file per source file, so a class must
 * live alone in a file named after it to be require-able cross-module.
 */
data class WaveState(
    val wave: Int,
    val score: Int,
    val enemiesAlive: Int,
    val secondsToNext: Int,
    val heroHpPercent: Int,
    val gameOver: Boolean,
)
