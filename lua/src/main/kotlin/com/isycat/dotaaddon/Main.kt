package com.isycat.dotaaddon

import com.isycat.dota.types.PlayerID
import com.isycat.dota.types.lua.CustomUI
import com.isycat.dota.types.lua.HeroList
import com.isycat.dota.types.lua.PLAYER_CHAT
import com.isycat.dota.types.lua.PlayerController
import com.isycat.dota.types.lua.PlayerResource
import com.isycat.dota.types.lua.Vector
import com.isycat.dota.types.lua.createUnitByName
import com.isycat.dota.types.lua.msg
import com.isycat.dotaaddon.shared.AddonInfo
import com.isycat.ktox.dota.lib.onGameEvent

fun main() {
    msg(AddonInfo.getWelcomeMessage())

    try {
        CustomUI.dynamicHudCreate(PlayerID(1), "", "", "")
    } catch (_: Exception) {
        println("Expected!  Failed to create dynamic HUD with garbage params")
    }

    val hero = HeroList.getHero(0)
    if (hero != null) {
        msg("First hero found: ${hero.unitName}")
        val hp = hero.health
        hero.health += 25
        msg("Hero health is now $hp")
    }

    fun getActivePlayers(): List<PlayerController> =
        (0..PlayerResource.playerCount)
            .asSequence()
            .filter { PlayerResource.isValidPlayer(it) }
            .mapNotNull { PlayerResource.getPlayer(PlayerID(it)) }
            .filter { it.isPlayer }
            .filter { it.isAlive }
            .filter { it.assignedHero.isAlive }
            .toList()

    // NOTE: `context` (null) is passed explicitly to work around a transpiler bug where
    // skipped default args aren't filled in, shifting the trailing lambda into the wrong
    // slot. See BUG-onGameEvent-default-arg.md. Revert to `onGameEvent(PLAYER_CHAT) { ... }`
    // once the transpiler fix lands.
    onGameEvent(PLAYER_CHAT, null) { event ->
        println(event)
        println(event.toString())
        println("Player ${event.playerid} said: ${event.text}")
        println("HELLO:) " + getActivePlayers().size)
        getActivePlayers().forEach {
            println("alive: ${it.isAlive}")
            println("PlayerResource: $PlayerResource")
            println("hero: ${it.assignedHero.unitName}")
            println("heroAlive: ${it.assignedHero.isAlive}")
            println("isPlayer: ${it.isPlayer}")
            println("team: ${it.team}")
        }
        val player0 = PlayerResource.getPlayer(event.playerid)!!
        val p0Team = player0.team
        createUnitByName(
            "npc_dota_hero_lich",
            Vector(0f, 0f, 0f),
            true,
            player0.assignedHero,
            player0,
            p0Team,
        )
        createUnitByName(
            "npc_dota_hero_antimage",
            Vector(0f, 0f, 0f),
            true,
            player0.assignedHero,
            player0,
            p0Team,
        )
        createUnitByName(
            "npc_dota_hero_antimage",
            Vector(0f, 0f, 0f),
            true,
            player0,
            player0,
            PlayerResource.getPlayer(event.playerid)?.team!!,
        )
    }
}
