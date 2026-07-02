package com.isycat.dotaaddon.panorama.inventory

import com.isycat.dota.types.EntityIndex
import com.isycat.dota.types.panorama.Abilities
import com.isycat.dota.types.panorama.AbilityEntityIndex
import com.isycat.dota.types.panorama.DOTAItemImage
import com.isycat.dota.types.panorama.DotaAbilityBehavior
import com.isycat.dota.types.panorama.Dotaunitorder
import com.isycat.dota.types.panorama.DragSettings
import com.isycat.dota.types.panorama.Entities
import com.isycat.dota.types.panorama.Game
import com.isycat.dota.types.panorama.GameEvents
import com.isycat.dota.types.panorama.Label
import com.isycat.dota.types.panorama.Panel
import com.isycat.dota.types.panorama.Players
import com.isycat.dota.types.panorama.PrepareUnitOrdersArgument
import com.isycat.dota.types.panorama.panorama
import com.isycat.dotaaddon.shared.events.SwapItemsRequest
import com.isycat.dotaaddon.shared.events.WD_SWAP
import com.isycat.ktox.panorama.dsl.ON_ACTIVATE
import com.isycat.ktox.panorama.dsl.ON_CONTEXT_MENU
import com.isycat.ktox.panorama.dsl.ON_MOUSE_OUT
import com.isycat.ktox.panorama.dsl.ON_MOUSE_OVER
import com.isycat.ktox.panorama.dsl.PanoramaView
import kotlin.math.ceil

/**
 * One inventory slot in the custom item bar — the snippet-backed [PanoramaView] counterpart of
 * [AbilitySlotView]. Unlike the abilities bar, slots are fixed: one view per slot, created once by
 * [ItemsPanel] and [refresh]ed each tick against the live inventory. Icon, cooldown, charges and
 * tooltip all come from the engine off the live item binding; clicking issues a genuine cast order.
 */
