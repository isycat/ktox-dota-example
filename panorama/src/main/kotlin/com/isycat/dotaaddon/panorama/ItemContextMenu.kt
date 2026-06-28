package com.isycat.dotaaddon.panorama

import com.isycat.dota.types.EntityIndex
import com.isycat.dota.types.panorama.Label
import com.isycat.dota.types.panorama.Panel
import com.isycat.dota.types.panorama.panorama
import com.isycat.ktox.panorama.dsl.ON_ACTIVATE

/**
 * Right-click context menu for an inventory item — the confirm step that replaces the old instant-sell
 * footgun (a stray right-click used to sell an item outright, no undo). Part of the reusable inventory
 * module (see HUD_MODULES.md): [ItemSlotView]'s `ON_CONTEXT_MENU` opens this instead of selling directly.
 *
 * Built lazily on first open — the same imperative `$.CreatePanel` pattern the drag image and elite
 * pop-ups use — so it needs no slot in the layout. A click-catching scrim (dismiss on an outside click)
 * holds a centred box of actions. Today that's just Sell / Keep; adding a row is a one-liner, which is
 * the point of having a real menu rather than a hard-wired action.
 */
object ItemContextMenu {
    /** The lazily-built root (the full-screen scrim); also our "is it built yet" flag. */
    private var scrim: Panel? = null

    /** The item the open menu acts on, or null when closed. */
    private var targetItem: EntityIndex? = null

    /** Open the menu for [item] (the right-clicked slot's current item). */
    fun open(item: EntityIndex) {
        ensureBuilt()
        targetItem = item
        scrim?.visible = true
    }

    private fun close() {
        scrim?.visible = false
        targetItem = null
    }

    private fun ensureBuilt() {
        if (scrim != null) return
        // Full-screen catcher: a click anywhere outside the box dismisses the menu. Parented to the
        // context panel (same as the drag image), so it overlays the whole HUD.
        val s = panorama.createPanel("Panel", panorama.getContextPanel(), "WdItemMenuScrim")
        s.addClass("WdItemMenuScrim")
        s.hittest = true
        s.setPanelEvent(ON_ACTIVATE) { close() }
        // The box swallows clicks (hittest) so clicking its background doesn't fall through to the scrim.
        val box = panorama.createPanel("Panel", s, "WdItemMenu")
        box.addClass("WdItemMenu")
        box.hittest = true
        val title = panorama.createPanel("Label", box, "") as Label
        title.addClass("WdItemMenuTitle")
        title.text = "Sell this item?"
        addButton(box, "WdItemMenuSell", "Sell") {
            val t = targetItem
            if (t != null) ItemUse.sell(t)
            close()
        }
        addButton(box, "WdItemMenuKeep", "Keep") { close() }
        s.visible = false
        scrim = s
    }

    private fun addButton(
        parent: Panel,
        variantClass: String,
        label: String,
        onClick: () -> Unit,
    ) {
        val button = panorama.createPanel("Label", parent, "") as Label
        button.addClass("WdItemMenuButton")
        button.addClass(variantClass)
        button.text = label
        button.hittest = true
        button.setPanelEvent(ON_ACTIVATE) { onClick() }
    }
}
