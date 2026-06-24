package com.isycat.dotaaddon.panorama

import com.isycat.dota.types.panorama.GameEvents
import com.isycat.dota.types.panorama.Panel
import com.isycat.dotaaddon.shared.EliteAlert
import com.isycat.dotaaddon.shared.GameConfig
import com.isycat.ktox.panorama.dsl.PanoramaView

/**
 * Container for elite-spawn pop-ups. A singleton HUD panel that, on each [GameConfig.EVENT_ELITE],
 * creates a fresh [EliteSpawnPopup] under itself, fills its text, and lets it auto-dispose after a
 * few seconds — so several elites produce several independent, self-destroying pop-ups at once.
 */
@PanoramaView(snippet = false)
class EliteFeedPanel : Panel(id = "WdEliteFeed", type = "Panel") {
    override fun onLoad() {
        GameEvents.subscribe(WD_ELITE) { onElite(it) }
    }

    private fun onElite(alert: EliteAlert) {
        // Live construction: one pop-up per elite, parented into this feed; deleteAsync disposes it.
        val popup = EliteSpawnPopup(this)
        popup.text = "ELITE: ${alert.name}"
        popup.deleteAsync(GameConfig.ELITE_POPUP_SECONDS)
    }
}
