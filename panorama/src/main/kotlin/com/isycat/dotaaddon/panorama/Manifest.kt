package com.isycat.dotaaddon.panorama

import com.isycat.dota.types.panorama.DOTAGameState
import com.isycat.dota.types.panorama.DotaDefaultUIElement
import com.isycat.dota.types.panorama.Game
import com.isycat.dota.types.panorama.GameUI
import com.isycat.dota.types.panorama.panorama
import com.isycat.dota.types.panorama.Panel
import com.isycat.dotaaddon.shared.GameConfig

/**
 * Manifest controller — global, context-independent UI setup.
 *
 * `custom_ui_manifest.xml` is the file Dota reads first; its panel invokes [init] from `onload`.
 * This is the one place genuinely global setup runs (disabling the stock Dota HUD) — per-HUD
 * behaviour stays in the individual HUD views (e.g. [WaveStatsPanel]). An `object` so its entry is
 * namespaced (`Manifest.init`) under the auto-included script set.
 */
object Manifest {
    fun init() {
        panorama.msg("[Manifest] init")
        trimWhenInGame()
    }

    /**
     * The manifest UI is alive from the very first frame — including hero selection / strategy
     * time — so trimming the default UI immediately would tear down the draft screen and strand
     * the player (no pick buttons). Defer the trim until the game has actually started
     * ([DOTAGameState.PRE_GAME] or later),
     * polling once a second via [com.isycat.dota.types.panorama.DollarStatic.schedule].
     */
    private fun trimWhenInGame() {
        if (Game.state.toInt() >= DOTAGameState.PRE_GAME.value) {
            panorama.msg("[Manifest] in-game — trimming default UI")
            trimDefaultUi()
            hideTormentorButton()
        } else {
            panorama.schedule(1.0f) { trimWhenInGame() }
        }
    }

    /**
     * The tormentor button/timer (`TormentorTimerContainer`, inside the stock `minimap_container`)
     * is not a [DotaDefaultUIElement], so `SetDefaultUIEnabled` cannot hide it. Reach it the only
     * way available to a custom UI: walk up from this script's context panel to the shared HUD
     * root, then `FindChildTraverse` for it by id and collapse it.
     */
    private fun hideTormentorButton() {
        var root: Panel = panorama.getContextPanel()
        var parent = root.getParent()
        while (parent != null) {
            root = parent
            parent = root.getParent()
        }
        val tormentor = root.findChildTraverse("TormentorTimerContainer")
        if (tormentor != null) {
            tormentor.visible = false
            panorama.msg("[Manifest] hid TormentorTimerContainer")
        } else {
            panorama.msg("[Manifest] TormentorTimerContainer NOT found from this context")
        }
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
            // The minimap is left ENABLED (commented out, not deleted) so the player keeps spatial
            // awareness during waves — it pairs with the custom inventory bar ([ItemsPanel]).
            // DotaDefaultUIElement.ACTION_MINIMAP.value,
            DotaDefaultUIElement.INVENTORY_PANEL.value,
            // Shop UI is left ENABLED so the player can still buy items in Wave Defense — these
            // trims are commented out (kept in code, not deleted) so it's clear what is being
            // intentionally NOT hidden.
            // DotaDefaultUIElement.INVENTORY_SHOP.value,
            DotaDefaultUIElement.INVENTORY_ITEMS.value,
            // DotaDefaultUIElement.INVENTORY_QUICKBUY.value,
            DotaDefaultUIElement.INVENTORY_COURIER.value,
            DotaDefaultUIElement.INVENTORY_PROTECT.value,
            // Gold is left ENABLED so the player can see their gold (commented out, not deleted).
            // DotaDefaultUIElement.INVENTORY_GOLD.value,
            // DotaDefaultUIElement.SHOP_SUGGESTEDITEMS.value,
            // DotaDefaultUIElement.SHOP_COMMONITEMS.value,
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
}
