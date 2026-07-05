package com.isycat.dotaaddon.panorama.abilitybar
import com.isycat.dota.types.EntityIndex
import com.isycat.dota.types.panorama.Abilities
import com.isycat.dota.types.panorama.AbilityLearnResult
import com.isycat.dota.types.panorama.Entities
import com.isycat.dota.types.panorama.GameUI
import com.isycat.dota.types.panorama.Panel
import com.isycat.dota.types.panorama.Players
import com.isycat.dota.types.panorama.panorama
import com.isycat.ktox.panorama.dsl.ON_ACTIVATE
import com.isycat.ktox.panorama.dsl.PanoramaView

/**
 * Custom abilities + talents bar that replaces the stock action panel (hidden by [Manifest]). Reads the
 * local hero's kit and shows an [AbilitySlotView] per displayed ability plus the +stats bonus and a row
 * per talent. Clicking upgrades via the engine's TRAIN_ABILITY order ([AbilityUpgrade]). Rebuilds only
 * when the level/point signature changes, so the refresh loop is cheap.
 */
// hittest=false is CRITICAL: #WdAbilities is width:100%, so the default hittest=true would capture every
// mouse move across the whole screen. The icons and talent rows carry their own hittest and are unaffected.
@PanoramaView
class AbilitiesPanel : Panel(id = "WdAbilities", type = "Panel", hittest = false) {
    lateinit var talentColumn: Panel
        private set
    lateinit var abilityRow: Panel
        private set

    private var signature = ""

    /** Counts refresh ticks so the heavier layout scan runs coarser than the per-tick cooldown sweep (see [refresh]). */
    private var layoutScanTick = 0

    /** The live ability slots from the current [rebuild]; their cooldowns refresh every tick. */
    private val slots = mutableListOf<AbilitySlotView>()

    init {
        layout {
            Panel(id = "WdTalentColumn", classes = "WdTalentColumn") bind ::talentColumn
            // This AbilitySlotView() only registers the snippet definition; real slots are created in rebuild().
            Panel(id = "WdAbilityRow", classes = "WdAbilityRow") {
                AbilitySlotView()
            } bind ::abilityRow
        }
    }

    override fun onLoad() {
        refresh()
    }

    private fun refresh() {
        val hero = Players.getLocalPlayerPortraitUnit()
        if (Entities.isValidEntity(hero)) {
            // The layout only changes on level-up / talent-learn, so run the full scan once per window
            // (tick 0), not at the 10Hz cadence the cooldown sweep needs.
            if (layoutScanTick == 0) {
                val current = buildSignature(hero)
                if (current != signature) {
                    signature = current
                    rebuild(hero)
                }
            }
            layoutScanTick = (layoutScanTick + 1) % AbilityBarConfig.LAYOUT_SCAN_TICKS
            // Silence is hero-wide: read once and let each slot reflect it.
            val silenced = Entities.isSilenced(hero)
            for (slot in slots) {
                slot.refreshCooldown(silenced)
            }
        }
        panorama.schedule(AbilityBarConfig.REFRESH_SECONDS) { refresh() }
    }

    /** Cheap fingerprint of "anything that would change the panel": hero level + ability points + every level. */
    private fun buildSignature(hero: EntityIndex): String {
        var sig = "${Entities.getLevel(hero)}:${Entities.getAbilityPoints(hero)}"
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
        slots.clear()
        val points = Entities.getAbilityPoints(hero)
        val heroLevel = Entities.getLevel(hero)
        val count = Entities.getAbilityCount(hero)

        // Talents need a whole-tree view before any one can be judged: each 10/15/20/25 tier is a
        // mutually-exclusive PAIR, so a talent's availability depends on whether its SIBLING at that
        // tier is taken — the engine's per-ability canAbilityBeUpgraded does not encode that pairing.
        // Collect talents first; render abilities/+stats inline.
        val talents = mutableListOf<Talent>()
        for (i in 0 until count) {
            val ability = Entities.getAbility(hero, i)
            if (!Entities.isValidEntity(ability)) continue
            val name = Abilities.getAbilityName(ability)
            if (name == "") continue
            val level = Abilities.getLevel(ability)
            if (GameUI.isAbilityDOTATalent(name)) {
                // The tier is the engine's own required hero level (10/15/20/25).
                talents.add(Talent(ability, name, level, Abilities.getHeroLevelRequiredToUpgrade(ability).toInt()))
            } else if (Abilities.isAttributeBonus(ability) || Abilities.isDisplayedAbility(ability)) {
                val maxLevel = Abilities.getMaxLevel(ability)
                // canAbilityBeUpgraded is the engine's own check (points, max level, upgradability).
                val learnResult = Abilities.canAbilityBeUpgraded(ability, false).toInt()
                val canUpgrade = points > 0 && learnResult == AbilityLearnResult.CAN_BE_UPGRADED.value
                slots.add(AbilitySlotView(abilityRow, i, ability, name, level, maxLevel, canUpgrade))
            }
        }

        // A tier whose choice is already locked in: the OTHER talent there can never be taken.
        val decidedTiers = talents.filter { it.level > 0 }.map { it.tier }
        for (talent in talents) {
            val state =
                when {
                    talent.level > 0 -> TalentState.TAKEN
                    // Choosable only if the tier is reached, a point is unspent, and neither half is taken.
                    points > 0 && heroLevel >= talent.tier && talent.tier !in decidedTiers -> TalentState.AVAILABLE
                    // Tier not reached, no point, or the sibling was chosen — greyed, not clickable.
                    else -> TalentState.LOCKED
                }
            addTalent(talent.ability, talent.name, state)
        }
        // Surface the talents only when there's a point to spend — otherwise a distracting box.
        talentColumn.visible = points > 0
    }

    private fun addTalent(
        ability: EntityIndex,
        name: String,
        state: TalentState,
    ) {
        val row = panorama.createPanel("Panel", talentColumn, "")
        row.addClass(AbilityBarStyles.TALENT_ROW)
        row.hittest = true
        when (state) {
            TalentState.TAKEN -> row.addClass(AbilityBarStyles.TALENT_TAKEN)
            TalentState.AVAILABLE -> {
                row.addClass(AbilityBarStyles.CAN_UPGRADE)
                // Only a choosable talent responds to a click — a taken/locked row does nothing.
                row.setPanelEvent(ON_ACTIVATE) { AbilityUpgrade.train(ability) }
            }
            TalentState.LOCKED -> row.addClass(AbilityBarStyles.LOCKED)
        }

        val lbl = panorama.createPanel("Label", row, "")
        lbl.addClass(AbilityBarStyles.TALENT_LABEL)
        GameUI.setupDOTATalentNameLabel(lbl, name)
    }
}

/** One talent for [AbilitiesPanel.rebuild]: the ability, its display name, current level, and its 10/15/20/25 tier. */
private data class Talent(
    val ability: EntityIndex,
    val name: String,
    val level: Int,
    val tier: Int,
)

/**
 * A talent's pick state: [TAKEN] (already chosen), [AVAILABLE] (a point can be spent here now), or
 * [LOCKED] (tier not reached, no unspent point, or the other talent in its tier was already picked).
 */
private enum class TalentState { TAKEN, AVAILABLE, LOCKED }