@PanoramaView
class ItemSlotView(
    parent: Panel? = null,
) : Panel(type = "Panel") {
    lateinit var icon: DOTAItemImage
        private set

    /** Dark radial-clip overlay drawn over the icon as the cooldown "spiral" (shared with abilities). */
    lateinit var cdSpiral: Panel
        private set
    lateinit var cooldown: Label
        private set
    lateinit var charges: Label
        private set

    /** Which inventory slot (0-5 inventory, 6-8 backpack) this view tracks; set once by [bind]. */
    private var slot = 0

    /** The item currently in [slot], or null when empty; drives the tooltip + click order. */
    private var item: EntityIndex? = null
    private var itemName = ""

    /** Entity index the icon is currently bound to (-1 = none); re-bind only when the slot's item changes. */
    private var boundEntIndex = -1

    /** Whether the cooldown-spiral overlay is currently shown (avoids redundant per-tick visibility writes). */
    private var cdVisible = false

    init {
        // A snippet must have exactly one panel child: the icon + its overlays live in one content panel.
        layout {
            Panel(classes = "WdSlotContent") {
                DOTAItemImage(id = "WdItemSlotIcon", classes = "WdItemIcon", hittest = true) {
                    Panel(id = "WdItemSlotCdSpiral", classes = "WdAbilityCdSpiral") bind ::cdSpiral
                    Label(id = "WdItemSlotCd", classes = "WdAbilityCooldown") bind ::cooldown
                    Label(id = "WdItemSlotCharges", classes = "WdAbilityCharges") bind ::charges
                } bind ::icon
            }
        }
    }

    /**
     * Bind this view to inventory [slot] (called once after creation): style the live root and wire the
     * tooltip + click handlers, which read the live [item] field that [refresh] keeps current.
     */
    fun bind(slot: Int) {
        this.slot = slot
        addClass("WdItemSlot")
        cooldown.hittest = false
        charges.hittest = false
        cdSpiral.hittest = false
        // Overlays default visible; hide them up front (the spiral is a dark full-size wash over the item).
        cdSpiral.visible = false
        cooldown.visible = false
        charges.visible = false
        // Seed the empty display so refresh() can skip per-tick DOM writes while the slot stays empty.
        icon.visible = false
        addClass("WdItemSlotEmpty")
        // The slot root is the drop target, so empty slots (hidden icon) still accept drops.
        hittest = true
        icon.setDisableFocusOnMouseDown(true)
        icon.draggable = true
        icon.setPanelEvent(ON_MOUSE_OVER) {
            val current = item
            if (current != null) {
                panorama.dispatchEvent("DOTAShowAbilityTooltipForEntityIndex", icon, itemName, current)
            }
        }
        icon.setPanelEvent(ON_MOUSE_OUT) {
            panorama.dispatchEvent("DOTAHideAbilityTooltip", icon)
        }
        // Left-click uses the item (a server-validated CAST order — the client only requests).
        icon.setPanelEvent(ON_ACTIVATE) {
            val current = item
            if (current != null) ItemUse.use(current)
        }
        // Right-click opens a confirm menu (anchored on this slot) rather than instant-selling.
        icon.setPanelEvent(ON_CONTEXT_MENU) {
            val current = item
            if (current != null) ItemContextMenu.open(current, this@ItemSlotView)
        }
        // Drag to rearrange. The unit-order API has no item-move, so the swap runs server-side ([WD_SWAP]).
        // DragDrop binds to the slot root so empty slots count; DragEnd drops on the ground if unconsumed.
        panorama.registerEventHandler(
            "DragStart",
            icon,
            fun(
                _: String,
                settings: DragSettings,
            ) {
                val current = item ?: return
                val dragImage = panorama.createPanel("DOTAItemImage", panorama.getContextPanel(), "")
                dragImage.addClass("WdItemDragImage")
                (dragImage as DOTAItemImage).itemname = itemName
                settings.displayPanel = dragImage
                settings.removePositionBeforeDrop = true
                ItemMove.begin(slot, current, dragImage)
            },
        )
        panorama.registerEventHandler("DragDrop", this) {
            ItemMove.drop(slot)
        }
        panorama.registerEventHandler("DragEnd", icon) {
            ItemMove.end()
        }
    }

    /** Re-read [slot] from the live inventory and update the icon, cooldown sweep and charge count. */
    fun refresh(hero: EntityIndex) {
        val raw = EntityIndex(Entities.getItemInSlot(hero, slot))
        if (!Entities.isValidEntity(raw)) {
            // Already empty? Display is correct — skip the per-tick DOM writes (most slots, most of the time).
            if (item == null) return
            // Empty slot: hide the icon. Don't null contextEntityIndex — the V8 binding rejects null (needs a Number).
            item = null
            itemName = ""
            boundEntIndex = -1
            icon.visible = false
            cooldown.visible = false
            charges.visible = false
            setCdStep(0f)
            addClass("WdItemSlotEmpty")
            return
        }
        removeClass("WdItemSlotEmpty")
        icon.visible = true
        item = raw
        // Bind the icon to the live item entity (contextEntityIndex): renders the current icon and reflects
        // in-place state (treads toggle, bottle charges) with no polling. Re-bind only when the item changes.
        if (raw.value != boundEntIndex) {
            boundEntIndex = raw.value
            itemName = Abilities.getAbilityName(raw)
            icon.itemname = itemName
            icon.contextEntityIndex = raw
        }
        refreshCooldown(raw)
    }

    /** Cooldown sweep + charge count for the item currently in this slot. */
    private fun refreshCooldown(current: EntityIndex) {
        val chargeCount = Abilities.getCurrentCharges(current).toInt()
        if (chargeCount > 0) {
            charges.text = "$chargeCount"
            charges.visible = true
        } else {
            charges.visible = false
        }
        val remaining = Abilities.getCooldownTimeRemaining(current)
        if (remaining > 0.05f) {
            cooldown.text = formatCd(remaining)
            cooldown.visible = true
            val length = Abilities.getCooldownLength(current).toFloat()
            setCdStep(if (length > 0f) remaining / length else 1f)
        } else {
            cooldown.visible = false
            setCdStep(0f)
        }
    }

    /** 1 decimal under 5s (where the fraction matters), whole seconds above — same as [AbilitySlotView]. */
    private fun formatCd(remaining: Float): String =
        if (remaining >= 5f) {
            "${ceil(remaining).toInt()}"
        } else {
            val whole = remaining.toInt()
            val tenth = ((remaining - whole) * 10f).toInt()
            "$whole.$tenth"
        }

    /**
     * Drives the cooldown spiral CONTINUOUSLY from the remaining [fraction] (0 = ready → hidden) — an inline
     * `panel.style.clip = radial(…)` each tick (pocket's technique) sweeps smoothly instead of stepping.
     */
    private fun setCdStep(fraction: Float) {
        if (fraction <= 0f) {
            if (cdVisible) {
                cdSpiral.visible = false
                cdVisible = false
            }
            return
        }
        if (!cdVisible) {
            cdSpiral.visible = true
            cdVisible = true
        }
        val deg = ceil(fraction * 360f).toInt().coerceIn(1, 360)
        cdSpiral.styleClip = "radial(50% 50%, ${360 - deg}deg, ${deg}deg)"
    }
}

