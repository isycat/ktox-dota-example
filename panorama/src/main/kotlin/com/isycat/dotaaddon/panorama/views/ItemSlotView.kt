package com.isycat.dotaaddon.panorama.views

import com.isycat.dota.types.EntityIndex
import com.isycat.dota.types.panorama.Abilities
import com.isycat.dota.types.panorama.DOTAItemImage
import com.isycat.dota.types.panorama.DotaAbilityBehavior
import com.isycat.dota.types.panorama.Dotaunitorder
import com.isycat.dota.types.panorama.DragSettings
import com.isycat.dota.types.panorama.Entities
import com.isycat.dota.types.panorama.Game
import com.isycat.dota.types.panorama.GameEvents
import com.isycat.dota.types.panorama.Players
import com.isycat.dota.types.panorama.Label
import com.isycat.dota.types.panorama.Panel
import com.isycat.dota.types.panorama.PrepareUnitOrdersArgument
import com.isycat.dota.types.panorama.panorama
import com.isycat.dotaaddon.shared.events.WD_SWAP
import com.isycat.dotaaddon.shared.events.SwapItemsRequest
import com.isycat.ktox.panorama.dsl.ON_ACTIVATE
import com.isycat.ktox.panorama.dsl.ON_CONTEXT_MENU
import com.isycat.ktox.panorama.dsl.ON_MOUSE_OUT
import com.isycat.ktox.panorama.dsl.ON_MOUSE_OVER
import com.isycat.ktox.panorama.dsl.PanoramaView
import kotlin.math.ceil

