package com.isycat.dotaaddon.panorama

import com.isycat.dota.types.EntityIndex
import com.isycat.dota.types.panorama.Abilities
import com.isycat.dota.types.panorama.AbilityLearnResult
import com.isycat.dota.types.panorama.Entities
import com.isycat.dota.types.panorama.GameUI
import com.isycat.dota.types.panorama.Panel
import com.isycat.dota.types.panorama.Players
import com.isycat.dota.types.panorama.panorama
import com.isycat.dotaaddon.shared.GameConfig
import com.isycat.ktox.panorama.dsl.ON_ACTIVATE
import com.isycat.ktox.panorama.dsl.PanoramaView

/**
 * Custom abilities + talents bar that replaces the stock action panel (hidden by [Manifest]).
 *
 * It reads the local hero's kit client-side and shows an [AbilitySlotView] per *displayed* ability
 * (hidden / scepter / shard / non-displayed innate entries are filtered out via `isDisplayedAbility`),
 * plus the +stats attribute bonus, and a row per talent. Clicking upgrades via the engine's
 * TRAIN_ABILITY order ([AbilityUpgrade]); upgradeable entries glow, unlearnable ones grey out, and
 * hovering an ability shows Dota's native tooltip. It rebuilds only when the level/point "signature"
 * changes, so the refresh loop is cheap.
 *
 * Each ability slot is a live-created, **snippet-backed** [AbilitySlotView] (created via
 * `BLoadLayoutSnippet`), not imperative `$.CreatePanel` calls — the snippet's XML `hittest` is what
 * makes the native tooltip work. Talent rows stay imperative (trivial label rows).
 */
@PanoramaView(snippet = false)
class AbilitiesPanel : Panel(id = "WdAbilities", type = "Panel") {
    lateinit var talentColumn: Panel
        private set
    lateinit var abilityRow: Panel
        private set

    private var signature = ""

    /** The live ability slots from the current [rebuild]; their cooldowns refresh every tick. */
    private val slots = mutableListOf<AbilitySlotView>()

    init {
        layout {
            Panel(id = "WdTalentColumn", classes = "WdTalentColumn") bind ::talentColumn
            // AbilitySlotView() here is purely declarative: snippet=true registers the
            // <snippet name="AbilitySlotView"> definition (and emits nothing inline). Real slots are
            // created live in rebuild() via BLoadLayoutSnippet.
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
            val current = buildSignature(hero)
            if (current != signature) {
                signature = current
                rebuild(hero)
            }
            // Cooldowns change every frame, so refresh them every tick (not just on rebuild).
            for (slot in slots) {
                slot.refreshCooldown()
            }
        }
        panorama.schedule(GameConfig.ABILITY_REFRESH_SECONDS) { refresh() }
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
            // Availability is the engine's own check (covers level requirement + max + talent-tier-pair
            // exclusivity); CAN_BE_UPGRADED == 0. Gate only on a spare point + that check — no recreated
            // level math.
            val learnResult = Abilities.canAbilityBeUpgraded(ability, false).toInt()
            val canUpgrade = points > 0 && learnResult == AbilityLearnResult.CAN_BE_UPGRADED.value
            // Talents and the attribute-bonus (+stats) are shown even though they aren't "displayed";
            // everything else must pass isDisplayedAbility (filters hidden / scepter / shard entries).
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
