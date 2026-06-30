package com.isycat.dotaaddon.model

/**
 * How a boss hero's signature ability is delivered — drives which `CastAbility*` order
 * WaveDefenseController.bossCastThink issues. See [BossSpec].
 */
enum class BossCast {
    /** Fired with no target, on/around the boss itself (e.g. Tidehunter's Ravage). */
    NO_TARGET,

    /** Cast directly on the player hero as the unit target (e.g. Lina's Laguna Blade). */
    TARGET,

    /** Cast at the player hero's current position (e.g. Jakiro's Macropyre). */
    POSITION,
}
