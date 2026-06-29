package com.isycat.dotaaddon.panorama.panels
import com.isycat.dotaaddon.panorama.HudConfig
import com.isycat.dotaaddon.panorama.views.ItemSlotView

import com.isycat.dota.types.panorama.Entities
import com.isycat.dota.types.panorama.Panel
import com.isycat.dota.types.panorama.Players
import com.isycat.dota.types.panorama.panorama
import com.isycat.ktox.panorama.dsl.PanoramaView

/**
 * Custom inventory bar that replaces the stock inventory panel (hidden by [Manifest]).
 *
 * It mirrors the local hero's real inventory client-side: a 3×2 grid of the six carried slots plus a
 * backpack row of three, each a live, snippet-backed [ItemSlotView] bound to a fixed inventory slot.
 * Unlike [AbilitiesPanel] (which rebuilds when the kit changes), the slot set never changes — the
 * views are created once and just [ItemSlotView.refresh]ed every tick against [Entities.getItemInSlot],
 * so icons, cooldown sweeps and charge counts track the engine's own inventory with no parallel state.
 *
 * The single declarative [ItemSlotView] in `layout {}` exists only to register the snippet definition
 * (a `snippet = true` view emits nothing inline); the real slots are created in [build].
 */
@PanoramaView(snippet = false)
// hittest=false: this is a display container; only the item icons and the per-slot drop targets need to
// receive mouse events (they carry their own hittest=true and are hit-tested independently). Leaving the
// container hit-testable would needlessly capture mouse moves over its whole area.
class ItemsPanel : Panel(id = "WdInventory", type = "Panel", hittest = false) {
    lateinit var inventoryGrid: Panel
        private set
    lateinit var backpackRow: Panel
        private set

    /** The nine live slot views (0-5 inventory, 6-8 backpack); refreshed every tick. */
    private val slots = mutableListOf<ItemSlotView>()

    init {
        layout {
            Panel(id = "WdItemGrid", classes = "WdItemGrid") {
                // Declarative only: registers the <snippet name="ItemSlotView">; emits nothing inline.
                // Real slots are created live in build() via ItemSlotView(parent).
                ItemSlotView()
            } bind ::inventoryGrid
            Panel(id = "WdBackpackRow", classes = "WdBackpackRow") bind ::backpackRow
        }
    }

    override fun onLoad() {
        build()
        refresh()
    }

    /** Create the fixed slot views once: inventory slots 0-5 in two rows of three, backpack 6-8. */
    private fun build() {
        for (row in 0 until 2) {
            val rowPanel = panorama.createPanel("Panel", inventoryGrid, "")
            rowPanel.addClass("WdItemRow")
            for (col in 0 until 3) {
                val view = ItemSlotView(rowPanel)
                view.bind(row * 3 + col)
                slots.add(view)
            }
        }
        for (i in 0 until 3) {
            val view = ItemSlotView(backpackRow)
            view.bind(6 + i)
            view.addClass("WdBackpackSlot")
            slots.add(view)
        }
    }

    private fun refresh() {
        val hero = Players.getLocalPlayerPortraitUnit()
        if (Entities.isValidEntity(hero)) {
            for (slot in slots) {
                slot.refresh(hero)
            }
        }
        panorama.schedule(HudConfig.REFRESH_SECONDS) { refresh() }
    }
}
