package com.isycat.dotaaddon.model

/**
 * One boss: the hero unit to spawn and the name shown on the HUD boss bar. WaveDefenseController spawns it
 * as a full hero — all its abilities are learned and cast (see WaveDefenseController.spawnBoss), so no
 * per-ability config is needed here.
 */
data class BossSpec(
    val unitName: String,
    val displayName: String,
)
