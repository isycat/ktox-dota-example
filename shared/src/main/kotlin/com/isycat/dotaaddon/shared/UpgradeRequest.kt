package com.isycat.dotaaddon.shared

/**
 * Payload for [GameConfig.EVENT_UPGRADE_ABILITY] — the client→server "level up this ability" signal.
 * Carries the clicked ability's entity index. The upgrade is performed server-side (via
 * `hero:UpgradeAbility`) because a client `TRAIN_ABILITY` order is rejected for *hidden* abilities
 * such as the +stats attribute bonus ("ability is hidden").
 *
 * One declaration per file so its FQN-derived require path (`shared/UpgradeRequest`) matches an
 * emitted file. See WaveState.kt / RestartRequest.kt.
 */
data class UpgradeRequest(
    val abilityIndex: Int,
)
