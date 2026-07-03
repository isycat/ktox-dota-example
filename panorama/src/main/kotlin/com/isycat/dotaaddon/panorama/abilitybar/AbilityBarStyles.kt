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

    /** A talent row already taken (dimmed, no click highlight). */
    const val TALENT_TAKEN = "WdTalentTaken"

    /** One talent row in the talent column. */
    const val TALENT_ROW = "WdTalentRow"

    /** The talent row's name label. */
    const val TALENT_LABEL = "WdTalentLabel"
}
