package com.isycat.dotaaddon.shared.events

import com.isycat.dota.types.CustomGameEventKey
import com.isycat.ktox.annotations.ReplaceReferencesWithLiteral
import com.isycat.ktox.annotations.externalSource

/**
 * The custom game events for Wave Defense — declared ONCE here in `:shared` as typed [CustomGameEventKey]s,
 * so the server (Lua) and the HUD (Panorama) share one source of truth for both the event NAME and its
 * payload TYPE. `@ReplaceReferencesWithLiteral` lowers each reference to its event-name string, so
 * `sendServerToAllClients(WD_STATE, state)` / `subscribe(WD_STATE) { … }` map to the same native calls as a
 * raw string would — but the body type can never drift from the key.
 *
 * Server→client: [WD_STATE], [WD_MESSAGE], [WD_ELITE]. Client→server: [WD_RESTART], [WD_UPGRADE], [WD_SWAP].
 */
@ReplaceReferencesWithLiteral("wd_state")
val WD_STATE: CustomGameEventKey<WaveState> = externalSource()

@ReplaceReferencesWithLiteral("wd_message")
val WD_MESSAGE: CustomGameEventKey<Announcement> = externalSource()

@ReplaceReferencesWithLiteral("wd_elite")
val WD_ELITE: CustomGameEventKey<EliteAlert> = externalSource()

@ReplaceReferencesWithLiteral("wd_restart")
val WD_RESTART: CustomGameEventKey<RestartRequest> = externalSource()

@ReplaceReferencesWithLiteral("wd_upgrade_ability")
val WD_UPGRADE: CustomGameEventKey<UpgradeRequest> = externalSource()

@ReplaceReferencesWithLiteral("wd_swap_items")
val WD_SWAP: CustomGameEventKey<SwapItemsRequest> = externalSource()
