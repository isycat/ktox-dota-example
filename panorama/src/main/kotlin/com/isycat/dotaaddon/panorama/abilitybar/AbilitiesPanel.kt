package com.isycat.dotaaddon.panorama.abilitybar
import com.isycat.dota.types.EntityIndex
import com.isycat.dota.types.panorama.Abilities
import com.isycat.dota.types.panorama.AbilityLearnResult
import com.isycat.dota.types.panorama.DOTA_ABILITY_CHANGED
import com.isycat.dota.types.panorama.DOTA_CREATURE_GAINED_LEVEL
import com.isycat.dota.types.panorama.DOTA_PLAYER_LEARNED_ABILITY
import com.isycat.dota.types.panorama.DOTA_PLAYER_UPDATE_QUERY_UNIT
import com.isycat.dota.types.panorama.DOTA_PLAYER_UPDATE_SELECTED_UNIT
import com.isycat.dota.types.panorama.Entities
import com.isycat.dota.types.panorama.GameEvents
import com.isycat.dota.types.panorama.GameUI
import com.isycat.dota.types.panorama.Label
import com.isycat.dota.types.panorama.Panel
import com.isycat.dota.types.panorama.Players
import com.isycat.dota.types.panorama.panorama
import com.isycat.dotaaddon.shared.WdTokens
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

    /** Holds the tier rows; collapsed behind [talentTab] until revealed (hover or click — see config). */
    lateinit var talentTree: Panel
        private set

    /** The always-visible handle that reveals [talentTree]. */
    lateinit var talentTab: Label
        private set

    /** Full-screen click-catcher behind the open tree (click mode); a click on it closes the tree. */
    lateinit var talentBackdrop: Panel
        private set
    lateinit var abilityRow: Panel
        private set

    /** Whether the click-toggled tree is currently open (unused in hover-reveal mode). */
    private var talentsOpen = false

    private var signature = ""

    /** Entity index of the unit the bar is currently built for; a change rebuilds instantly (see [refresh]). */
    private var displayedUnit = -1

    /** Counts refresh ticks so the heavier layout scan runs coarser than the per-tick cooldown sweep (see [refresh]). */
    private var layoutScanTick = 0

    /** The live ability slots from the current [rebuild]; their cooldowns refresh every tick. */
    private val slots = mutableListOf<AbilitySlotView>()

    init {
        layout {
            // #WdAbilities has no flow, so child order is purely PAINT order. The ability row paints first
            // (bottom), then the backdrop (covers everything while open, so a click elsewhere dismisses the
            // tree), then the talent column on top (its tab + tree stay clickable above the backdrop).
            // This AbilitySlotView() only registers the snippet definition; real slots are created in rebuild().
            Panel(id = "WdAbilityRow", classes = "WdAbilityRow") {
                AbilitySlotView()
            } bind ::abilityRow
            Panel(id = "WdTalentBackdrop", classes = "WdTalentBackdrop", hittest = true) bind ::talentBackdrop
            // hittest=true so a hover over any part of the column (tab or the revealed tree) keeps the
            // CSS `:hover` reveal latched; the tree flows ABOVE the tab (bottom-anchored, grows upward).
            Panel(id = "WdTalentColumn", classes = "WdTalentColumn", hittest = true) {
                Panel(id = "WdTalentTree", classes = "WdTalentTree") bind ::talentTree
                Label(id = "WdTalentTab", classes = "WdTalentTab") bind ::talentTab
            } bind ::talentColumn
        }
    }

    override fun onLoad() {
        // Event-driven rebuilds (pocket's approach): react the instant selection, abilities, or level
        // change instead of waiting for the poll in [refresh]. Each handler just asks for a re-sync; it's
        // deferred one frame because these engine events fire BEFORE the state they signal has settled —
        // reading the selected unit synchronously in the handler returns the OLD one.
        GameEvents.subscribe(DOTA_PLAYER_UPDATE_SELECTED_UNIT) { scheduleSync() }
        GameEvents.subscribe(DOTA_PLAYER_UPDATE_QUERY_UNIT) { scheduleSync() }
        GameEvents.subscribe(DOTA_ABILITY_CHANGED) { scheduleSync() } // Invoker invoke, Rubick steal
        GameEvents.subscribe(DOTA_PLAYER_LEARNED_ABILITY) { scheduleSync() } // spent a point / took a talent
        GameEvents.subscribe(DOTA_CREATURE_GAINED_LEVEL) { scheduleSync() } // level-up freed a point / tier

        // Wire the talent tab's reveal behaviour once (the tree starts collapsed behind it).
        talentTab.text = panorama.localize("#${WdTokens.TALENTS}")
        if (AbilityBarConfig.TALENT_REVEAL_ON_HOVER) {
            // Pure-CSS reveal: the tree shows whenever the column is hovered (see _talents.scss). No backdrop.
            talentColumn.addClass(AbilityBarStyles.TALENT_HOVER_REVEAL)
        } else {
            // Click the tab to toggle; a click on the backdrop (anywhere else) closes.
            talentTab.setPanelEvent(ON_ACTIVATE) { toggleTalents() }
            talentBackdrop.setPanelEvent(ON_ACTIVATE) { closeTalents() }
        }
        refresh()
    }

    /**
     * Click-reveal mode: flip the tree open/closed. The open class lives on the #WdAbilities root (`this`)
     * so one class governs BOTH the tree and the full-screen backdrop, which are siblings under the root.
     */
    private fun toggleTalents() {
        if (talentsOpen) closeTalents() else openTalents()
    }

    private fun openTalents() {
        talentsOpen = true
        addClass(AbilityBarStyles.TALENT_OPEN)
    }

    private fun closeTalents() {
        talentsOpen = false
        removeClass(AbilityBarStyles.TALENT_OPEN)
    }

    /** Re-check the current unit next frame — the triggering event's state isn't settled yet this frame. */
    private fun scheduleSync() {
        panorama.schedule(0f) { syncNow() }
    }

    /** Rebuild immediately if the controlled unit — or its kit — changed since the last build. */
    private fun syncNow() {
        val hero = Players.getLocalPlayerPortraitUnit()
        if (!Entities.isValidEntity(hero)) return
        if (hero.value != displayedUnit) {
            displayedUnit = hero.value
            signature = buildSignature(hero)
            rebuild(hero)
        } else {
            val current = buildSignature(hero)
            if (current != signature) {
                signature = current
                rebuild(hero)
            }
        }
    }

    private fun refresh() {
        val hero = Players.getLocalPlayerPortraitUnit()
        if (Entities.isValidEntity(hero)) {
            // Safety net for anything the events miss (e.g. an Invoker invoked-spell level shifting with an
            // orb level): a cheap unit-switch check every tick, and the heavier full-kit rescan only every
            // ~0.5s. The event subscriptions above are what make the common cases instant.
            if (hero.value != displayedUnit || layoutScanTick == 0) {
                syncNow()
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
        // Only the tier rows are transient — the tab + tree container in [talentColumn] persist.
        talentTree.removeAndDeleteChildren()
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
        // Show the talents column (tab + collapsed tree) only when the unit actually has talents.
        talentColumn.visible = pairCount > 0
        // Highest tier on top: the column is bottom-anchored and flows down, so adding the top pair first
        // puts tier 25 at the top and tier 10 nearest the bottom — the familiar talent-tree orientation.
        // Counting DOWN from the last pair never indexes below 0, so a non-standard talent count can't
        // reach an out-of-range entry.
        var anyAvailable = false
        var pair = pairCount - 1
        while (pair >= 0) {
            val tier = TALENT_BASE_TIER + pair * TALENT_TIER_STEP
            if (addTierRow(tier, talents[pair * 2], talents[pair * 2 + 1], heroLevel, points)) {
                anyAvailable = true
            }
            pair--
        }
        // Glow the tab when a talent can be spent right now, so the collapsed tree still advertises it.
        if (anyAvailable) {
            talentTab.addClass(AbilityBarStyles.TALENT_TAB_ALERT)
        } else {
            talentTab.removeClass(AbilityBarStyles.TALENT_TAB_ALERT)
        }
    }

    /** One tier: `[ left talent ][ tier badge ][ right talent ]`. Returns whether either half is available. */
    private fun addTierRow(
        tier: Int,
        left: Talent,
        right: Talent,
        heroLevel: Int,
        points: Int,
    ): Boolean {
        val row = panorama.createPanel("Panel", talentTree, "")
        row.addClass(AbilityBarStyles.TALENT_TIER_ROW)
        val leftAvailable = addTalentButton(row, left, tier, right.level > 0, heroLevel, points)
        val badge = panorama.createPanel("Label", row, "") as Label
        badge.addClass(AbilityBarStyles.TALENT_TIER_BADGE)
        badge.text = "$tier"
        val rightAvailable = addTalentButton(row, right, tier, left.level > 0, heroLevel, points)
        return leftAvailable || rightAvailable
    }

    /** Renders one talent button; returns true when it is AVAILABLE (a point can be spent on it now). */
    private fun addTalentButton(
        parent: Panel,
        talent: Talent,
        tier: Int,
        siblingTaken: Boolean,
        heroLevel: Int,
        points: Int,
    ): Boolean {
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
                // Picking one dismisses the tree (the rebuild that follows will re-open nothing).
                btn.setPanelEvent(ON_ACTIVATE) {
                    AbilityUpgrade.train(talent.ability)
                    closeTalents()
                }
            }
            TalentState.REACHED -> btn.addClass(AbilityBarStyles.TALENT_REACHED)
            TalentState.LOCKED -> btn.addClass(AbilityBarStyles.TALENT_LOCKED)
        }
        val lbl = panorama.createPanel("Label", btn, "")
        lbl.addClass(AbilityBarStyles.TALENT_LABEL)
        GameUI.setupDOTATalentNameLabel(lbl, talent.name)
        return state == TalentState.AVAILABLE
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
