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

    /** Client→server event: the player clicked an ability slot to level it up (server-side upgrade). */
    const val EVENT_UPGRADE_ABILITY = "wd_upgrade_ability"

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

    // --- Enemy HP ----------------------------------------------------------
    // Regular creeps and elites scale per wave (bosses use bossHpForWave). The spawned lane creeps'
    // default HP (~550) made even wave 1 a slog for a level-1 hero, so we set HP explicitly: light at
    // wave 1, ramping up. Elites are tankier mini-threats.
    const val CREEP_BASE_HP = 40
    const val CREEP_HP_PER_WAVE = 12
    const val ELITE_HP_MULTIPLIER = 5

    fun creepHpForWave(wave: Int): Int = CREEP_BASE_HP + (wave - 1) * CREEP_HP_PER_WAVE

    fun eliteHpForWave(wave: Int): Int = creepHpForWave(wave) * ELITE_HP_MULTIPLIER

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
    // Each successive batch spawns one ring further out, so the outermost ring is
    // SPAWN_RADIUS + (SPAWN_BATCHES - 1) * SPAWN_RING_STEP. More batches at a slightly longer interval
    // make the wave pour in as a visible stream (rather than a couple of big clumps ~instantly), and a
    // bigger ring step widens the spread (min stays 1600, max ≈ 4750).
    const val SPAWN_RING_STEP = 450f
    const val SPAWN_BATCHES = 8
    const val SPAWN_BATCH_INTERVAL = 0.25f

    const val ENEMY_MELEE_UNIT = "npc_dota_creep_badguys_melee"
    const val ENEMY_RANGED_UNIT = "npc_dota_creep_badguys_ranged"

    /**
     * Each wave pours in from ONE cardinal direction — a 90° arc of the spawn ring. The index is the
     * quadrant (angle = index * 90°, measured from +X/east, CCW), and the name is used in the
     * "Wave N incoming from the <dir>" announcement.
     */
    val DIRECTION_NAMES = listOf("east", "north", "west", "south")

    // --- The Ancient (defended objective) ----------------------------------
    /**
     * Enemies march on the Ancient at the map centre and attack it when they arrive. If it is
     * destroyed the run ends — a second lose condition alongside the hero dying. It sits on the
     * player's team so the enemy creeps treat it as hostile.
     */
    const val ANCIENT_UNIT = "npc_dota_creep_goodguys_melee"
    const val ANCIENT_HP = 5000
    const val ANCIENT_MODEL_SCALE = 1.0f

    /**
     * The Ancient is spawned from a creep unit (reliably spawnable via CreateUnitByName) but re-skinned
     * with the Radiant ancient building model so it reads as the objective rather than a giant creep.
     */
    const val ANCIENT_MODEL = "models/props_structures/good_ancient001.vmdl"

    // --- Scoring -----------------------------------------------------------
    const val SCORE_PER_KILL = 10

    // --- "Nova" showcase ability ------------------------------------------
    const val NOVA_RADIUS = 450f
    const val NOVA_DAMAGE = 120f
    const val NOVA_PARTICLE = "particles/basic_explosion/basic_explosion.vpcf"

    /** Number of enemies spawned on a given (1-based) wave. */
    fun enemiesForWave(wave: Int): Int = FIRST_WAVE_SIZE + (wave - 1) * ENEMIES_ADDED_PER_WAVE
}
