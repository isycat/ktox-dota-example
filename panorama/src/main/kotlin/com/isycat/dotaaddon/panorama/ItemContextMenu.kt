package com.isycat.dotaaddon.panorama

import com.isycat.dota.types.EntityIndex
import com.isycat.dota.types.panorama.Label
import com.isycat.dota.types.panorama.Panel
import com.isycat.dota.types.panorama.panorama
import com.isycat.ktox.annotations.NativeName
import com.isycat.ktox.panorama.dsl.ON_ACTIVATE

/**
 * Right-click context menu for an inventory item — a small popup positioned right AT the clicked icon,
 * the way a real context menu works (not a centred modal, not the raw cursor, not squashed inside the
 * slot). Replaces the old instant-sell footgun with a confirm step.
 *
 * It's parented to the full-screen context panel (so it isn't clipped or squished by the tiny inventory
 * slot) and moved to the icon's location with [setPositionInPixels] — the standard Panorama popup
 * pattern. A transparent full-screen scrim underneath catches an outside click to dismiss. Built fresh
 * per open and deleted on dismiss; today it's a plain vertical list of action rows (Sell / Cancel).
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

        // Transparent full-screen catcher (created first, so it sits UNDER the menu): a click anywhere
        // outside the menu dismisses it.
        val s = panorama.createPanel("Panel", root, "")
        s.addClass("WdItemMenuScrim")
        s.hittest = true
        s.setPanelEvent(ON_ACTIVATE) { close() }
        scrim = s

        // The menu itself (created after the scrim, so it's on top + clickable).
        val box = panorama.createPanel("Panel", root, "")
        box.addClass("WdItemMenu")
        box.hittest = true
        addRow(box, "Sell") {
            targetItem?.let { ItemUse.sell(it) }
            close()
        }
        addRow(box, "Cancel") { close() }

        // Move it to the icon: GetPositionWithinWindow is in window pixels, SetPositionInPixels wants
        // layout pixels, so divide by the UI scale. Sit it just above-left of the slot (right-aligned).
        val pos = anchor.getPositionWithinWindow()
        val x = (pos["x"] ?: 0) / (anchor.actualuiscale_x ?: 1)
        val y = (pos["y"] ?: 0) / (anchor.actualuiscale_y ?: 1)
        box.setPositionInPixels(x - 70, y - 80, 0)
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
        row.addClass("WdItemMenuRow")
        row.text = label
        row.hittest = true
        row.setPanelEvent(ON_ACTIVATE) { onClick() }
    }
}

/**
 * Position a panel at ([x], [y], [z]) in layout pixels, relative to its parent — Panorama's
 * `Panel.SetPositionInPixels`, declared here as an `@NativeName` extension (it isn't in the bindings yet).
 */
@NativeName("SetPositionInPixels")
fun Panel.setPositionInPixels(
    x: Int,
    y: Int,
    z: Int,
) {}
