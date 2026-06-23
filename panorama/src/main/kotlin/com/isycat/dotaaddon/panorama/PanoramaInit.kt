package com.isycat.dotaaddon.panorama

import com.isycat.dota.types.panorama.Label
import com.isycat.dota.types.panorama.panorama
import com.isycat.dotaaddon.shared.AddonInfo
import com.isycat.ktox.dota.lib.panorama.get
import com.isycat.ktox.dota.lib.panorama.invoke

/**
 * Entry point for the plain-XML example HUD ([example_hud.xml]) — a small bottom-left
 * "addon name + version" credit overlay.
 *
 * The real HUD is authored with the Panorama Layout DSL + SCSS + a @PanoramaView (see
 * [GameHud] / [WaveStatsView]). This file demonstrates the *alternative* authoring style:
 * a hand-written Panorama XML layout backed by plain CSS, with logic in Kotlin transpiled
 * to Panorama JS and wired via the panel's `onload`. It also exercises the ktox-dota-lib
 * panorama extensions `get` (`$.FindChildInContext`) and `invoke` (find-by-selector).
 */
fun panoramaInit() {
    panorama.msg(AddonInfo.getWelcomeMessage())
    val panel = panorama["CreditPanel"] ?: throw Exception("CreditPanel not found")
    panel(".CreditLabel").forEach { (it as Label).text = "${AddonInfo.NAME} v${AddonInfo.VERSION}" }
}
