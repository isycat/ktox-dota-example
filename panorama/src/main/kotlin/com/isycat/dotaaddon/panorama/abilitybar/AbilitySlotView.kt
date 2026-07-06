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
import com.isycat.dotaaddon.panorama.hud.CooldownDisplay
import com.isycat.dotaaddon.panorama.hud.Keybind
import com.isycat.dotaaddon.shared.events.UpgradeRequest
import com.isycat.dotaaddon.shared.events.WD_UPGRADE
import com.isycat.ktox.panorama.dsl.ON_ACTIVATE
import com.isycat.ktox.panorama.dsl.ON_CONTEXT_MENU
import com.isycat.ktox.panorama.dsl.ON_MOUSE_OUT
import com.isycat.ktox.panorama.dsl.ON_MOUSE_OVER
import com.isycat.ktox.panorama.dsl.PanoramaView

/**
 * One ability / +stats slot in the abilities bar — a snippet-backed [PanoramaView] created live per
 * ability in [AbilitiesPanel.rebuild], fully initialised by its CONSTRUCTOR (no two-phase setup).
 * The snippet-backed image keeps `hittest=true` from the XML, so the native ability tooltip works
 * (a runtime `$.CreatePanel`'d image does not).
 *
 * The state parameters default only so the bare `AbilitySlotView()` snippet-DEFINITION placement in
 * [AbilitiesPanel]'s layout compiles; every live instance passes real values.
 */
