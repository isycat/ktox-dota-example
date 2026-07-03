package com.isycat.dotaaddon.panorama.inventory

/**
 * The inventory bar's CSS class names (defined in `_inventory.scss`), named once so runtime
 * add/removeClass logic never carries string literals. Layout-DSL `classes =` attributes keep
 * their literals — they are the structural definition the snippet XML is compiled from.
 */
object InventoryStyles {
    /** The live root of one item slot. */
    const val SLOT = "WdItemSlot"

    /** Marks a slot with nothing in it (hidden icon, dimmed frame). */
    const val SLOT_EMPTY = "WdItemSlotEmpty"

    /** The three backpack slots' size/dim treatment. */
    const val BACKPACK_SLOT = "WdBackpackSlot"

    /** One row of three inventory slots. */
    const val ROW = "WdItemRow"

    /** The floating icon that follows the cursor during a drag. */
    const val DRAG_IMAGE = "WdItemDragImage"

    /** Full-screen scrim behind the right-click menu (click to dismiss). */
    const val MENU_SCRIM = "WdItemMenuScrim"

    /** The right-click confirm menu box. */
    const val MENU = "WdItemMenu"

    /** One clickable row in the right-click menu. */
    const val MENU_ROW = "WdItemMenuRow"
}
