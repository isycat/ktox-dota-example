package com.isycat.dotaaddon

import com.isycat.dota.lua.CustomUI
import com.isycat.dota.lua.HeroList
import com.isycat.dota.lua.PlayerChat
import com.isycat.dota.lua.PlayerController
import com.isycat.dota.lua.PlayerID
import com.isycat.dota.lua.PlayerResource
import com.isycat.dota.lua.Vector
import com.isycat.dota.lua.createUnitByName
import com.isycat.dota.lua.listenToGameEvent
import com.isycat.dota.lua.msg

fun getActivePlayers(): List<PlayerController> =
    (0..PlayerResource.playerCount)
        .filter { PlayerResource.isValidPlayer(it) }
        .map { PlayerID(it) }
        .mapNotNull { PlayerResource.getPlayer(it) }
        .filter { it.isPlayer }
        .filter { it.isAlive }
        .filter { it.assignedHero.isAlive }

fun main() {
    msg("Hello from Kotlin Dota 2 Lua!")

    try {
        CustomUI.dynamicHudCreate(PlayerID(1), "", "", "")
    } catch (e: Exception) {
        println("Failed to create dynamic HUD. Expected! bs params")
    }

    val hero = HeroList.getHero(0)
    if (hero != null) {
        msg("First hero found: ${hero.unitName}")
        val hp = hero.health
        hero.health += 25
        msg("Hero health is now $hp")
    }

    listenToGameEvent(
        "player_chat",
        { event ->
            val zxc = event as PlayerChat
            println("Player ${event.playerid} said: ${event.text}")
            println("HELLO:) " + getActivePlayers().size)
            (0..PlayerResource.playerCount)
                .filter { PlayerResource.isValidPlayer(it) }
                .map { PlayerID(it) }
                .map { PlayerResource.getPlayer(it) }
                .filter { it != null }
                .map { it as PlayerController }
                .forEach {
                    println("alive: ${it.isAlive}")
                    println("PlayerResource: $PlayerResource")
                    println("hero: ${it.assignedHero.unitName}")
                    println("heroAlive: ${it.assignedHero.isAlive}")
                    println("isPlayer: ${it.isPlayer}")
                    println("team: ${it.team}")
                }
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
                player0?.team!!,
            )
        },
        null,
    )
}

