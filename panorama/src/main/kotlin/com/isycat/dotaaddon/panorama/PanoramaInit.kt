package com.isycat.dotaaddon.panorama

import com.isycat.dota.types.panorama.Label
import com.isycat.dota.types.panorama.panorama
import com.isycat.dotaaddon.shared.AddonInfo
import com.isycat.ktox.dota.lib.panorama.get
import com.isycat.ktox.dota.lib.panorama.invoke

fun panoramaInit() {
    // AddonInfo.js is bundled from the :shared module and deployed alongside
    // this script. In Dota 2 Panorama (no-module-system environment) it must
    // be loaded before this script via the panel's XML <scripts> element.
    println(AddonInfo.getWelcomeMessage())
    val mainPanel = panorama["MainUI"] ?: throw Exception("MainUI panel not found")
    mainPanel(".AddonNameLabel").forEach {
        (it as Label).text = AddonInfo.NAME
    }
    sequenceOf(1,2,3).forEach { println(it) }
    sequenceOf(4,2,3).forEach { println(it) }
}
