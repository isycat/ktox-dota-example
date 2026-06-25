package com.isycat.dotaaddon.panorama

import com.isycat.dota.types.EntityIndex
import com.isycat.dota.types.panorama.Abilities
import com.isycat.dota.types.panorama.DOTAAbilityImage
import com.isycat.dota.types.panorama.Dotaunitorder
import com.isycat.dota.types.panorama.Game
import com.isycat.dota.types.panorama.GameEvents
import com.isycat.dota.types.panorama.Label
import com.isycat.dota.types.panorama.Panel
import com.isycat.dota.types.panorama.PrepareUnitOrdersArgument
import com.isycat.dota.types.panorama.panorama
import com.isycat.dotaaddon.shared.GameConfig
import com.isycat.dotaaddon.shared.UpgradeRequest
import com.isycat.ktox.panorama.dsl.ON_ACTIVATE
import com.isycat.ktox.panorama.dsl.ON_MOUSE_OUT
import com.isycat.ktox.panorama.dsl.ON_MOUSE_OVER
import com.isycat.ktox.panorama.dsl.PanoramaView
import kotlin.math.ceil

/**
 * One ability / +stats slot in the abilities bar — a live-created, **snippet-backed** `@PanoramaView`.
 *
 * `snippet = true` (the default) is what makes live creation work: the transpiler emits this class's
 * `layout {}` once as a `<snippet name="AbilitySlotView">` definition, and the generated constructor
 * creates each instance with `$.CreatePanel(...).BLoadLayoutSnippet("AbilitySlotView")` (pocket's
 * pattern) so the icon/cooldown/level children exist before the selectors bind. Because the
 * `DOTAAbilityImage` comes from the XML snippet with `hittest="true"`, its hover bubbles and the
 * native ability tooltip works — which a runtime `$.CreatePanel`'d image does not.
 *
 * Placed once in [AbilitiesPanel]'s `layout {}` purely to register the snippet definition; real
 * instances are created per-ability in [AbilitiesPanel.rebuild] via `AbilitySlotView(abilityRow)`.
 */
