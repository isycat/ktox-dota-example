package com.isycat.dotaaddon.panorama

import com.isycat.dota.types.panorama.Label
import com.isycat.dota.types.panorama.panorama
import com.isycat.dotaaddon.shared.AddonInfo
import com.isycat.ktox.dota.lib.panorama.get

/**
 * Entry point for the plain-XML example HUD ([example_hud.xml]).
 *
 * This addon authors its real HUD with the Panorama Layout DSL + SCSS (see
 * [GameHud] / game_hud.dota.xml.kts / game_hud.scss). This file exists to show
 * the *alternative* authoring style — a hand-written Panorama XML layout backed
 * by plain CSS — with logic still written in Kotlin and transpiled to Panorama JS.
 *
 * Wired via the panel's `onload="panoramaInit()"` in example_hud.xml.
 */
fun panoramaInit() {
    panorama.msg(AddonInfo.getWelcomeMessage())
    val statusLabel = panorama["StatusLabel"] ?: throw Exception("StatusLabel not found")
    (statusLabel as Label).text = "${AddonInfo.NAME} v${AddonInfo.VERSION}"
}