@PanoramaView
class AbilitySlotView(
    parent: Panel? = null,
    /** Ability-slot index on the hero (drives the server-side +stats upgrade path). */
    private val slot: Int = -1,
    /** The ability this slot represents; drives every per-tick refresh. */
    private val ability: EntityIndex = EntityIndex(-1),
    private val name: String = "",
    private val level: Int = 0,
    private val maxLevel: Int = 0,
    private val canUpgrade: Boolean = false,
) : Panel(type = "Panel") {
    lateinit var icon: DOTAAbilityImage
        private set

    /** Big "✕" drawn over the icon while the hero is silenced. */
    lateinit var silenceX: Label
        private set
    lateinit var cdSpiral: Panel
        private set
    lateinit var cooldown: Label
        private set
    lateinit var charges: Label
        private set
    lateinit var levelLabel: Label
        private set

    /** Bound-key badge (top-left); shown only when [AbilityBarConfig.SHOW_KEYBINDS] and the slot is bound. */
    lateinit var keyLabel: Label
        private set

    /** Cooldown label + spiral, shared HUD component (owns the per-tick DOM-write guards). */
    private lateinit var cd: CooldownDisplay

    /** Last-rendered toggle / auto-cast state, so the per-tick refresh only writes the DOM on a change. */
    private var lastToggleOn = false
    private var lastAutocastOn = false

    /** Last-rendered castability state (out-of-mana / silenced), same per-tick DOM-write guard. */
    private var lastNoMana = false
    private var lastSilenced = false

    /** True once the ability is learned (level > 0): only then do the out-of-mana / silence washes show. */
    private var learned = false

    init {
        // A snippet must have exactly one panel child, so the icon + level pip live in one content panel.
        layout {
            Panel(classes = "WdSlotContent") {
                DOTAAbilityImage(id = "WdSlotIcon", classes = "WdAbilityIcon", hittest = true) {
                    Panel(id = "WdSlotCdSpiral", classes = "WdAbilityCdSpiral") bind ::cdSpiral
                    Label(id = "WdSlotSilenceX", classes = "WdAbilitySilenceX") bind ::silenceX
                    Label(id = "WdSlotCd", classes = "WdAbilityCooldown") bind ::cooldown
                    Label(id = "WdSlotCharges", classes = "WdAbilityCharges") bind ::charges
                    Label(id = "WdSlotKey", classes = "WdSlotKey") bind ::keyLabel
                } bind ::icon
                Label(id = "WdSlotLevel", classes = "WdAbilityLevel") bind ::levelLabel
            }
        }
    }

    /**
     * One-time RUNTIME setup: overlays, highlight state, and tooltip + click handlers. Runs after
     * bootstrap wired the selectors and constructor state — and never during the JVM layout
     * evaluation, where the engine APIs used here are unavailable.
     */
    override fun onLoad() {
        cd = CooldownDisplay(cdSpiral, cooldown)
        charges.hittest = false
        charges.visible = false
        silenceX.text = SILENCE_GLYPH
        silenceX.hittest = false
        silenceX.visible = false
        learned = level > 0 // only a learned ability shows the out-of-mana / silence wash

        // The live root is a bare $.CreatePanel'd panel (the snippet carries only content), so style it here.
        addClass(AbilityBarStyles.SLOT)
        icon.abilityname = name
        val isStats = Abilities.isAttributeBonus(ability)
        if (isStats) addClass(AbilityBarStyles.STATS_SLOT)
        // Keybind badge (top-left): the engine's bound key for this ability slot, compacted (MOUSE 5 → M5).
        // Hidden on the +stats slot, on a PASSIVE ability (nothing to press), when unbound, or feature-off.
        val keybind =
            if (AbilityBarConfig.SHOW_KEYBINDS && !isStats && !Abilities.isPassive(ability)) {
                Keybind.short(Game.getKeybindForAbility(slot))
            } else {
                ""
            }
        if (keybind != "") {
            keyLabel.text = keybind
            keyLabel.hittest = false
        } else {
            keyLabel.visible = false
        }
        // Highlight when a point can be spent here; grey out when unlearned and not learnable now.
        if (canUpgrade) {
            addClass(AbilityBarStyles.CAN_UPGRADE)
        } else if (level == 0) {
            addClass(AbilityBarStyles.LOCKED)
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
        refreshCastability(silenced)
        // Charge-based abilities show a charge count; the sweep itself (charge-aware) is shared.
        if (Abilities.usesAbilityCharges(ability)) {
            charges.text = "${Abilities.getCurrentAbilityCharges(ability).toInt()}"
            charges.visible = true
        } else if (charges.visible == true) {
            charges.visible = false
        }
        cd.refreshFrom(ability)
        refreshToggleAndAutocast()
    }

    /**
     * Reflect why the ability can't be cast: dimmed when out of mana, a big ✕ while silenced. Only a
     * learned ability shows these, and only when a state flips (this runs every tick).
     */
    private fun refreshCastability(silenced: Boolean) {
        val noMana = learned && !Abilities.isOwnersManaEnough(ability)
        if (noMana != lastNoMana) {
            lastNoMana = noMana
            if (noMana) icon.addClass(AbilityBarStyles.NO_MANA) else icon.removeClass(AbilityBarStyles.NO_MANA)
        }
        val showSilence = learned && silenced
        if (showSilence != lastSilenced) {
            lastSilenced = showSilence
            silenceX.visible = showSilence
        }
    }

    /** Reflect toggle / auto-cast state on the icon (border + badge). Only writes the DOM when a state flips. */
    private fun refreshToggleAndAutocast() {
        if (Abilities.isToggle(ability)) {
            val on = Abilities.getToggleState(ability)
            if (on != lastToggleOn) {
                lastToggleOn = on
                if (on) icon.addClass(AbilityBarStyles.TOGGLED_ON) else icon.removeClass(AbilityBarStyles.TOGGLED_ON)
            }
        }
        if (Abilities.isAutocast(ability)) {
            val on = Abilities.getAutoCastState(ability)
            if (on != lastAutocastOn) {
                lastAutocastOn = on
                if (on) icon.addClass(AbilityBarStyles.AUTOCAST_ON) else icon.removeClass(AbilityBarStyles.AUTOCAST_ON)
            }
        }
    }

    companion object {
        private const val SILENCE_GLYPH = "✕"
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
