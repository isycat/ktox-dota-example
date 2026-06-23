package com.isycat.dotaaddon.shared

/**
 * Tunables and identifiers shared between the Lua game-logic module and the
 * Panorama UI module.
 *
 * Living in `:shared`, every constant here is written once and consumed from
 * both transpile targets (Lua and JS), so the server and the HUD can never
 * disagree about an event name, a net-table key, or a scoring rule.
 */
object GameConfig {
    // --- Lua <-> Panorama bridge identifiers -------------------------------
    /** Custom game event carrying the full [WaveState] snapshot to all clients. */
    const val EVENT_STATE = "wd_state"

    /** Custom game event carrying a transient announcement string. */
    const val EVENT_MESSAGE = "wd_message"

    // --- Match flow --------------------------------------------------------
    const val START_DELAY_SECONDS = 5
    const val WAVE_INTERVAL_SECONDS = 20
    const val THINK_INTERVAL_SECONDS = 1f

    // --- Wave composition --------------------------------------------------
    const val FIRST_WAVE_SIZE = 4
    const val ENEMIES_ADDED_PER_WAVE = 2
    const val SPAWN_RADIUS = 700f
    const val ENEMY_MELEE_UNIT = "npc_dota_creep_badguys_melee"
    const val ENEMY_RANGED_UNIT = "npc_dota_creep_badguys_ranged"

    // --- Scoring -----------------------------------------------------------
    const val SCORE_PER_KILL = 10

    // --- "Nova" showcase ability ------------------------------------------
    const val NOVA_RADIUS = 450f
    const val NOVA_DAMAGE = 120f
    const val NOVA_PARTICLE = "particles/basic_explosion/basic_explosion.vpcf"

    /** Number of enemies spawned on a given (1-based) wave. */
    fun enemiesForWave(wave: Int): Int =
        GameConfig.FIRST_WAVE_SIZE + (wave - 1) * GameConfig.ENEMIES_ADDED_PER_WAVE
}
