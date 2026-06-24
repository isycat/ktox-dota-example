package com.isycat.dotaaddon.panorama

import com.isycat.dota.types.EntityIndex
import com.isycat.dota.types.panorama.Abilities
import com.isycat.dota.types.panorama.DOTAAbilityImage
import com.isycat.dota.types.panorama.Dotaunitorder
import com.isycat.dota.types.panorama.Entities
import com.isycat.dota.types.panorama.Game
import com.isycat.dota.types.panorama.GameUI
import com.isycat.dota.types.panorama.Label
import com.isycat.dota.types.panorama.Panel
import com.isycat.dota.types.panorama.Players
import com.isycat.dota.types.panorama.PrepareUnitOrdersArgument
import com.isycat.dota.types.panorama.panorama
import com.isycat.dotaaddon.shared.GameConfig
import com.isycat.ktox.panorama.dsl.ON_ACTIVATE
import com.isycat.ktox.panorama.dsl.ON_MOUSE_OUT
import com.isycat.ktox.panorama.dsl.ON_MOUSE_OVER
import com.isycat.ktox.panorama.dsl.PanoramaView
import kotlin.math.ceil

/**
 * Custom abilities + talents bar that replaces the stock action panel (hidden by [Manifest]).
 *
 * It reads the local hero's kit client-side and builds an icon per *displayed* ability (so hidden /
 * scepter / shard / non-displayed innate entries are filtered out via `isDisplayedAbility`), a stats
 * slot for the attribute bonus, and a row per talent. Each is clickable to spend an ability point
 * (`Abilities.attemptToUpgrade`); upgradeable entries get a highlight, and hovering an icon shows
 * Dota's native ability tooltip (the same `DOTAShowAbilityTooltipForEntityIndex` dispatch the stock
 * HUD uses). It rebuilds only when the level/point "signature" changes, so the refresh loop is cheap.
 *
 * Children are created imperatively with `$.CreatePanel` because a hero's kit is only known at
 * runtime; once `@PanoramaView`s can instantiate their `layout {}` subtree, each slot can become a
 * small composed view instead.
 */
@PanoramaView(snippet = false)
class AbilitiesPanel : Panel(id = "WdAbilities", type = "Panel") {
    lateinit var talentColumn: Panel
        private set
    lateinit var abilityRow: Panel
        private set

    private var signature = ""

    init {
        layout {
            Panel(id = "WdTalentColumn", classes = "WdTalentColumn") bind ::talentColumn
            Panel(id = "WdAbilityRow", classes = "WdAbilityRow") bind ::abilityRow
        }
    }

    override fun onLoad() {
        refresh()
    }

    private fun refresh() {
        val hero = Players.getLocalPlayerPortraitUnit()
        if (Entities.isValidEntity(hero)) {
            val current = buildSignature(hero)
            if (current != signature) {
                signature = current
                rebuild(hero)
            }
            // Cooldowns change every frame, so they update every tick (not just on rebuild). The
            // cooldown label for ability slot i carries id "WdCd{i}", looked up by index.
            updateCooldowns(hero)
        }
        panorama.schedule(GameConfig.ABILITY_REFRESH_SECONDS) { refresh() }
    }

    private fun updateCooldowns(hero: EntityIndex) {
        val count = Entities.getAbilityCount(hero)
        for (i in 0 until count) {
            val label = abilityRow.findChildTraverse("WdCd$i")
            if (label != null) {
                val remaining = Abilities.getCooldownTimeRemaining(Entities.getAbility(hero, i))
                if (remaining > 0.5f) {
                    (label as Label).text = "${ceil(remaining).toInt()}"
                    label.visible = true
                } else {
                    label.visible = false
                }
            }
        }
    }

    /** Cheap fingerprint of "anything that would change the panel": ability points + every level. */
    private fun buildSignature(hero: EntityIndex): String {
        var sig = "${Entities.getAbilityPoints(hero)}"
        val count = Entities.getAbilityCount(hero)
        for (i in 0 until count) {
            val ability = Entities.getAbility(hero, i)
            if (Entities.isValidEntity(ability)) {
                sig = "$sig:${Abilities.getLevel(ability)}"
            }
        }
        return sig
    }

