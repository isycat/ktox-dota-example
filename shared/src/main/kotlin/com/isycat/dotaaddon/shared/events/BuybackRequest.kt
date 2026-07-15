package com.isycat.dotaaddon.shared.events

import com.isycat.dota.types.CustomGameEventKey
import com.isycat.ktox.annotations.ReplaceReferencesWithLiteral
import com.isycat.ktox.annotations.externalSource

@ReplaceReferencesWithLiteral("wd_buyback")
val WD_BUYBACK: CustomGameEventKey<BuybackRequest> = externalSource()

/**
 * The client→server "buy back now" signal from the death screen — no data (the server derives the cost
 * from the CURRENT wave via [com.isycat.dotaaddon.shared.GameConfig.buybackCostForWave] and validates
 * gold itself), but a typed payload keeps the event API uniform.
 */
class BuybackRequest
