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
        val count = Entities.getAbilityCount(hero)
        for (i in 0 until count) {
            val ability = Entities.getAbility(hero, i)
            if (!Entities.isValidEntity(ability)) continue
            val name = Abilities.getAbilityName(ability)
            if (name == "") continue
            val level = Abilities.getLevel(ability)
            val maxLevel = Abilities.getMaxLevel(ability)
            // canAbilityBeUpgraded is the engine's own check (level requirement, max, talent-tier exclusivity).
            val learnResult = Abilities.canAbilityBeUpgraded(ability, false).toInt()
            val canUpgrade = points > 0 && learnResult == AbilityLearnResult.CAN_BE_UPGRADED.value
            // Talents and +stats are shown even though they aren't "displayed"; everything else must pass
            // isDisplayedAbility (filters hidden / scepter / shard entries).
            if (GameUI.isAbilityDOTATalent(name)) {
                addTalent(ability, name, level, canUpgrade)
            } else if (Abilities.isAttributeBonus(ability) || Abilities.isDisplayedAbility(ability)) {
                val slot = AbilitySlotView(abilityRow)
                slot.configure(i, ability, name, level, maxLevel, canUpgrade)
                slots.add(slot)
            }
        }
        // Surface the talents only when there's a point to spend — otherwise a distracting box.
        talentColumn.visible = points > 0
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

        row.setPanelEvent(ON_ACTIVATE) { AbilityUpgrade.train(ability) }
    }
}