    private fun rebuild(hero: EntityIndex) {
        abilityRow.removeAndDeleteChildren()
        talentColumn.removeAndDeleteChildren()
        val points = Entities.getAbilityPoints(hero)
        val count = Entities.getAbilityCount(hero)
        for (i in 0 until count) {
            val ability = Entities.getAbility(hero, i)
            if (!Entities.isValidEntity(ability)) continue
            val name = Abilities.getAbilityName(ability)
            if (name == "") continue
            val level = Abilities.getLevel(ability)
            val maxLevel = Abilities.getMaxLevel(ability)
            // canAbilityBeUpgraded respects the per-tier talent rule (the unlearned talent in an
            // already-picked tier reports a "cannot" result), unlike a bare level < maxLevel check.
            // CAN_BE_UPGRADED == 0; we still gate on having a point to spend.
            val canUpgrade = points > 0 && Abilities.canAbilityBeUpgraded(ability, false).toInt() == 0
            // Talents and the attribute-bonus (+stats) are shown even though they aren't flagged as
            // "displayed"; everything else must pass isDisplayedAbility (filters hidden / scepter /
            // shard entries that were wrongly appearing).
            if (GameUI.isAbilityDOTATalent(name)) {
                addTalent(ability, name, level, canUpgrade)
            } else if (Abilities.isAttributeBonus(ability) || Abilities.isDisplayedAbility(ability)) {
                addAbility(ability, name, level, maxLevel, canUpgrade, i)
            }
        }
        // Surface the talents only when there's a point to spend — otherwise it's a distracting
        // always-on box on the left.
        talentColumn.visible = points > 0
    }

    private fun addAbility(
        ability: EntityIndex,
        name: String,
        level: Int,
        maxLevel: Int,
        canUpgrade: Boolean,
        index: Int,
    ) {
        val slot = panorama.createPanel("Panel", abilityRow, "")
        slot.addClass("WdAbilitySlot")
        slot.hittest = true
        if (Abilities.isAttributeBonus(ability)) slot.addClass("WdStatsSlot")
        // Highlight when a point can be spent here; grey out when it's unlearned and not learnable
        // right now (no point, or level-gated). A learned ability with no point sits at neither.
        if (canUpgrade) {
            slot.addClass("WdCanUpgrade")
        } else if (level == 0) {
            slot.addClass("WdLocked")
        }

        // The ability image, created directly so it always renders. hittest=true makes it the
        // hover/click target, so the mouse handlers registered on it below fire directly on it.
        val icon = panorama.createPanel("DOTAAbilityImage", slot, "")
        icon.addClass("WdAbilityIcon")
        icon.hittest = true
        (icon as DOTAAbilityImage).abilityname = name

        // Cooldown readout overlaying the icon, keyed by slot index so updateCooldowns can find it.
        val cd = panorama.createPanel("Label", icon, "WdCd$index")
        cd.addClass("WdAbilityCooldown")
        cd.hittest = false
        cd.visible = false

        // Only show a level pip for abilities that actually have multiple levels — skips innates,
        // which sit at a fixed level and would otherwise read a confusing "1/1".
        if (maxLevel > 1) {
            val lvl = panorama.createPanel("Label", slot, "")
            lvl.addClass("WdAbilityLevel")
            lvl.hittest = false
            (lvl as Label).text = "$level/$maxLevel"
        }

        // Native ability tooltip on hover — dispatched on the image, by name, with -1 for the owning
        // entity (the stock HUD / pocket args). Handlers live on the icon itself: it's the hittest
        // target so onmouseover fires directly on it — onmouseover does NOT bubble up from a child.
        icon.setPanelEvent(ON_MOUSE_OVER) {
            panorama.dispatchEvent("DOTAShowAbilityTooltipForEntityIndex", icon, name, -1)
        }
        icon.setPanelEvent(ON_MOUSE_OUT) {
            panorama.dispatchEvent("DOTAHideAbilityTooltip", icon)
        }
        icon.setPanelEvent(ON_ACTIVATE) { upgradeAbility(ability) }
        icon.setDisableFocusOnMouseDown(true)
    }

    /**
     * Upgrade [ability] via the engine's TRAIN_ABILITY order — the same path the stock action panel
     * uses, and the same for normal abilities, talents, and the +stats attribute bonus.
     * `Abilities.attemptToUpgrade` issues an order the engine rejects for HIDDEN abilities (the +stats
     * `special_bonus_attributes` → "ability is hidden"); the train order upgrades those too while still
     * respecting available ability points. Targets the selected unit (the player's hero).
     */
    private fun upgradeAbility(ability: EntityIndex) {
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

    private fun addTalent(
        ability: EntityIndex,
        name: String,
        level: Int,
        canUpgrade: Boolean,
    ) {
        val row = panorama.createPanel("Panel", talentColumn, "")
        row.addClass("WdTalentRow")
        row.hittest = true
        if (level > 0) {
            row.addClass("WdTalentTaken")
        } else if (canUpgrade) {
            row.addClass("WdCanUpgrade")
        } else {
            // Unlearned and not choosable right now (tier not reached, or its pair already taken).
            row.addClass("WdLocked")
        }

        val lbl = panorama.createPanel("Label", row, "")
        lbl.addClass("WdTalentLabel")
        GameUI.setupDOTATalentNameLabel(lbl, name)

        row.setPanelEvent(ON_ACTIVATE) { upgradeAbility(ability) }
    }
}
