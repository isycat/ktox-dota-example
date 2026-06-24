package com.isycat.dotaaddon.panorama

import com.isycat.dota.types.panorama.Label
import com.isycat.dota.types.panorama.panorama
import com.isycat.dotaaddon.shared.AddonInfo
import com.isycat.ktox.dota.lib.panorama.invoke

/**
 * Entry point for the plain-XML example HUD ([example_hud.xml]) — a small bottom-left
 * "addon name + version" credit overlay.
 *
 * The real HUD is authored with the Panorama Layout DSL + SCSS + a @PanoramaView (see
 * [WaveStatsPanel]). This file demonstrates the *alternative* authoring style: a hand-written
 * Panorama XML layout backed by plain CSS, with logic in Kotlin transpiled to Panorama JS and wired
 * via a panel's `onload`. Uses the callable `$` selector — `panorama("#CreditLabel")` lowers to
 * `$("#CreditLabel")` — to grab the label and fill it in.
 */
object PanoramaInit {
    fun init() {
        panorama.msg("[PanoramaInit] init")
        panorama.msg(AddonInfo.getWelcomeMessage())
        val label = panorama("#CreditLabel")
        if (label != null) {
            (label as Label).text = "${AddonInfo.NAME} v${AddonInfo.VERSION}"
        }
    }
}