/**
 * One inventory slot in the custom item bar — a live-created, **snippet-backed** `@PanoramaView`,
 * the item-side counterpart of [AbilitySlotView].
 *
 * `snippet = true` (the default) is what makes live creation + the native tooltip work: the
 * transpiler emits this class's `layout {}` once as a `<snippet name="ItemSlotView">`, and each
 * instance is created via `$.CreatePanel(...).BLoadLayoutSnippet("ItemSlotView")` so the
 * `DOTAItemImage` carries the snippet XML's `hittest="true"` and its hover bubbles the real Dota
 * item tooltip — which a runtime `$.CreatePanel`'d image does not.
 *
 * Unlike the abilities bar (which rebuilds when the kit changes), inventory slots are FIXED: one view
 * per slot is created once by [ItemsPanel] and [refresh]ed every tick against the live inventory
 * ([Entities.getItemInSlot]). It reads and acts on the real inventory only — the icon, cooldown
 * sweep, charges and tooltip all come from the engine, and clicking issues a genuine cast order.
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

    /** Current cooldown-spiral step (0 = none, 24 = full); the matching WdCdStepN class is on cdSpiral. */
    private var cdStep = 0

    init {
        // A Panorama snippet must have exactly ONE panel child: the icon (with its cooldown overlays)
        // lives inside a single content panel, exactly like AbilitySlotView.
        layout {
            Panel(classes = "WdSlotContent") {
                // hittest=true comes from the snippet XML, so the icon is the hover/click target.
                DOTAItemImage(id = "WdItemSlotIcon", classes = "WdItemIcon", hittest = true) {
                    // Back-to-front: dark cooldown spiral, then the cooldown number, then the charges.
                    Panel(id = "WdItemSlotCdSpiral", classes = "WdAbilityCdSpiral") bind ::cdSpiral
                    Label(id = "WdItemSlotCd", classes = "WdAbilityCooldown") bind ::cooldown
                    Label(id = "WdItemSlotCharges", classes = "WdAbilityCharges") bind ::charges
                } bind ::icon
            }
        }
    }

    /**
     * Bind this view to inventory [slot] (called once after creation): style the live root and wire the
     * hover-tooltip + click-to-use handlers once. The handlers read the live [item] field, which
     * [refresh] keeps current as items move between slots.
     */
    fun bind(slot: Int) {
        this.slot = slot
        addClass("WdItemSlot")
        cooldown.hittest = false
        charges.hittest = false
        cdSpiral.hittest = false
        // Panels default to visible; the cooldown spiral is a dark 100%×100% overlay, so it must start
        // hidden or it darkens every item. setCdStep(0) early-returns when already at step 0, so it never
        // hides it on its own — hide the overlays up front (same as AbilitySlotView.configure does).
        cdSpiral.visible = false
        cooldown.visible = false
        charges.visible = false
        // Seed the EMPTY display up front (icon hidden + empty class) so refresh() can skip the per-tick
        // DOM writes while the slot stays empty. Without this the icon would default visible until the
        // first refresh, and the skip-guard would leave it that way.
        icon.visible = false
        addClass("WdItemSlotEmpty")
        // The slot root is the drop target (so EMPTY slots — whose icon is hidden — still accept drops).
        hittest = true
        icon.setDisableFocusOnMouseDown(true)
        icon.draggable = true
        icon.setPanelEvent(ON_MOUSE_OVER) {
            val current = item
            if (current != null) {
                // Pass the EntityIndex directly (it lowers to its raw int) — same as AbilityIndex below.
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
        // Right-click opens a confirm menu rather than selling outright — a stray right-click used to
        // instant-sell the item with no undo. The menu's Sell button issues the SELL_ITEM order (which
        // works anywhere because WaveDefenseController makes the whole arena a shop).
        icon.setPanelEvent(ON_CONTEXT_MENU) {
            val current = item
            // Anchor the menu to THIS slot so it pops up by the icon (not centred / at the cursor).
            if (current != null) ItemContextMenu.open(current, this@ItemSlotView)
        }
        // Drag to rearrange: DragStart (on the draggable icon) records the source slot + item and supplies
        // a drag image; DragDrop (on the SLOT ROOT, so empty slots count) swaps the two; DragEnd drops the
        // item on the ground if it wasn't dropped onto a slot. The unit-order API has no item-move, so the
        // swap runs server-side (WaveDefenseController). Drag isn't a SetPanelEvent event, so register on the panel.
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
            // Already showing empty (item == null)? The display is correct — skip the per-tick DOM writes.
            // A wave-1 inventory is mostly empty slots, so this removes the bulk of the items HUD's
            // constant idle cost. bind() seeds the empty display up front so this guard holds from frame 1.
            if (item == null) return
            // Empty slot: hide the icon (and its child overlays) entirely. Do NOT set contextEntityIndex
            // to null — the engine's V8 binding rejects null (it expects a Number) and throws.
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
        // Bind the icon to the LIVE item entity (contextEntityIndex) — the stock inventory's mechanism.
        // This renders the item's current icon AND reflects in-place state (power-treads str/agi/int by
        // toggle, the bottle's full/empty/rune variant by charge) off the live binding, with no polling.
        // itemname is set too as a base fallback. Re-bind only when the slot's item entity changes.
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
        // Items show their stock "current charges" (bottle, wards, etc.) when they have any.
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

    /** Selects the WdCdStepN class (N = 1..60, each a 6° radial clip) for the remaining [fraction]. */
    private fun setCdStep(fraction: Float) {
        val step = if (fraction <= 0f) 0 else minOf(60, ceil(fraction * 60f).toInt())
        if (step == cdStep) return
        if (cdStep > 0) cdSpiral.removeClass("WdCdStep$cdStep")
        if (step > 0) cdSpiral.addClass("WdCdStep$step")
        cdSpiral.visible = step > 0
        cdStep = step
    }
}

/**
 * Item activation for the inventory bar. [use] issues the engine's own order — a server-validated
 * request identical to clicking the item in the stock HUD, so cooldown, charges, mana and silence are
 * all enforced natively (no client-authored logic to exploit). Toggle items (e.g. armlet) get
 * `CAST_TOGGLE`; everything else `CAST_NO_TARGET`. The toggle test reads the item's behavior bitmask
 * with Kotlin `and` on an `Int` (→ JS bitwise `&`). Target/point items can't be aimed from a custom HUD.
 */
object ItemUse {
    fun use(item: EntityIndex) {
        val behavior: Int = Abilities.getBehavior(item).toInt()
        val toggleBit: Int = DotaAbilityBehavior.TOGGLE.value.toInt()
        val order =
            if ((behavior and toggleBit) != 0) {
                Dotaunitorder.CAST_TOGGLE.value
            } else {
                Dotaunitorder.CAST_NO_TARGET.value
            }
        Game.prepareUnitOrders(
            object : PrepareUnitOrdersArgument {
                override var orderType = order
                override var abilityIndex: EntityIndex? = item
                override var targetIndex: EntityIndex? = null
                override var position: List<Float>? = null
                override var queue: Boolean? = false
                override var showEffects: Boolean? = false
            },
        )
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
 * a drag is resolved by asking the server to swap the two slots ([WD_SWAP] →
 * WaveDefenseController `SwapItems`). [begin] is called by the dragged slot's DragStart; [drop] by the slot the
 * item is released onto; [end] by DragEnd — if no slot consumed the drop, the item is dropped on the
 * ground (DROP_ITEM at the hero's feet).
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
