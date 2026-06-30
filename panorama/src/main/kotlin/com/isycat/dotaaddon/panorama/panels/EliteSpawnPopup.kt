package com.isycat.dotaaddon.panorama.panels

import com.isycat.dota.types.panorama.Label
import com.isycat.dota.types.panorama.Panel
import com.isycat.ktox.panorama.dsl.PanoramaView

/**
 * Elite-spawn pop-up — the showcase of a **class** `@PanoramaView` created and disposed on the fly,
 * in multiples. Unlike the singleton HUD panels (which are placed once in the layout), this is
 * instantiated programmatically — `EliteSpawnPopup(feed)` — one per elite. The generated constructor
 * `$.CreatePanel`s it under [parent] and bootstraps that fresh element directly (no id lookup, so
 * many instances don't collide). [EliteFeedPanel] sets its text and calls `deleteAsync` to let it
 * auto-dispose. The view IS the label, so callers just write `popup.text`.
 */
@PanoramaView(snippet = false)
class EliteSpawnPopup(
    parent: Panel,
) : Label() {
    override fun onLoad() {
        // addClass is inherited from Panel (not declared here), so reference it through `this`.
        this.addClass("WdElitePopup")
    }
}
