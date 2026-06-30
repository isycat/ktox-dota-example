package com.isycat.dotaaddon.bosses

/**
 * One entry in the boss roster: the hero unit to spawn as a boss, the signature ability it repeatedly
 * casts at the player, how that ability is delivered ([BossCast]), and the name shown on the HUD boss
 * bar. WaveDefenseController cycles the roster by boss wave.
 */
data class BossSpec(
    val unitName: String,
    val abilityName: String,
    val cast: BossCast,
    val displayName: String,
)
