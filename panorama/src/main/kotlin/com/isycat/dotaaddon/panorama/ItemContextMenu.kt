package com.isycat.dotaaddon.panorama

import com.isycat.dota.types.EntityIndex
import com.isycat.dota.types.panorama.Label
import com.isycat.dota.types.panorama.Panel
import com.isycat.dota.types.panorama.panorama
import com.isycat.ktox.panorama.dsl.ON_ACTIVATE

/**
 * Right-click context menu for an inventory item — a small popup ANCHORED to the clicked slot (it's
 * parented to that slot panel, so it sits right by the icon), the way a real context menu works: not a
 * centred modal, not at the raw cursor. Replaces the old instant-sell footgun (a stray right-click used
 * to sell outright) with a confirm step.
 *
 * Built fresh under the slot on each open and deleted on dismiss. Today it's a plain vertical list of
 * action rows (Sell / Cancel) with a hover highlight — like Dota's own item menu; adding a row is a
 * one-liner.
 */
object ItemContextMenu {
    /** The live menu panel (a child of the clicked slot), or null when closed. */
    private var menu: Panel? = null

    /** The item the open menu acts on. */
    private var targetItem: EntityIndex? = null

    /** Open the menu for [item], anchored to [anchor] (the right-clicked slot panel). */
    fun open(
        item: EntityIndex,
        anchor: Panel,
    ) {
        close()
        targetItem = item
        val box = panorama.createPanel("Panel", anchor, "")
        box.addClass("WdItemMenu")
        box.hittest = true
        addRow(box, "Sell") {
            targetItem?.let { ItemUse.sell(it) }
            close()
        }
        addRow(box, "Cancel") { close() }
        menu = box
    }

    /** Dismiss + delete the menu. */
    private fun close() {
        menu?.deleteAsync(0f)
        menu = null
        targetItem = null
    }

    private fun addRow(
        parent: Panel,
        label: String,
        onClick: () -> Unit,
    ) {
        val row = panorama.createPanel("Label", parent, "") as Label
        row.addClass("WdItemMenuRow")
        row.text = label
        row.hittest = true
        row.setPanelEvent(ON_ACTIVATE) { onClick() }
    }
}
