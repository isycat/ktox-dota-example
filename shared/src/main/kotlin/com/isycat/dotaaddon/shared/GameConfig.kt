package com.isycat.dotaaddon.shared

import com.isycat.dotaaddon.shared.GameConfig.BOTTLE_CHARGES
import com.isycat.dotaaddon.shared.GameConfig.CLEARED_NEXT_WAVE_SECONDS
import com.isycat.dotaaddon.shared.GameConfig.SPAWN_BATCHES
import com.isycat.dotaaddon.shared.GameConfig.SPAWN_RADIUS
import com.isycat.dotaaddon.shared.GameConfig.WAVE_SCALE

/** Tunables shared by the Lua game logic and the Panorama HUD, so both agree on every rule. */
object GameConfig {
    // --- HUD timing --------------------------------------------------------

    const val ANNOUNCEMENT_SECONDS = 3.5f
    const val ELITE_POPUP_SECONDS = 4.0f

    // --- Difficulty scaling ------------------------------------------------

    /** Difficulty is driven off an *effective* wave that climbs at [WAVE_SCALE] the real rate (~3x slower). */
    const val WAVE_SCALE = 1.0f / 3.0f

    fun effectiveWave(wave: Int): Float = 1f + (wave - 1) * WAVE_SCALE

    // --- Elites ------------------------------------------------------------

    fun elitesForWave(wave: Int): Int {
        val ew = effectiveWave(wave)
        return if (ew < 3f) 0 else 1 + ((ew - 3f) / 2f).toInt()
    }

    // --- Bosses ------------------------------------------------------------

    /** Every Nth wave is a boss wave — first boss at wave 10, then 20, 30, … */
    const val BOSS_WAVE_INTERVAL = 10
    const val BOSS_BASE_HP = 3000
    const val BOSS_HP_PER_WAVE = 600

    /** How long a dead boss's body lies before removal (long enough for the death animation). */
    const val BOSS_CORPSE_SECONDS = 4f

    /** Within this range of the player's hero, a boss fights the PLAYER instead of marching. */
    const val BOSS_ENGAGE_RANGE = 900f

    /** How often a boss re-evaluates casting (each ability's own cooldown still gates it). */
    const val BOSS_CAST_INTERVAL_SECONDS = 3f

    /** The boss roster — real heroes spawned as bosses, cycled by boss wave (precached in addon_game_mode). */
    val BOSS_ROSTER =
        listOf(
            BossSpec("npc_dota_hero_tidehunter", "Leviathan, the Tidehunter"),
            BossSpec("npc_dota_hero_lina", "Lina, the Slayer"),
            BossSpec("npc_dota_hero_jakiro", "Jakiro, the Twin Dragon"),
            BossSpec("npc_dota_hero_lion", "Lion, the Demon Witch"),
        )

    fun isBossWave(wave: Int): Boolean = wave > 0 && wave % BOSS_WAVE_INTERVAL == 0

    /**
     * Level to force a boss hero to for its stat block — the FIRST boss (wave 10) is level 4, and each
     * later boss gains 3 levels (7, 10, …), capped so late bosses stay sane.
     */
    fun bossLevelForWave(wave: Int): Int = (1 + 3 * (wave / BOSS_WAVE_INTERVAL)).coerceIn(4, 25)

    /**
     * Boss model scale — SUBTLE growth with the boss's [level] from a modest base (was a flat, too-large
     * 2.0). A level-4 boss is ~1.16×, a level-25 boss ~1.4×.
     */
    fun bossScaleForLevel(level: Int): Float = 1.1f + level * 0.012f

    fun bossHpForWave(wave: Int): Int = BOSS_BASE_HP + (effectiveWave(wave) * BOSS_HP_PER_WAVE).toInt()

    // --- Enemy HP ----------------------------------------------------------
    // HP is set explicitly (the creeps' default ~550 makes wave 1 a slog for a level-1 hero).
    const val CREEP_BASE_HP = 25
    const val CREEP_HP_PER_WAVE = 12
    const val ELITE_HP_MULTIPLIER = 5

    fun creepHpForWave(wave: Int): Int =
        CREEP_BASE_HP + ((effectiveWave(wave) - 1f) * CREEP_HP_PER_WAVE).toInt()

    fun eliteHpForWave(wave: Int): Int = creepHpForWave(wave) * ELITE_HP_MULTIPLIER

    // --- Match flow --------------------------------------------------------

    const val START_DELAY_SECONDS = 10

    /** Stock default-announcer "prepare for battle" soundevent — no custom soundevents file needed. */
    const val PREPARE_MESSAGE = "Prepare for battle!"
    const val PREPARE_SOUND = "announcer_battle_prepare"

    /** Long between waves, but snapped down to [CLEARED_NEXT_WAVE_SECONDS] once the board is cleared. */
    const val WAVE_INTERVAL_SECONDS = 45
    const val CLEARED_NEXT_WAVE_SECONDS = 3
    const val THINK_INTERVAL_SECONDS = 1f

    const val STARTING_GOLD = 1000

    /** A carried Bottle is refilled to [BOTTLE_CHARGES] each wave. */
    const val BOTTLE_ITEM = "item_bottle"
    const val BOTTLE_CHARGES = 3

    /** While the run is over, the hero's respawn is pushed out this far so it can't come back. */
    const val GAMEOVER_RESPAWN_LOCK_SECONDS = 999999f

    const val HP_POLL_SECONDS = 0.1f

    // --- Wave composition --------------------------------------------------
    const val FIRST_WAVE_SIZE = 2
    const val ENEMIES_ADDED_PER_WAVE = 1

    /** Enemies spawn in rings from [SPAWN_RADIUS] outward, in [SPAWN_BATCHES] batches, and pour inward. */
    const val SPAWN_RADIUS = 2400f

    /** Arena-wide home-shop radius so buying/selling works anywhere. */
    const val SHOP_RADIUS = 4000f

    const val SPAWN_RING_STEP = 500f
    const val SPAWN_BATCHES = 8
    const val SPAWN_BATCH_INTERVAL = 0.25f

    const val ENEMY_MELEE_UNIT = "npc_dota_creep_badguys_melee"
    const val ENEMY_RANGED_UNIT = "npc_dota_creep_badguys_ranged"

    /** Each wave pours in from one cardinal direction (index * 90° from east, CCW); shown in the announcement. */
    val DIRECTION_NAMES = listOf("east", "north", "west", "south")

    // --- The Ancient (defended objective) ----------------------------------

    /** Enemies march on the Ancient; if it dies the run ends. A goodguys creep re-skinned as the ancient building. */
    const val ANCIENT_UNIT = "npc_dota_creep_goodguys_melee"
    const val ANCIENT_HP = 5000
    const val ANCIENT_MODEL_SCALE = 1.0f
    const val ANCIENT_MODEL = "models/props_structures/good_ancient001.vmdl"

    // --- Scoring -----------------------------------------------------------
    const val SCORE_PER_KILL = 10

    /** Whirling Death particle (the @Dota2Class WhirlingDeath ability granted to the hero). */
    const val WHIRLING_DEATH_PARTICLE = "particles/units/heroes/hero_shredder/shredder_whirling_death.vpcf"

    fun enemiesForWave(wave: Int): Int =
        FIRST_WAVE_SIZE + ((effectiveWave(wave) - 1f) * ENEMIES_ADDED_PER_WAVE).toInt()
}
