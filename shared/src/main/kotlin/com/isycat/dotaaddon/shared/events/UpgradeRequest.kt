package com.isycat.dotaaddon.shared.events

/**
 * Payload for [WD_UPGRADE] — the client→server "level up this ability" signal.
 * Carries the ability's SLOT index, not an entity index: the server resolves the actual ability from
 * the requesting player's own hero at that slot, so a forged event can't target an arbitrary entity.
 * The upgrade is then performed and validated server-side (a client `TRAIN_ABILITY` order is also
 * rejected for hidden abilities like the +stats attribute bonus).
 *
 * One declaration per file so its FQN-derived require path (`shared/UpgradeRequest`) matches an
 * emitted file. See WaveState.kt / RestartRequest.kt.
 */
data class UpgradeRequest(
    val slot: Int,
)
