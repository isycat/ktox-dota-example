package com.isycat.dotaaddon.panorama

import com.isycat.dota.types.panorama.DotaDefaultUIElement
import com.isycat.dota.types.panorama.GameEvents
import com.isycat.dota.types.panorama.GameUI
import com.isycat.dota.types.panorama.Label
import com.isycat.dota.types.panorama.ProgressBar
import com.isycat.dota.types.panorama.panorama
import com.isycat.dotaaddon.shared.Announcement
import com.isycat.dotaaddon.shared.GameConfig
import com.isycat.dotaaddon.shared.WaveState
import com.isycat.ktox.dota.lib.panorama.get

/**
 * Panorama HUD controller, transpiled from Kotlin to Panorama JavaScript.
 *
 * [gameHudInit] is the entrypoint invoked by the HUD root panel's `onload`
 * (see game_hud.dota.xml.kts). It trims the default Dota UI and subscribes to
 * the two custom game events the Lua side broadcasts, then mirrors that state
 * into the HUD labels. The [WaveState] / [Announcement] payload types come from
 * the shared module, so server and HUD share one definition.
 */
fun gameHudInit() {
    panorama.msg("[GameHud] init")
    trimDefaultUi()

    GameEvents.subscribe(GameConfig.EVENT_STATE) { event -> onState(event) }
    GameEvents.subscribe(GameConfig.EVENT_MESSAGE) { event -> onMessage(event) }
}

/** Hide a few default HUD elements we replace with our own overlay. */
private fun trimDefaultUi() {
    GameUI.setDefaultUIEnabled(DotaDefaultUIElement.TOP_HEROES.value, false)
    GameUI.setDefaultUIEnabled(DotaDefaultUIElement.FLYOUT_SCOREBOARD.value, false)
}

private fun onState(event: Any) {
    val state = event as WaveState
    setLabel("WdWaveValue", "${state.wave}")
    setLabel("WdScoreValue", "${state.score}")
    setLabel("WdEnemiesValue", "${state.enemiesAlive}")
    setLabel("WdNextValue", if (state.gameOver) "--" else "${state.secondsToNext}s")
    setLabel("WdHpLabel", "${state.heroHpPercent}%")

    val hpBar = panorama["WdHpProgress"]
    if (hpBar != null) (hpBar as ProgressBar).value = state.heroHpPercent

    if (state.gameOver) setLabel("WdAnnouncement", "GAME OVER")
}

private fun onMessage(event: Any) {
    val message = event as Announcement
    setLabel("WdAnnouncement", message.text)
}

/** Find a Label by id in the HUD context and set its text. */
private fun setLabel(id: String, value: String) {
    val panel = panorama[id]
    if (panel != null) (panel as Label).text = value
}
