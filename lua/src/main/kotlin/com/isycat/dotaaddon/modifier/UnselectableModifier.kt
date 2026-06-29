package com.isycat.dotaaddon.modifier

import com.isycat.dota.types.lua.ModifierLua
import com.isycat.dota.types.lua.Modifierstate
import com.isycat.ktox.dota.Dota2Class

/**
 * A hidden, permanent modifier that makes its parent unit unselectable — applied to the Ancient
 * objective so a stray click can't select it (and steal the player's hero selection).
 *
 * All-Kotlin → Lua: `@Dota2Class` lowers it to the `UnselectableModifier = class({})` idiom and
 * ktox-dota auto-emits the engine `LinkLuaModifier` registration (no hand-written Lua). `CheckState`
 * returns the engine state table `{ [MODIFIER_STATE_UNSELECTABLE] = true }`; the health bar is left
 * on so players can still read the objective's HP.
 */
@Dota2Class
class UnselectableModifier : ModifierLua {
    override val isHidden: Boolean get() = true
    override val isPurgable: Boolean get() = false

    override fun checkState(): Map<Modifierstate, Boolean> = mapOf(Modifierstate.UNSELECTABLE to true)
}
