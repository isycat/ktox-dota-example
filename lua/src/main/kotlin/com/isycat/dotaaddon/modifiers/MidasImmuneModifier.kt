package com.isycat.dotaaddon.modifiers

import com.isycat.dota.types.lua.ModifierLua
import com.isycat.ktox.dota.Dota2Class

/**
 * A hidden, permanent marker modifier applied to elite creeps so the server-side order filter can reject a
 * Hand of Midas cast on them (instant-killing a tanky elite for gold would trivialise the run). It carries
 * no state or behaviour — it exists purely as an engine-queryable flag ON THE UNIT, which the filter checks
 * with `hasModifier`. That's the engine-native way to mark units, rather than mirroring them in a Kotlin
 * collection that has to be hand-maintained (added on spawn, removed on death, cleared on restart) and can
 * leak. (Bosses are real heroes, which Midas can't target at all, so only elites need the marker.)
 *
 * `@Dota2Class` lowers it to the `MidasImmuneModifier = class({})` idiom and auto-emits its `LinkLuaModifier`
 * registration; applying it via the typed `addNewModifier(MidasImmuneModifier::class, …)` overload is what
 * imports + registers it.
 */
@Dota2Class
class MidasImmuneModifier : ModifierLua {
    override val isHidden: Boolean get() = true
    override val isPurgable: Boolean get() = false
}
