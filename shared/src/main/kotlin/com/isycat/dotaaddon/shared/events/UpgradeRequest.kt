package com.isycat.dotaaddon.shared.events

import com.isycat.dota.types.CustomGameEventKey
import com.isycat.ktox.annotations.ReplaceReferencesWithLiteral
import com.isycat.ktox.annotations.externalSource

@ReplaceReferencesWithLiteral("wd_upgrade_ability")
val WD_UPGRADE: CustomGameEventKey<UpgradeRequest> = externalSource()

/**
 * Client→server "level up this ability" — carries the SLOT, not an entity index, so the server resolves
 * the ability off the requesting player's own hero (a forged event can't target something arbitrary).
 */
data class UpgradeRequest(
    val slot: Int,
)
