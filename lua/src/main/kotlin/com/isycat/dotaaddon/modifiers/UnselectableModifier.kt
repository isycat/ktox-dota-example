package com.isycat.dotaaddon.modifiers

import com.isycat.dota.types.lua.ModifierLua
import com.isycat.dota.types.lua.Modifierstate
import com.isycat.ktox.dota.Dota2Class

/**
 * A hidden, permanent modifier that makes its parent unit unselectable — applied to the Ancient objective
 * so a stray click can't steal the player's hero selection. [Dota2Class] links the modifier and ktox-dota
 * auto-emits its LinkLuaModifier registration (no hand-written Lua).
 */
@Dota2Class
class UnselectableModifier : ModifierLua {
    override val isHidden: Boolean get() = true
    override val isPurgable: Boolean get() = false

    override fun checkState(): Map<Modifierstate, Boolean> = mapOf(Modifierstate.UNSELECTABLE to true)
}
