package com.isycat.dotaaddon.panorama.panels

import com.isycat.dota.types.panorama.Label
import com.isycat.dota.types.panorama.Panel
import com.isycat.ktox.panorama.dsl.PanoramaView

/**
 * Elite-spawn pop-up — a class [PanoramaView] created and disposed on the fly, one per elite (unlike the
 * singleton HUD panels placed once in the layout). `EliteSpawnPopup(feed)` `$.CreatePanel`s it under
 * [parent]; [EliteFeedPanel] sets its text and calls `deleteAsync`. The view IS the label, so callers
 * just write `popup.text`.
 */
@PanoramaView(snippet = false)
class EliteSpawnPopup(
    parent: Panel,
) : Label() {
    override fun onLoad() {
        this.addClass("WdElitePopup")
    }
}
