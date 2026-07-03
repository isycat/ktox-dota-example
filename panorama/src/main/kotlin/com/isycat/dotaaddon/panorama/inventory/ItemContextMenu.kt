package com.isycat.dotaaddon.panorama.inventory

import com.isycat.dota.types.EntityIndex
import com.isycat.dota.types.panorama.Label
import com.isycat.dota.types.panorama.Panel
import com.isycat.dota.types.panorama.panorama
import com.isycat.ktox.panorama.dsl.ON_ACTIVATE
import com.isycat.ktox.panorama.dsl.ON_CONTEXT_MENU

/**
 * Right-click context menu for an inventory item — a small popup at the clicked icon, replacing the old
 * instant-sell footgun with a confirm step. Parented to the full-screen context panel (so it isn't
 * clipped by the slot) and positioned with setPositionInPixels. A transparent scrim dismisses on outside
 * click. Built fresh per open, deleted on dismiss.
 */
object ItemContextMenu {
    /** The transparent full-screen click-catcher (outside-click dismiss), or null when closed. */
    private var scrim: Panel? = null

    /** The live menu panel, or null when closed. */
    private var menu: Panel? = null

    /** The item the open menu acts on. */
    private var targetItem: EntityIndex? = null

    /** Open the menu for [item], positioned at [anchor] (the right-clicked slot panel). */
    fun open(
        item: EntityIndex,
        anchor: Panel,
    ) {
        close()
        targetItem = item
        val root = panorama.getContextPanel()

        // Transparent full-screen catcher, created first so it sits under the menu. Either button dismisses.
        val s = panorama.createPanel("Panel", root, "")
        s.addClass(InventoryStyles.MENU_SCRIM)
        s.hittest = true
        s.setPanelEvent(ON_ACTIVATE) { close() }
        s.setPanelEvent(ON_CONTEXT_MENU) { close() }
        scrim = s

        // The menu itself, created after the scrim so it's on top.
        val box = panorama.createPanel("Panel", root, "")
        box.addClass(InventoryStyles.MENU)
        box.hittest = true
        addRow(box, "Sell") {
            targetItem?.let { ItemUse.sell(it) }
            close()
        }
        addRow(box, "Cancel") { close() }

        // Move it to the icon. GetPositionWithinWindow is window pixels but SetPositionInPixels wants layout
        // pixels, so divide by the UI scale; sit it just above-left of the slot.
        val pos = anchor.getPositionWithinWindow()
        val x = (pos["x"] ?: 0) / (anchor.actualuiscale_x ?: 1f)
        val y = (pos["y"] ?: 0) / (anchor.actualuiscale_y ?: 1f)
        box.setPositionInPixels(x - 70f, y - 80f, 0f)
        menu = box
    }

    /** Dismiss + delete the menu and its scrim. */
    private fun close() {
        menu?.deleteAsync(0f)
        scrim?.deleteAsync(0f)
        menu = null
        scrim = null
        targetItem = null
    }

    private fun addRow(
        parent: Panel,
        label: String,
        onClick: () -> Unit,
    ) {
        val row = panorama.createPanel("Label", parent, "") as Label
        row.addClass(InventoryStyles.MENU_ROW)
        row.text = label
        row.hittest = true
        row.setPanelEvent(ON_ACTIVATE) { onClick() }
    }
}
