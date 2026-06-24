package com.isycat.dotaaddon.panorama

import com.isycat.dota.types.PanoramaEventKey
import com.isycat.dotaaddon.shared.Announcement
import com.isycat.dotaaddon.shared.EliteAlert
import com.isycat.dotaaddon.shared.WaveState
import com.isycat.ktox.annotations.ReplaceReferencesWithLiteral
import com.isycat.ktox.annotations.externalSource

/**
 * Typed Panorama event keys for the Wave Defense custom events.
 *
 * Declaring a key binds an event **name** to its **payload type**, so
 * `GameEvents.subscribe(WD_STATE) { … }` hands the callback a typed [WaveState] with no cast — the
 * preferred, type-safe way to consume events. This mirrors exactly how the built-in keys in
 * `com.isycat.dota.types.panorama` are declared: `@ReplaceReferencesWithLiteral` lowers each
 * reference to the event-name string, so a typed subscribe maps to the same native
 * `GameEvents.Subscribe(name, callback)` as a raw-string subscribe.
 *
 * The literal names mirror the server's `GameConfig.EVENT_*` strings (the Lua side sends by name).
 */
@ReplaceReferencesWithLiteral("wd_state")
val WD_STATE: PanoramaEventKey<WaveState> = externalSource()

@ReplaceReferencesWithLiteral("wd_message")
val WD_MESSAGE: PanoramaEventKey<Announcement> = externalSource()

@ReplaceReferencesWithLiteral("wd_elite")
val WD_ELITE: PanoramaEventKey<EliteAlert> = externalSource()
