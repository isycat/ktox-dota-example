package com.isycat.dotaaddon.panorama

import com.isycat.dota.types.panorama.DOTAGameState
import com.isycat.dota.types.panorama.DotaDefaultUIElement
import com.isycat.dota.types.panorama.Game
import com.isycat.dota.types.panorama.GameUI
import com.isycat.dota.types.panorama.Panel
import com.isycat.dota.types.panorama.panorama

/**
 * Manifest controller — global UI setup. `custom_ui_manifest.xml` is read first and invokes [init] from
 * `onload`; this is where genuinely global setup runs (disabling the stock Dota HUD). Per-HUD behaviour
 * stays in the individual views (e.g. [WaveStatsPanel]).
 */
object Manifest {
    fun init() {
        panorama.msg("[Manifest] init")
        trimWhenInGame()
    }

    /**
     * The manifest UI is alive from the first frame (hero selection included), so trimming immediately
     * would strand the player on the draft screen. Defer the trim until [DOTAGameState.PRE_GAME] or later.
     */
    private fun trimWhenInGame() {
        if (Game.state.toInt() >= DOTAGameState.PRE_GAME.value) {
            panorama.msg("[Manifest] in-game — trimming default UI")
            trimDefaultUi()
            hideTormentorButton()
            hideMinimapClutter()
        } else {
            panorama.schedule(1.0f) { trimWhenInGame() }
        }
    }

    /**
     * The tormentor timer isn't a [DotaDefaultUIElement], so SetDefaultUIEnabled can't hide it. Walk up to
     * the shared HUD root and FindChildTraverse for `TormentorTimerContainer` by id.
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
     * Hide everything in the minimap area except the map. Structure is `minimap_container > minimap_block >
     * minimap`, so hide every child of `minimap_container` except `minimap_block` (stopping there, or we'd
     * hide the rest of the HUD). No-op if not found.
     */
    private fun hideMinimapClutter() {
        var root: Panel = panorama.getContextPanel()
        var parent = root.getParent()
        while (parent != null) {
            root = parent
            parent = root.getParent()
        }
        val map = root.findChildTraverse("minimap") ?: return
        val block = map.getParent() ?: return
        val container = block.getParent() ?: return
        val count = container.childCount
        for (i in 0 until count) {
            val child = container.getChild(i) ?: continue
            if (child != block) child.visible = false
        }
    }

    /**
     * Disable the default Dota HUD for a clean overlay canvas, keeping the top-right menu button (pause/quit),
     * minimap, shop and gold. Each commented-out entry marks something deliberately left enabled.
     */
    private fun trimDefaultUi() {
        listOf(
            DotaDefaultUIElement.TOP_TIMEOFDAY.value,
            DotaDefaultUIElement.TOP_HEROES.value,
            DotaDefaultUIElement.FLYOUT_SCOREBOARD.value,
            DotaDefaultUIElement.ACTION_PANEL.value,
            // ACTION_MINIMAP left enabled — spatial awareness during waves.
            // DotaDefaultUIElement.ACTION_MINIMAP.value,
            DotaDefaultUIElement.INVENTORY_PANEL.value,
            // INVENTORY_SHOP left enabled — the player still buys items.
            // DotaDefaultUIElement.INVENTORY_SHOP.value,
            DotaDefaultUIElement.INVENTORY_ITEMS.value,
            // DotaDefaultUIElement.INVENTORY_QUICKBUY.value,
            DotaDefaultUIElement.INVENTORY_COURIER.value,
            DotaDefaultUIElement.INVENTORY_PROTECT.value,
            // INVENTORY_GOLD left enabled — the player sees their gold.
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
