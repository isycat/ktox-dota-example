package com.isycat.dotaaddon.shared.events

import com.isycat.dota.types.CustomGameEventKey
import com.isycat.ktox.annotations.ReplaceReferencesWithLiteral
import com.isycat.ktox.annotations.externalSource

/**
 * Payload for [WD_UPGRADE] — the client→server "level up this ability" signal.
 * Carries the ability's SLOT index, not an entity index: the server resolves the actual ability from
 * the requesting player's own hero at that slot, so a forged event can't target an arbitrary entity.
 * The upgrade is then performed and validated server-side (a client `TRAIN_ABILITY` order is also
 * rejected for hidden abilities like the +stats attribute bonus).
 *
 * One transpiled declaration per file so its FQN-derived require path (`shared/events/UpgradeRequest`)
 * matches an emitted file. See WaveState.kt / RestartRequest.kt.
 */
data class UpgradeRequest(
    val slot: Int,
)

/** Client→server ability-upgrade request. Typed key — its generic fixes the [UpgradeRequest] payload. */
@ReplaceReferencesWithLiteral("wd_upgrade_ability")
val WD_UPGRADE: CustomGameEventKey<UpgradeRequest> = externalSource()
