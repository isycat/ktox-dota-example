package com.isycat.dotaaddon.panorama

import com.isycat.dota.types.EntityIndex
import com.isycat.dota.types.panorama.Abilities
import com.isycat.dota.types.panorama.DOTAAbilityImage
import com.isycat.dota.types.panorama.GameEvents
import com.isycat.dota.types.panorama.Label
import com.isycat.dota.types.panorama.Panel
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
    lateinit var cooldown: Label
        private set
    lateinit var charges: Label
        private set
    lateinit var levelLabel: Label
        private set

    /** The ability this slot currently represents (set by [configure]); drives [refreshCooldown]. */
    private var ability: EntityIndex? = null

    init {
        // A Panorama snippet must have exactly ONE panel child, so the icon + level pip live inside a
        // single content panel. The live instance's own $.CreatePanel'd root (which configure() styles
        // as WdAbilitySlot) wraps this content — the same wrapper+snippet shape pocket uses.
        layout {
            Panel(classes = "WdSlotContent") {
                // hittest=true comes through from the snippet XML, so the icon is the hover/click
                // target; the cooldown overlay sits inside it, the level pip beneath it.
                DOTAAbilityImage(id = "WdSlotIcon", classes = "WdAbilityIcon", hittest = true) {
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
        if (Abilities.isAttributeBonus(ability)) addClass("WdStatsSlot")
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
        // Upgrade by SLOT, not by entity index: the server resolves the ability from the player's own
        // hero at that slot (see WaveDefense), so the client can't ask to upgrade an arbitrary entity.
        icon.setPanelEvent(ON_ACTIVATE) { AbilityUpgrade.train(slot) }
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
            } else {
                cooldown.visible = false
            }
        }
    }

}

/**
 * Shared upgrade path for ability slots and talents. A client `TRAIN_ABILITY` order is rejected by
 * the engine for *hidden* abilities (the +stats attribute bonus → "ability is hidden"), so instead we
 * fire a custom event and let the server level it up with `UpgradeAbility` (see WaveDefense), which
 * has no such restriction. This also keeps all upgrades on one authoritative server path.
 */
object AbilityUpgrade {
    fun train(slot: Int) {
        GameEvents.sendCustomGameEventToServer(GameConfig.EVENT_UPGRADE_ABILITY, UpgradeRequest(slot))
    }
}
