package com.isycat.dotaaddon.panorama

import com.isycat.dota.types.panorama.Label
import com.isycat.dota.types.panorama.panorama
import com.isycat.dotaaddon.shared.AddonInfo
import com.isycat.ktox.dota.lib.panorama.invoke

/**
 * Entry point for the plain-XML example HUD ([example_hud.xml]) — a small bottom-left credit overlay.
 * Demonstrates the alternative authoring style to [WaveStatsPanel]'s DSL: a hand-written Panorama XML
 * layout wired via `onload`, using the callable `$` selector (`panorama("#CreditLabel")` → `$("#…")`).
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