@PanoramaView
class AbilitySlotView(
    parent: Panel? = null,
) : Panel(type = "Panel") {
    lateinit var icon: DOTAAbilityImage
        private set

    /** Dark radial-clip overlay drawn over the icon as the cooldown "spiral" (see [refreshCooldown]). */
    lateinit var cdSpiral: Panel
        private set
    lateinit var cooldown: Label
        private set
    lateinit var charges: Label
        private set
    lateinit var levelLabel: Label
        private set

    /** The ability this slot currently represents (set by [configure]); drives [refreshCooldown]. */
    private var ability: EntityIndex? = null

    /** Current cooldown-spiral step (0 = none, 12 = full); the matching WdCdStepN class is on cdSpiral. */
    private var cdStep = 0

    init {
        // A Panorama snippet must have exactly ONE panel child, so the icon + level pip live inside a
        // single content panel. The live instance's own $.CreatePanel'd root (which configure() styles
        // as WdAbilitySlot) wraps this content — the same wrapper+snippet shape pocket uses.
        layout {
            Panel(classes = "WdSlotContent") {
                // hittest=true comes through from the snippet XML, so the icon is the hover/click
                // target; the cooldown overlay sits inside it, the level pip beneath it.
                DOTAAbilityImage(id = "WdSlotIcon", classes = "WdAbilityIcon", hittest = true) {
                    // Children render back-to-front: the dark cooldown spiral sits under the cooldown
                    // number (centred on top) which sits under the charge count (bottom-right corner).
                    Panel(id = "WdSlotCdSpiral", classes = "WdAbilityCdSpiral") bind ::cdSpiral
                    Label(id = "WdSlotCd", classes = "WdAbilityCooldown") bind ::cooldown
                    // Charge count (bottom-right), shown only for charge-based abilities.
                    Label(id = "WdSlotCharges", classes = "WdAbilityCharges") bind ::charges
                } bind ::icon
                Label(id = "WdSlotLevel", classes = "WdAbilityLevel") bind ::levelLabel
            }
        }
    }

    /**
     * Point this slot at [ability]: fill the icon, level pip, highlight/greyed state, and wire the
     * hover tooltip + click-to-upgrade handlers.
     */
    fun configure(
        slot: Int,
        ability: EntityIndex,
        name: String,
        level: Int,
        maxLevel: Int,
        canUpgrade: Boolean,
    ) {
        this.ability = ability
        // The live root is a bare $.CreatePanel'd panel (the snippet only carries the content), so the
        // slot's own styling class is applied here.
        addClass("WdAbilitySlot")
        icon.abilityname = name
        cooldown.hittest = false
        cooldown.visible = false
        charges.hittest = false
        charges.visible = false
        cdSpiral.hittest = false
        cdSpiral.visible = false
        cdStep = 0
        val isStats = Abilities.isAttributeBonus(ability)
        if (isStats) addClass("WdStatsSlot")
        // Highlight when a point can be spent here; grey out when unlearned and not learnable now.
        if (canUpgrade) {
            addClass("WdCanUpgrade")
        } else if (level == 0) {
            addClass("WdLocked")
        }
        // Only show a level pip for multi-level abilities (skips innates that would read "1/1").
        if (maxLevel > 1) {
            levelLabel.text = "$level/$maxLevel"
        } else {
            levelLabel.visible = false
        }
        // Native tooltip on hover (anchored on the snippet image, by name, -1) and upgrade on click.
        icon.setPanelEvent(ON_MOUSE_OVER) {
            panorama.dispatchEvent("DOTAShowAbilityTooltipForEntityIndex", icon, name, -1)
        }
        icon.setPanelEvent(ON_MOUSE_OUT) {
            panorama.dispatchEvent("DOTAHideAbilityTooltip", icon)
        }
        // Normal abilities + talents go through the engine's own TRAIN_ABILITY order, which validates
        // points, hero level, max level, and talent-tier exclusivity natively (no client-authored
        // logic to exploit). The +stats attribute bonus is hidden and the engine rejects that order
        // for it, so it alone is upgraded server-side (by slot, re-validated in WaveDefense).
        icon.setPanelEvent(ON_ACTIVATE) {
            if (isStats) AbilityUpgrade.trainStats(slot) else AbilityUpgrade.train(ability)
        }
        icon.setDisableFocusOnMouseDown(true)
    }

    /** Refresh the cooldown overlay + charge count; called every tick by [AbilitiesPanel]. */
    fun refreshCooldown() {
        val current = ability
        if (current == null) return
        // Charge-based abilities tick a per-charge restore timer (not the regular cooldown) and show a
        // charge count; non-charge abilities use the plain cooldown.
        if (Abilities.usesAbilityCharges(current)) {
            charges.text = "${Abilities.getCurrentAbilityCharges(current).toInt()}"
            charges.visible = true
            // Charge abilities communicate readiness via the charge count, not a sweep.
            setCdStep(0f)
            val restore = Abilities.getAbilityChargeRestoreTimeRemaining(current).toFloat()
            if (restore > 0.5f) {
                cooldown.text = "${ceil(restore).toInt()}"
                cooldown.visible = true
            } else {
                cooldown.visible = false
            }
        } else {
            charges.visible = false
            val remaining = Abilities.getCooldownTimeRemaining(current)
            if (remaining > 0.5f) {
                cooldown.text = "${ceil(remaining).toInt()}"
                cooldown.visible = true
                val length = Abilities.getCooldownLength(current).toFloat()
                setCdStep(if (length > 0f) remaining / length else 1f)
            } else {
                cooldown.visible = false
                setCdStep(0f)
            }
        }
    }

    /**
     * Drives the cooldown spiral: selects the WdCdStepN class (N = 1..12, each a 30°-stepped radial
     * clip) whose dark sector covers the remaining [fraction] of the cooldown, swapping it on [cdSpiral]
     * only when the step actually changes. Step 0 hides the overlay.
     */
    private fun setCdStep(fraction: Float) {
        val step = if (fraction <= 0f) 0 else minOf(12, ceil(fraction * 12f).toInt())
        if (step == cdStep) return
        if (cdStep > 0) cdSpiral.removeClass("WdCdStep$cdStep")
        if (step > 0) cdSpiral.addClass("WdCdStep$step")
        cdSpiral.visible = step > 0
        cdStep = step
    }
}

/**
 * Upgrade paths for the abilities bar.
 *
 * [train] is the normal path for ordinary abilities and talents: it issues the engine's own
 * `TRAIN_ABILITY` order, exactly like the stock action panel, so the engine validates everything
 * natively — available points, hero level, ability max level, and talent-tier exclusivity (you can't
 * take both sides of a tier). It's a request the server validates, not client-authored logic.
 *
 * [trainStats] is the one exception: the +stats attribute bonus is a *hidden* ability that the engine
 * rejects from a TRAIN_ABILITY order ("ability is hidden"), so it is upgraded server-side via a custom
 * event (re-validated against the engine's rules in WaveDefense).
 */
object AbilityUpgrade {
    fun train(ability: EntityIndex) {
        Game.prepareUnitOrders(
            object : PrepareUnitOrdersArgument {
                override var orderType = Dotaunitorder.TRAIN_ABILITY.value
                override var abilityIndex: EntityIndex? = ability
                override var targetIndex: EntityIndex? = null
                override var position: List<Float>? = null
                override var queue: Boolean? = false
                override var showEffects: Boolean? = false
            },
        )
    }

    fun trainStats(slot: Int) {
        GameEvents.sendCustomGameEventToServer(GameConfig.EVENT_UPGRADE_ABILITY, UpgradeRequest(slot))
    }
}
