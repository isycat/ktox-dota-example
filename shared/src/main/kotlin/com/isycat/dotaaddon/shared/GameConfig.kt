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

    /** Client→server event: the player clicked "Play Again" on the game-over screen. */
    const val EVENT_RESTART = "wd_restart"

    /** Server→client event: an elite enemy spawned — drives a transient pop-up in the HUD feed. */
    const val EVENT_ELITE = "wd_elite"

    // --- HUD timing --------------------------------------------------------

    /** Seconds a centre-screen announcement stays before it auto-clears. */
    const val ANNOUNCEMENT_SECONDS = 3.5f

    /** Seconds an elite-spawn pop-up lives before it disposes itself. */
    const val ELITE_POPUP_SECONDS = 4.0f

    // --- Elites ------------------------------------------------------------

    /** How many elite enemies accompany a given (1-based) wave. */
    fun elitesForWave(wave: Int): Int = if (wave < 3) 0 else 1 + (wave - 3) / 2

    /** Flavour names cycled through for spawned elites. */
    val ELITE_NAMES = listOf("Marauder", "Ravager", "Stormcaller", "Bonebreaker")

    // --- Bosses ------------------------------------------------------------

    /** Every Nth wave is a boss wave (drives the dedicated boss HP bar). */
    const val BOSS_WAVE_INTERVAL = 5
    const val BOSS_MODEL_SCALE = 3.0f
    const val BOSS_BASE_HP = 3000
    const val BOSS_HP_PER_WAVE = 600

    /** True if [wave] (1-based) is a boss wave. */
    fun isBossWave(wave: Int): Boolean = wave > 0 && wave % BOSS_WAVE_INTERVAL == 0

    fun bossHpForWave(wave: Int): Int = BOSS_BASE_HP + wave * BOSS_HP_PER_WAVE

    // --- Match flow --------------------------------------------------------
    const val START_DELAY_SECONDS = 5
    const val WAVE_INTERVAL_SECONDS = 20
    const val THINK_INTERVAL_SECONDS = 1f

    /** Gold every run begins with — applied to the first attempt and every restart alike. */
    const val STARTING_GOLD = 1000

    /** While the run is over, the hero's respawn is pushed this far out so it can't come back. */
    const val GAMEOVER_RESPAWN_LOCK_SECONDS = 999999f

    /** How often the HUD polls the local hero's health client-side (smooth, no server round-trip). */
    const val HP_POLL_SECONDS = 0.1f

    /** How often the abilities panel re-checks the hero for level/point changes (cheap; rebuilds only on change). */
    const val ABILITY_REFRESH_SECONDS = 0.3f

    // --- Wave composition --------------------------------------------------
    const val FIRST_WAVE_SIZE = 2
    const val ENEMIES_ADDED_PER_WAVE = 1

    /**
     * Enemies spawn in rings around the map centre, far out, and pour inward. The first ring sits at
     * [SPAWN_RADIUS]; each successive batch is [SPAWN_RING_STEP] further out. A wave's enemies are
     * split into [SPAWN_BATCHES] batches spawned [SPAWN_BATCH_INTERVAL]s apart (rapid succession) so
     * they arrive as a rolling tide rather than all at once.
     */
    const val SPAWN_RADIUS = 1600f
    const val SPAWN_RING_STEP = 300f
    const val SPAWN_BATCHES = 4
    const val SPAWN_BATCH_INTERVAL = 0.2f

    const val ENEMY_MELEE_UNIT = "npc_dota_creep_badguys_melee"
    const val ENEMY_RANGED_UNIT = "npc_dota_creep_badguys_ranged"

    // --- The Ancient (defended objective) ----------------------------------
    /**
     * Enemies march on the Ancient at the map centre and attack it when they arrive. If it is
     * destroyed the run ends — a second lose condition alongside the hero dying. It sits on the
     * player's team so the enemy creeps treat it as hostile.
     */
    const val ANCIENT_UNIT = "npc_dota_creep_goodguys_melee"
    const val ANCIENT_HP = 5000
    const val ANCIENT_MODEL_SCALE = 4.0f

    // --- Scoring -----------------------------------------------------------
    const val SCORE_PER_KILL = 10

    // --- "Nova" showcase ability ------------------------------------------
    const val NOVA_RADIUS = 450f
    const val NOVA_DAMAGE = 120f
    const val NOVA_PARTICLE = "particles/basic_explosion/basic_explosion.vpcf"

    /** Number of enemies spawned on a given (1-based) wave. */
    fun enemiesForWave(wave: Int): Int = FIRST_WAVE_SIZE + (wave - 1) * ENEMIES_ADDED_PER_WAVE
}
