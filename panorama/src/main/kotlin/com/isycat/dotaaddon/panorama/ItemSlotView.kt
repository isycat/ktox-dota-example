package com.isycat.dotaaddon.panorama

import com.isycat.dota.types.EntityIndex
import com.isycat.dota.types.panorama.Abilities
import com.isycat.dota.types.panorama.DOTAItemImage
import com.isycat.dota.types.panorama.DotaAbilityBehavior
import com.isycat.dota.types.panorama.Dotaunitorder
import com.isycat.dota.types.panorama.DragSettings
import com.isycat.dota.types.panorama.Entities
import com.isycat.dota.types.panorama.Game
import com.isycat.dota.types.panorama.GameEvents
import com.isycat.dota.types.panorama.Label
import com.isycat.dota.types.panorama.Panel
import com.isycat.dota.types.panorama.PrepareUnitOrdersArgument
import com.isycat.dota.types.panorama.panorama
import com.isycat.dotaaddon.shared.GameConfig
import com.isycat.dotaaddon.shared.SwapItemsRequest
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

    /** The item name the icon currently shows; re-set the icon only when it changes. */
    private var iconName = ""

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
        // Left- or right-click uses the item: a genuine engine order the server validates exactly like
        // the stock inventory (cooldown, charges, mana, silence). Toggle items toggle, everything else
        // casts no-target (consumables like the bottle WaveDefense refreshes); target/point items can't
        // be aimed from a custom HUD — that targeting flow lives only in the stock action panel.
        icon.setPanelEvent(ON_ACTIVATE) {
            val current = item
            if (current != null) ItemUse.use(current)
        }
        icon.setPanelEvent(ON_CONTEXT_MENU) {
            val current = item
            if (current != null) ItemUse.use(current)
        }
        // Drag to rearrange: DragStart records the source slot and supplies a drag image; DragDrop on a
        // slot asks the server to swap the two. The unit-order API has no item-move, so the swap runs
        // server-side (WaveDefense). Drag isn't a SetPanelEvent event, so register it on the panel.
        panorama.registerEventHandler(
            "DragStart",
            icon,
            fun(
                _: String,
                settings: DragSettings,
            ) {
                if (item == null) return
                val dragImage = panorama.createPanel("DOTAItemImage", panorama.getContextPanel(), "")
                dragImage.addClass("WdItemDragImage")
                (dragImage as DOTAItemImage).itemname = itemName
                settings.displayPanel = dragImage
                settings.removePositionBeforeDrop = true
                ItemMove.sourceSlot = slot
            },
        )
        panorama.registerEventHandler("DragDrop", icon) {
            ItemMove.drop(slot)
        }
    }

    /** Re-read [slot] from the live inventory and update the icon, cooldown sweep and charge count. */
    fun refresh(hero: EntityIndex) {
        val raw = EntityIndex(Entities.getItemInSlot(hero, slot))
        if (!Entities.isValidEntity(raw)) {
            // Empty slot: drop the item, blank the icon, hide overlays, mark the slot empty.
            item = null
            itemName = ""
            iconName = ""
            icon.itemname = ""
            cooldown.visible = false
            charges.visible = false
            setCdStep(0f)
            addClass("WdItemSlotEmpty")
            return
        }
        removeClass("WdItemSlotEmpty")
        item = raw
        itemName = Abilities.getAbilityName(raw)
        // Render via DOTAItemImage.itemname (the engine then shows the item's current variant — incl.
        // bottle charge / power-treads attribute states). Re-set only when the item changes.
        if (itemName != iconName) {
            iconName = itemName
            icon.itemname = itemName
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

    /** Selects the WdCdStepN class (N = 1..24, each a 15° radial clip) for the remaining [fraction]. */
    private fun setCdStep(fraction: Float) {
        val step = if (fraction <= 0f) 0 else minOf(24, ceil(fraction * 24f).toInt())
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
}

/**
 * Drag-to-rearrange state for the inventory bar. The unit-order API can't move an item between slots, so
 * a drag is resolved by asking the server to swap the two slots ([GameConfig.EVENT_SWAP_ITEMS] →
 * WaveDefense `SwapItems`). [sourceSlot] is set by the dragged slot's DragStart; [drop] is called by the
 * slot the item is released onto.
 */
object ItemMove {
    /** Inventory slot the in-progress drag started from, or -1 when no drag is active. */
    var sourceSlot = -1

    fun drop(targetSlot: Int) {
        val from = sourceSlot
        sourceSlot = -1
        if (from >= 0 && from != targetSlot) {
            GameEvents.sendCustomGameEventToServer(
                GameConfig.EVENT_SWAP_ITEMS,
                SwapItemsRequest(from, targetSlot),
            )
        }
    }
}
