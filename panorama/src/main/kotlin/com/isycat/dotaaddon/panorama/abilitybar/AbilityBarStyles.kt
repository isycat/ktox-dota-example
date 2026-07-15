package com.isycat.dotaaddon.panorama.abilitybar

/**
 * The ability bar's CSS class names (defined in `_abilities.scss`), named once so runtime
 * add/removeClass logic never carries string literals. Layout-DSL `classes =` attributes keep
 * their literals — they are the structural definition the snippet XML is compiled from.
 */
object AbilityBarStyles {
    /** The live root of one ability slot. */
    const val SLOT = "WdAbilitySlot"

    /** Marks the hidden +stats bonus slot (narrower icon treatment). */
    const val STATS_SLOT = "WdStatsSlot"

    /** A point can be spent here right now (gold highlight). */
    const val CAN_UPGRADE = "WdCanUpgrade"

    /** Unlearned and not learnable right now (greyed out). */
    const val LOCKED = "WdLocked"

    /** Dim wash while the owner can't afford the mana cost. */
    const val NO_MANA = "WdNoMana"

    /** Icon border while a toggle ability is on. */
    const val TOGGLED_ON = "WdToggledOn"

    /** Icon badge while auto-cast is on. */
    const val AUTOCAST_ON = "WdAutocastOn"

    /** The always-visible tab that reveals the talent tree (hover or click). */
    const val TALENT_TAB = "WdTalentTab"

    /** On the tab while at least one talent can be learned right now — a glowing "spend me" cue. */
    const val TALENT_TAB_ALERT = "WdTalentTabAlert"

    /** On the #WdAbilities root while the tree is click-toggled open (reveals the tree). */
    const val TALENT_OPEN = "WdTalentOpen"

    /** The collapsible container that holds the tier rows (hidden until revealed). */
    const val TALENT_TREE = "WdTalentTree"

    /** On the column while in hover-reveal mode — CSS shows the tree on `:hover`. */
    const val TALENT_HOVER_REVEAL = "WdTalentHoverReveal"

    /** One tier's row in the talent tree: `[ left talent ][ tier badge ][ right talent ]`. */
    const val TALENT_TIER_ROW = "WdTalentTierRow"

    /** The centre badge in a tier row showing the required level (10/15/20/25). */
    const val TALENT_TIER_BADGE = "WdTalentTierBadge"

    /** One talent button (half of a tier row). */
    const val TALENT_BUTTON = "WdTalentButton"

    /** A talent already taken (filled/highlighted). */
    const val TALENT_TAKEN = "WdTalentTaken"

    /** A talent that can be learned right now (glows, clickable). */
    const val TALENT_AVAILABLE = "WdTalentAvailable"

    /** A talent whose tier is reached and still open, but with no unspent point to spend on it now. */
    const val TALENT_REACHED = "WdTalentReached"

    /** A talent that can't be taken: tier not reached, or its sibling was already picked (dimmed). */
    const val TALENT_LOCKED = "WdTalentLocked"

    /** The talent button's name label. */
    const val TALENT_LABEL = "WdTalentLabel"
}