/**
 * Item activation for the inventory bar. [use] issues the engine's own order (server-validated, like the
 * stock HUD). Toggle items (e.g. armlet) get CAST_TOGGLE; everything else CAST_NO_TARGET — the toggle
 * test reads the behavior bitmask with Kotlin `and` on an Int (→ JS `&`).
 */
object ItemUse {
    /**
     * Activates [item] exactly like clicking it in the stock HUD: [Abilities.executeAbility] handles
     * EVERY behavior uniformly — no-target casts fire, toggles toggle, and TARGETED items (Hand of
     * Midas etc.) enter targeting mode. The previous hand-rolled CAST_NO_TARGET order silently
     * no-oped for targeted items, which read as "left-click does nothing".
     */
    fun use(item: EntityIndex) {
        val hero = Players.getLocalPlayerPortraitUnit()
        Abilities.executeAbility(AbilityEntityIndex(item.value), hero, false)
    }

    /** Sell [item] via the engine's SELL_ITEM order. Works anywhere — the arena is a shop (WaveDefenseController). */
    fun sell(item: EntityIndex) {
        Game.prepareUnitOrders(
            object : PrepareUnitOrdersArgument {
                override var orderType = Dotaunitorder.SELL_ITEM.value
                override var abilityIndex: EntityIndex? = item
                override var targetIndex: EntityIndex? = null
                override var position: List<Float>? = null
                override var queue: Boolean? = false
                override var showEffects: Boolean? = false
            },
        )
    }
}

/**
 * Drag-to-rearrange state for the inventory bar. The unit-order API can't move an item between slots, so
 * a drag asks the server to swap the two slots ([WD_SWAP]). [begin] on DragStart, [drop] on the target
 * slot, [end] on DragEnd — if no slot consumed the drop, the item is dropped on the ground.
 */
object ItemMove {
    /** Inventory slot the in-progress drag started from, or -1 when no drag is active. */
    var sourceSlot = -1

    /** Item being dragged (cleared once a slot consumes the drop), else dropped on the ground in [end]. */
    var dragged: EntityIndex? = null

    /** The drag-image panel created for this drag; deleted in [end] so it doesn't linger on screen. */
    var dragImage: Panel? = null

    fun begin(
        slot: Int,
        item: EntityIndex,
        image: Panel,
    ) {
        sourceSlot = slot
        dragged = item
        dragImage = image
    }

    /** Dropped onto [targetSlot] → swap (server-validated). Consumes the drag ([end] still cleans up). */
    fun drop(targetSlot: Int) {
        val from = sourceSlot
        sourceSlot = -1
        dragged = null
        if (from >= 0 && from != targetSlot) {
            GameEvents.sendCustomGameEventToServer(
                WD_SWAP,
                SwapItemsRequest(from, targetSlot),
            )
        }
    }

    /** Drag ended. Drop on the ground if no slot consumed it, then delete the drag image (always). */
    fun end() {
        val item = dragged
        sourceSlot = -1
        dragged = null
        dragImage?.deleteAsync(0f)
        dragImage = null
        if (item != null) {
            val hero = Players.getLocalPlayerPortraitUnit()
            if (Entities.isValidEntity(hero)) {
                Game.prepareUnitOrders(
                    object : PrepareUnitOrdersArgument {
                        override var orderType = Dotaunitorder.DROP_ITEM.value
                        override var abilityIndex: EntityIndex? = item
                        override var targetIndex: EntityIndex? = null
                        override var position: List<Float>? = Entities.getAbsOrigin(hero)
                        override var queue: Boolean? = false
                        override var showEffects: Boolean? = false
                    },
                )
            }
        }
    }
}
