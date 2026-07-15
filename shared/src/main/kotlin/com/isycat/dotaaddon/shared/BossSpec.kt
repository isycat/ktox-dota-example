package com.isycat.dotaaddon.shared

/**
 * One boss: the hero unit to spawn and the localization token of the name shown on the HUD boss bar
 * (string in `resource/addon_english.txt`). WaveDefenseController spawns it as a full hero — all its
 * abilities are learned and cast (see WaveDefenseController.spawnBoss), so no per-ability config is
 * needed here. Lives in `shared` so the roster can sit in [GameConfig].
 */
data class BossSpec(
    val unitName: String,
    val nameToken: String,
)
