package com.isycat.dotaaddon.panorama.abilitybar
import com.isycat.dota.types.EntityIndex
import com.isycat.dota.types.panorama.Abilities
import com.isycat.dota.types.panorama.AbilityLearnResult
import com.isycat.dota.types.panorama.Entities
import com.isycat.dota.types.panorama.GameUI
import com.isycat.dota.types.panorama.Label
import com.isycat.dota.types.panorama.Panel
import com.isycat.dota.types.panorama.Players
import com.isycat.dota.types.panorama.panorama
import com.isycat.ktox.panorama.dsl.ON_ACTIVATE
import com.isycat.ktox.panorama.dsl.PanoramaView

/**
 * Custom abilities + talents bar that replaces the stock action panel (hidden by [Manifest]). Reads the
 * currently-controlled unit's kit and shows an [AbilitySlotView] per displayed ability plus the +stats
 * bonus, and a proper tiered talent tree. Clicking upgrades via the engine's TRAIN_ABILITY order
 * ([AbilityUpgrade]).
 *
 * Reactive to the unit the player is looking at: a change of portrait unit (selecting a different unit)
 * rebuilds the whole bar IMMEDIATELY, and the per-ability signature includes each ability NAME so a
 * changed kit (Invoker invoking, Rubick stealing) rebuilds too — not just a level-up.
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

    /** Entity index of the unit the bar is currently built for; a change rebuilds instantly (see [refresh]). */
    private var displayedUnit = -1

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
            if (hero.value != displayedUnit) {
                // Unit switch: the whole kit differs, so rebuild NOW rather than waiting for the coarse
                // rescan below — the bar must track selection the instant the player clicks a new unit.
                displayedUnit = hero.value
                signature = buildSignature(hero)
                rebuild(hero)
            } else if (layoutScanTick == 0) {
                // Same unit: catch level-ups AND ability swaps (Invoker invoke, Rubick steal). The
                // signature includes every ability NAME, so a changed kit rebuilds even when the level
                // and point counts are unchanged.
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

    /**
     * Cheap fingerprint of "anything that would change the panel": hero level + ability points + every
     * ability's NAME and level. The name is what catches an in-place kit swap (Invoker's invoked slots,
     * a Rubick-stolen spell) that leaves the level count untouched.
     */
    private fun buildSignature(hero: EntityIndex): String {
        var sig = "${Entities.getLevel(hero)}:${Entities.getAbilityPoints(hero)}"
        val count = Entities.getAbilityCount(hero)
        for (i in 0 until count) {
            val ability = Entities.getAbility(hero, i)
            if (Entities.isValidEntity(ability)) {
                sig = "$sig:${Abilities.getAbilityName(ability)}@${Abilities.getLevel(ability)}"
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

        // Two passes over the kit: real abilities / +stats render inline into the ability row; talents are
        // collected in ability-LIST order, which in Dota IS tier order (see [buildTalentTree]).
        val talents = mutableListOf<Talent>()
        for (i in 0 until count) {
            val ability = Entities.getAbility(hero, i)
            if (!Entities.isValidEntity(ability)) continue
            val name = Abilities.getAbilityName(ability)
            if (name == "") continue
            val level = Abilities.getLevel(ability)
            if (GameUI.isAbilityDOTATalent(name)) {
                talents.add(Talent(ability, name, level))
            } else if (Abilities.isAttributeBonus(ability) || Abilities.isDisplayedAbility(ability)) {
                val maxLevel = Abilities.getMaxLevel(ability)
                // canAbilityBeUpgraded is the engine's own check (points, max level, upgradability).
                val learnResult = Abilities.canAbilityBeUpgraded(ability, false).toInt()
                val canUpgrade = points > 0 && learnResult == AbilityLearnResult.CAN_BE_UPGRADED.value
                slots.add(AbilitySlotView(abilityRow, i, ability, name, level, maxLevel, canUpgrade))
            }
        }

        buildTalentTree(talents, heroLevel, points)
    }

    /**
     * Render the talent tree as tier rows. Dota lists a hero's talents in ability order, two per tier, so
     * [talents] arrives as `[10L, 10R, 15L, 15R, 20L, 20R, 25L, 25R]`: pair `p` is tier `10 + p*5`, and the
     * two entries in a pair are each other's mutually-exclusive sibling. This ordering IS the tier — no
     * reliance on the engine's per-ability required-level, which does not report a usable tier for talents.
     */
    private fun buildTalentTree(
        talents: List<Talent>,
        heroLevel: Int,
        points: Int,
    ) {
        val pairCount = talents.size / 2
        // Show the tree whenever the unit actually has talents (a persistent read, like Dota's own tree).
        talentColumn.visible = pairCount > 0
        // Highest tier on top: the column is bottom-anchored and flows down, so adding the top pair first
        // puts tier 25 at the top and tier 10 nearest the bottom — the familiar talent-tree orientation.
        // Counting DOWN from the last pair never indexes below 0, so a non-standard talent count can't
        // reach an out-of-range entry.
        var pair = pairCount - 1
        while (pair >= 0) {
            val tier = TALENT_BASE_TIER + pair * TALENT_TIER_STEP
            addTierRow(tier, talents[pair * 2], talents[pair * 2 + 1], heroLevel, points)
            pair--
        }
    }

    /** One tier: `[ left talent ][ tier badge ][ right talent ]` — the classic branching read. */
    private fun addTierRow(
        tier: Int,
        left: Talent,
        right: Talent,
        heroLevel: Int,
        points: Int,
    ) {
        val row = panorama.createPanel("Panel", talentColumn, "")
        row.addClass(AbilityBarStyles.TALENT_TIER_ROW)
        addTalentButton(row, left, tier, right.level > 0, heroLevel, points)
        val badge = panorama.createPanel("Label", row, "") as Label
        badge.addClass(AbilityBarStyles.TALENT_TIER_BADGE)
        badge.text = "$tier"
        addTalentButton(row, right, tier, left.level > 0, heroLevel, points)
    }

    private fun addTalentButton(
        parent: Panel,
        talent: Talent,
        tier: Int,
        siblingTaken: Boolean,
        heroLevel: Int,
        points: Int,
    ) {
        val state =
            when {
                talent.level > 0 -> TalentState.TAKEN
                // The other half of this tier is locked in — this one can never be taken.
                siblingTaken -> TalentState.LOCKED
                // Tier not reached yet.
                heroLevel < tier -> TalentState.LOCKED
                // Reached, neither half taken, and a point is free to spend — choosable right now.
                points > 0 -> TalentState.AVAILABLE
                // Reached and open, but no unspent point at the moment.
                else -> TalentState.REACHED
            }
        val btn = panorama.createPanel("Panel", parent, "")
        btn.addClass(AbilityBarStyles.TALENT_BUTTON)
        btn.hittest = true
        when (state) {
            TalentState.TAKEN -> btn.addClass(AbilityBarStyles.TALENT_TAKEN)
            TalentState.AVAILABLE -> {
                btn.addClass(AbilityBarStyles.TALENT_AVAILABLE)
                // Only a choosable talent responds to a click — a taken/reached/locked button does nothing.
                btn.setPanelEvent(ON_ACTIVATE) { AbilityUpgrade.train(talent.ability) }
            }
            TalentState.REACHED -> btn.addClass(AbilityBarStyles.TALENT_REACHED)
            TalentState.LOCKED -> btn.addClass(AbilityBarStyles.TALENT_LOCKED)
        }
        val lbl = panorama.createPanel("Label", btn, "")
        lbl.addClass(AbilityBarStyles.TALENT_LABEL)
        GameUI.setupDOTATalentNameLabel(lbl, talent.name)
    }

    companion object {
        /** Dota's first talent tier is hero level 10, and each tier up is +5 levels (10/15/20/25). */
        private const val TALENT_BASE_TIER = 10
        private const val TALENT_TIER_STEP = 5
    }
}

/** One talent for [AbilitiesPanel.rebuild]: the ability, its display name, and current level (0 = not taken). */
private data class Talent(
    val ability: EntityIndex,
    val name: String,
    val level: Int,
)

/**
 * A talent's pick state: [TAKEN] (already chosen), [AVAILABLE] (a point can be spent here now — clickable),
 * [REACHED] (tier reached and open, but no unspent point right now), or [LOCKED] (tier not reached, or the
 * other talent in its tier was already picked).
 */
private enum class TalentState { TAKEN, AVAILABLE, REACHED, LOCKED }
