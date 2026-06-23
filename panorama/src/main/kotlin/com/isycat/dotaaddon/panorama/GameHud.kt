package com.isycat.dotaaddon.panorama

import com.isycat.dota.types.panorama.DotaDefaultUIElement
import com.isycat.dota.types.panorama.GameEvents
import com.isycat.dota.types.panorama.GameUI
import com.isycat.dota.types.panorama.Label
import com.isycat.dota.types.panorama.panorama
import com.isycat.dotaaddon.shared.Announcement
import com.isycat.dotaaddon.shared.GameConfig
import com.isycat.dotaaddon.shared.WaveState
import com.isycat.ktox.dota.lib.panorama.get

/**
 * Panorama HUD controller, transpiled from Kotlin to Panorama JavaScript.
 *
 * [main] is the entry point: ktox auto-invokes a top-level `fun main()` when the script loads
 * (the layout root panel's `onload` does NOT fire in Panorama, so this is how Dota addons run
 * init — from a top-level script). It trims the default Dota UI and subscribes to the two custom
 * game events the Lua side broadcasts; [onState] then mirrors that state into the HUD labels (it
 * runs later, on each event, when the panels are fully loaded). The [WaveState] / [Announcement]
 * payload types come from the shared module, so server and HUD share one definition.
 */
fun main() {
    panorama.msg("[GameHud] init")
    trimDefaultUi()

    GameEvents.subscribe(GameConfig.EVENT_STATE) { event -> onState(event) }
    GameEvents.subscribe(GameConfig.EVENT_MESSAGE) { event -> onMessage(event) }
}

/**
 * Disable the entire default Dota HUD, keeping only the top-right menu button
 * ([DotaDefaultUIElement.TOP_MENU_BUTTONS]) so the player can still pause/quit.
 * This gives the custom Wave Defense overlay a clean canvas — same approach as
 * the "pocket" addon. Each `DotaDefaultUIElement.X.value` lowers to its bare
 * Panorama global, so the list is just the element ids passed to SetDefaultUIEnabled.
 */
private fun trimDefaultUi() {
    listOf(
        DotaDefaultUIElement.TOP_TIMEOFDAY.value,
        DotaDefaultUIElement.TOP_HEROES.value,
        DotaDefaultUIElement.FLYOUT_SCOREBOARD.value,
        DotaDefaultUIElement.ACTION_PANEL.value,
        DotaDefaultUIElement.ACTION_MINIMAP.value,
        DotaDefaultUIElement.INVENTORY_PANEL.value,
        DotaDefaultUIElement.INVENTORY_SHOP.value,
        DotaDefaultUIElement.INVENTORY_ITEMS.value,
        DotaDefaultUIElement.INVENTORY_QUICKBUY.value,
        DotaDefaultUIElement.INVENTORY_COURIER.value,
        DotaDefaultUIElement.INVENTORY_PROTECT.value,
        DotaDefaultUIElement.INVENTORY_GOLD.value,
        DotaDefaultUIElement.SHOP_SUGGESTEDITEMS.value,
        DotaDefaultUIElement.SHOP_COMMONITEMS.value,
        DotaDefaultUIElement.HERO_SELECTION_TEAMS.value,
        DotaDefaultUIElement.HERO_SELECTION_GAME_NAME.value,
        DotaDefaultUIElement.HERO_SELECTION_CLOCK.value,
        DotaDefaultUIElement.HERO_SELECTION_HEADER.value,
        DotaDefaultUIElement.TOP_BAR_BACKGROUND.value,
        DotaDefaultUIElement.TOP_BAR_RADIANT_TEAM.value,
        DotaDefaultUIElement.TOP_BAR_DIRE_TEAM.value,
        DotaDefaultUIElement.TOP_BAR_SCORE.value,
        DotaDefaultUIElement.ENDGAME.value,
        DotaDefaultUIElement.ENDGAME_CHAT.value,
        DotaDefaultUIElement.QUICK_STATS.value,
        DotaDefaultUIElement.PREGAME_STRATEGYUI.value,
        DotaDefaultUIElement.KILLCAM.value,
        DotaDefaultUIElement.FIGHT_RECAP.value,
        DotaDefaultUIElement.TOP_BAR.value,
        DotaDefaultUIElement.CUSTOMUI_BEHIND_HUD_ELEMENTS.value,
        DotaDefaultUIElement.AGHANIMS_STATUS.value,
    ).forEach { GameUI.setDefaultUIEnabled(it, false) }
}

private fun onState(event: Any) {
    val state = event as WaveState

    // Class @PanoramaView (snippet): the stat strip is emitted by the DSL and bootstrapped on
    // load; resolve the bootstrapped panel and write through its cached, bound label properties.
    val stats = panorama["WdTopBar"] as WaveStatsView
    stats.waveValue.text = "${state.wave}"
    stats.scoreValue.text = "${state.score}"
    stats.enemiesValue.text = "${state.enemiesAlive}"
    stats.nextValue.text = if (state.gameOver) "--" else "${state.secondsToNext}s"

    // Object @PanoramaView (singleton, non-snippet): the HP bar resolves itself lazily on first
    // access — just write through its typed selector getters.
    HeroHpView.hpLabel.text = "${state.heroHpPercent}%"
    HeroHpView.hpProgress.value = state.heroHpPercent

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
