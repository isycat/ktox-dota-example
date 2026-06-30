package com.isycat.dotaaddon.shared.events

import com.isycat.dota.types.CustomGameEventKey
import com.isycat.ktox.annotations.ReplaceReferencesWithLiteral
import com.isycat.ktox.annotations.externalSource

@ReplaceReferencesWithLiteral("wd_upgrade_ability")
val WD_UPGRADE: CustomGameEventKey<UpgradeRequest> = externalSource()

/**
 * The client→server "level up this ability" signal. Carries the ability's SLOT index, not an entity index:
 * the server resolves the actual ability from the requesting player's own hero at that slot, so a forged
 * event can't target an arbitrary entity (the upgrade is then validated server-side). Kept alone as the
 * file's one transpiled declaration so its FQN-derived Lua require path matches the emitted file.
 */
data class UpgradeRequest(
    val slot: Int,
)
