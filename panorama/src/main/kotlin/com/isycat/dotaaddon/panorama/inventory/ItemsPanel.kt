package com.isycat.dotaaddon.panorama.inventory
import com.isycat.dota.types.panorama.Entities
import com.isycat.dota.types.panorama.Panel
import com.isycat.dota.types.panorama.Players
import com.isycat.dota.types.panorama.panorama
import com.isycat.ktox.panorama.dsl.PanoramaView

/**
 * Custom inventory bar that replaces the stock inventory panel (hidden by [Manifest]). Mirrors the local
 * hero's real inventory: a 3×2 grid of carried slots plus a backpack row of three, each a snippet-backed
 * [ItemSlotView] bound to a fixed slot. The slot set never changes — views are created once and
 * [ItemSlotView.refresh]ed each tick, so icons/cooldowns/charges track the engine with no parallel state.
 */
// hittest=false: display container only. The icons and per-slot drop targets carry their own hittest.
@PanoramaView(snippet = false)
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
                // Only registers the snippet definition; real slots are created in build().
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
        panorama.schedule(InventoryConfig.REFRESH_SECONDS) { refresh() }
    }
}
