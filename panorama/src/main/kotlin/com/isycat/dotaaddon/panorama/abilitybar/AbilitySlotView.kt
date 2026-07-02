package com.isycat.dotaaddon.panorama.abilitybar

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
import com.isycat.dotaaddon.shared.events.UpgradeRequest
import com.isycat.dotaaddon.shared.events.WD_UPGRADE
import com.isycat.ktox.panorama.dsl.ON_ACTIVATE
import com.isycat.ktox.panorama.dsl.ON_CONTEXT_MENU
import com.isycat.ktox.panorama.dsl.ON_MOUSE_OUT
import com.isycat.ktox.panorama.dsl.ON_MOUSE_OVER
import com.isycat.ktox.panorama.dsl.PanoramaView
import kotlin.math.ceil

/**
 * One ability / +stats slot in the abilities bar — a snippet-backed [PanoramaView] created live per
 * ability in [AbilitiesPanel.rebuild]. The snippet-backed image keeps `hittest=true` from the XML, so
 * the native ability tooltip works (a runtime `$.CreatePanel`'d image does not).
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

    /** Big "✕" drawn over the icon while the hero is silenced. */
    lateinit var silenceX: Label
        private set
    lateinit var cooldown: Label
        private set
    lateinit var charges: Label
        private set
    lateinit var levelLabel: Label
        private set

    /** The ability this slot currently represents (set by [configure]); drives [refreshCooldown]. */
    private var ability: EntityIndex? = null

    /** Whether the cooldown-spiral overlay is currently shown (avoids redundant per-tick visibility writes). */
    private var cdVisible = false

    /** Last-rendered toggle / auto-cast state, so the per-tick refresh only writes the DOM on a change. */
    private var lastToggleOn = false
    private var lastAutocastOn = false

    /** Last-rendered castability state (out-of-mana / silenced), same per-tick DOM-write guard. */
    private var lastNoMana = false
    private var lastSilenced = false

    /** True once the ability is learned (level > 0): only then do the out-of-mana / silence washes show. */
    private var learned = false

    /** Lets [refreshCooldown] skip per-tick DOM writes while the ability sits ready. Reset by [configure]. */
    private var readyShown = false

    init {
        // A snippet must have exactly one panel child, so the icon + level pip live in one content panel.
        layout {
            Panel(classes = "WdSlotContent") {
                DOTAAbilityImage(id = "WdSlotIcon", classes = "WdAbilityIcon", hittest = true) {
                    Panel(id = "WdSlotCdSpiral", classes = "WdAbilityCdSpiral") bind ::cdSpiral
                    Label(id = "WdSlotSilenceX", classes = "WdAbilitySilenceX") bind ::silenceX
                    Label(id = "WdSlotCd", classes = "WdAbilityCooldown") bind ::cooldown
                    Label(id = "WdSlotCharges", classes = "WdAbilityCharges") bind ::charges
                } bind ::icon
                Label(id = "WdSlotLevel", classes = "WdAbilityLevel") bind ::levelLabel
            }
        }
    }

    /** Point this slot at [ability]: fill the icon, level pip, highlight state, and wire tooltip + click handlers. */
    fun configure(
        slot: Int,
        ability: EntityIndex,
        name: String,
        level: Int,
        maxLevel: Int,
        canUpgrade: Boolean,
    ) {
        this.ability = ability
        readyShown = false // new ability → next refreshCooldown re-writes the display
        // The live root is a bare $.CreatePanel'd panel (the snippet carries only content), so style it here.
        addClass("WdAbilitySlot")
        icon.abilityname = name
        cooldown.hittest = false
        cooldown.visible = false
        charges.hittest = false
        charges.visible = false
        cdSpiral.hittest = false
        cdSpiral.visible = false
        cdVisible = false
        learned = level > 0 // only a learned ability shows the out-of-mana / silence wash

        icon.removeClass("WdNoMana")
        silenceX.text = "✕"
        silenceX.hittest = false
        silenceX.visible = false
        lastNoMana = false
        lastSilenced = false
        icon.removeClass("WdToggledOn")
        icon.removeClass("WdAutocastOn")
        lastToggleOn = false
        lastAutocastOn = false
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
        // Native tooltip on hover (anchored on the snippet image, by name).
        icon.setPanelEvent(ON_MOUSE_OVER) {
            panorama.dispatchEvent("DOTAShowAbilityTooltipForEntityIndex", icon, name, -1)
        }
        icon.setPanelEvent(ON_MOUSE_OUT) {
            panorama.dispatchEvent("DOTAHideAbilityTooltip", icon)
        }
        // Click levels the ability (casting is via hotkey, like the stock bar). The hidden +stats bonus
        // takes the server-side path since the engine rejects TRAIN_ABILITY on it; see [AbilityUpgrade].
        icon.setPanelEvent(ON_ACTIVATE) {
            if (isStats) AbilityUpgrade.trainStats(slot) else AbilityUpgrade.train(ability)
        }
        // Right-click toggles auto-cast (a no-op on abilities without it, like the stock bar).
        icon.setPanelEvent(ON_CONTEXT_MENU) { AbilityUpgrade.toggleAutocast(ability) }
        icon.setDisableFocusOnMouseDown(true)
    }

    /** Refresh the cooldown overlay + charge count; called every tick by [AbilitiesPanel]. */
    fun refreshCooldown(silenced: Boolean) {
        val current = ability
        if (current == null) return // TODO: fix code style
        refreshCastability(current, silenced)
        // Charge-based abilities show a charge count + per-charge restore timer instead of the cooldown sweep.
        if (Abilities.usesAbilityCharges(current)) {
            charges.text = "${Abilities.getCurrentAbilityCharges(current).toInt()}"
            charges.visible = true
            // Charge abilities get the sweep too — driven by the per-charge restore timer (its full duration
            // is the ability's cooldown length).
            val restore = Abilities.getAbilityChargeRestoreTimeRemaining(current).toFloat()
            if (restore > 0.05f) { // (Similar shape in ItemSlotView is INTENTIONAL: packages stay copy-paste self-contained.)
                cooldown.text = formatCd(restore)
                cooldown.visible = true
                val length = Abilities.getCooldownLength(current).toFloat()
                setCdStep(if (length > 0f) restore / length else 1f)
            } else {
                cooldown.visible = false
                setCdStep(0f)
            }
        } else {
            val remaining = Abilities.getCooldownTimeRemaining(current)
            if (remaining > 0.05f) {
                readyShown = false
                charges.visible = false
                cooldown.text = formatCd(remaining)
                cooldown.visible = true
                val length = Abilities.getCooldownLength(current).toFloat()
                setCdStep(if (length > 0f) remaining / length else 1f)
            } else if (!readyShown) {
                // Ability ready: write the "ready" display once, then skip until it goes on cooldown again.
                readyShown = true
                charges.visible = false
                cooldown.visible = false
                setCdStep(0f)
            }
        }
        refreshToggleAndAutocast(current)
    }

    /**
     * Reflect why the ability can't be cast: dimmed when out of mana, a big ✕ while silenced. Only a
     * learned ability shows these, and only when a state flips (this runs every tick).
     */
    private fun refreshCastability(
        current: EntityIndex,
        silenced: Boolean,
    ) {
        val noMana = learned && !Abilities.isOwnersManaEnough(current)
        if (noMana != lastNoMana) {
            lastNoMana = noMana
            if (noMana) icon.addClass("WdNoMana") else icon.removeClass("WdNoMana")
        }
        val showSilence = learned && silenced
        if (showSilence != lastSilenced) {
            lastSilenced = showSilence
            silenceX.visible = showSilence
        }
    }

    /** Reflect toggle / auto-cast state on the icon (border + badge). Only writes the DOM when a state flips. */
    private fun refreshToggleAndAutocast(current: EntityIndex) {
        if (Abilities.isToggle(current)) {
            val on = Abilities.getToggleState(current)
            if (on != lastToggleOn) {
                lastToggleOn = on
                if (on) icon.addClass("WdToggledOn") else icon.removeClass("WdToggledOn")
            }
        }
        if (Abilities.isAutocast(current)) {
            val on = Abilities.getAutoCastState(current)
            if (on != lastAutocastOn) {
                lastAutocastOn = on
                if (on) icon.addClass("WdAutocastOn") else icon.removeClass("WdAutocastOn")
            }
        }
    }

    /** Renders a cooldown: 1 decimal under 5s (where the fraction matters), whole seconds above. */
    private fun formatCd(remaining: Float): String =
        if (remaining >= 5f) {
            "${ceil(remaining).toInt()}"
        } else {
            val whole = remaining.toInt()
            val tenth = ((remaining - whole) * 10f).toInt()
            "$whole.$tenth"
        }

    /**
     * Drives the cooldown spiral CONTINUOUSLY from the exact remaining [fraction] (0 = ready → hidden,
     * 1 = full). Sets the dark wedge's radial clip inline each tick — `panel.style.clip = radial(…)`,
     * pocket's technique — so the sweep is smooth instead of stepping a whole second at a time. The dark
     * wedge is the last `deg` degrees before 12 o'clock and recedes clockwise as the cooldown elapses.
     */
    private fun setCdStep(fraction: Float) {
        if (fraction <= 0f) { // TODO: duplicated code. Fix the unshared code between AbilitySlotView and ItemSlotView
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
 * Upgrade paths for the abilities bar. [train] issues the engine's own TRAIN_ABILITY order (the engine
 * validates points, level, max level, talent exclusivity). [trainStats] is the exception: the hidden
 * +stats bonus is rejected by that order, so it's upgraded server-side via a custom event.
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
        GameEvents.sendCustomGameEventToServer(WD_UPGRADE, UpgradeRequest(slot))
    }

    /** Toggle an ability's auto-cast via the engine's own order (server-validated, like the stock bar). */
    fun toggleAutocast(ability: EntityIndex) {
        Game.prepareUnitOrders(
            object : PrepareUnitOrdersArgument {
                override var orderType = Dotaunitorder.CAST_TOGGLE_AUTO.value
                override var abilityIndex: EntityIndex? = ability
                override var targetIndex: EntityIndex? = null
                override var position: List<Float>? = null
                override var queue: Boolean? = false
                override var showEffects: Boolean? = false
            },
        )
    }
}
