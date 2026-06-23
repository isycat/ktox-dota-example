package com.isycat.dotaaddon

import com.isycat.dota.types.PlayerID
import com.isycat.dota.types.lua.BaseNPC
import com.isycat.dota.types.lua.BaseNPCHero
import com.isycat.dota.types.lua.CustomGameEventManager
import com.isycat.dota.types.lua.DOTATeam
import com.isycat.dota.types.lua.ENTITY_KILLED
import com.isycat.dota.types.lua.GameRules
import com.isycat.dota.types.lua.PLAYER_CHAT
import com.isycat.dota.types.lua.PlayerResource
import com.isycat.dota.types.lua.createUnitByName
import com.isycat.dota.types.lua.entIndexToHScript
import com.isycat.dota.types.lua.Vector
import com.isycat.dota.types.lua.randomVector
import com.isycat.dotaaddon.Nova
import com.isycat.dotaaddon.shared.Announcement
import com.isycat.dotaaddon.shared.GameConfig
import com.isycat.dotaaddon.shared.WaveState
import com.isycat.ktox.dota.lib.onGameEvent

/**
 * "Survival Wave Defense" — the whole game loop.
 *
 * NOTE ON STYLE: every reference to a member of this `object` is written
 * fully-qualified (`WaveDefense.wave`, `WaveDefense.onThink()`, …) rather than
 * relying on implicit `this`. The Lua transpiler currently mis-lowers implicit
 * `this` inside an `object` (emitting a nil `self` and unqualified global
 * calls); explicit qualification is the working pattern (matches the ktox
 * `EventLog` example). See BUG-object-implicit-this.md.
 *
 * Showcases: the game-mode think loop (`setContextThink`), typed game-event
 * listening (`onGameEvent`), unit spawning, entity lookup, pushing state to the
 * Panorama HUD (`CustomGameEventManager`), and shared cross-target state
 * (`GameConfig`, `WaveState`).
 */
object WaveDefense {
    var wave = 0
    var score = 0
    var enemiesAlive = 0
    var secondsToNext = GameConfig.START_DELAY_SECONDS
    var gameOver = false

    /**
     * Registers event listeners. Safe to call at script-load time (from main()).
     * Does NOT touch the game-mode entity, which doesn't exist yet at load.
     */
    fun start() {
        println("WaveDefense starting up")
        WaveDefense.registerKillListener()
        WaveDefense.registerNovaCommand()
        WaveDefense.registerRestartCommand()
    }

    /**
     * Starts the wave-spawning think loop. Must run from Activate(), not main():
     * `GameRules:GetGameModeEntity()` is nil at script load and only becomes valid
     * once the engine has activated the game mode.
     */
    fun beginThink() {
        GameRules.gameModeEntity.setContextThink(
            "wd_think",
            { _ -> WaveDefense.onThink() },
            GameConfig.THINK_INTERVAL_SECONDS,
        )
    }

    fun onThink(): Float {
        val hero = PlayerResource.getSelectedHeroEntity(PlayerID(0))

        if (hero == null) {
            WaveDefense.pushState(0)
            return GameConfig.THINK_INTERVAL_SECONDS
        }

        if (!hero.isAlive) {
            if (!WaveDefense.gameOver) {
                WaveDefense.gameOver = true
                WaveDefense.announce(
                    "Game over! You survived to wave " +
                        WaveDefense.wave + " with " + WaveDefense.score +
                        " points. Type 'restart' in chat to play again.",
                )
            }
            WaveDefense.pushState(0)
            return GameConfig.THINK_INTERVAL_SECONDS
        }

        if (!WaveDefense.gameOver) {
            WaveDefense.secondsToNext = WaveDefense.secondsToNext - 1
            if (WaveDefense.secondsToNext <= 0) {
                WaveDefense.wave = WaveDefense.wave + 1
                WaveDefense.spawnWave(hero)
                WaveDefense.announce("Wave " + WaveDefense.wave + " incoming!")
                WaveDefense.secondsToNext = GameConfig.WAVE_INTERVAL_SECONDS
            }
        }

        WaveDefense.pushState(hero.healthPercent)
        return GameConfig.THINK_INTERVAL_SECONDS
    }

    fun spawnWave(hero: BaseNPCHero) {
        val count = GameConfig.enemiesForWave(WaveDefense.wave)
        val center = hero.absOrigin
        for (i in 0 until count) {
            val spawnPos = center + randomVector(GameConfig.SPAWN_RADIUS)
            val unitName =
                if (i % 3 == 0) GameConfig.ENEMY_RANGED_UNIT else GameConfig.ENEMY_MELEE_UNIT
            createUnitByName(unitName, spawnPos, true, null, null, DOTATeam.BADGUYS)
            WaveDefense.enemiesAlive = WaveDefense.enemiesAlive + 1
        }
    }

    fun registerKillListener() {
        onGameEvent(ENTITY_KILLED, null) { event ->
            val entity = entIndexToHScript(event.entindex_killed)
            if (entity != null) {
                val killed = entity as BaseNPC
                if (killed.teamNumber == DOTATeam.BADGUYS) {
                    WaveDefense.score = WaveDefense.score + GameConfig.SCORE_PER_KILL
                    if (WaveDefense.enemiesAlive > 0) {
                        WaveDefense.enemiesAlive = WaveDefense.enemiesAlive - 1
                    }
                }
            }
        }
    }

    /** Type "nova" in all-chat to detonate the showcase AoE around your hero. */
    fun registerNovaCommand() {
        onGameEvent(PLAYER_CHAT, null) { event ->
            if (event.text.contains("nova")) {
                val caster = PlayerResource.getSelectedHeroEntity(event.playerid)
                if (caster != null && caster.isAlive) {
                    val hits = Nova.cast(caster)
                    WaveDefense.announce("Nova hit " + hits + " enemies!")
                }
            }
        }
    }

    /** Type "restart" in all-chat to reset the run: respawns your hero and restarts the waves. */
    fun registerRestartCommand() {
        onGameEvent(PLAYER_CHAT, null) { event ->
            if (event.text.contains("restart")) {
                val hero = PlayerResource.getSelectedHeroEntity(event.playerid)
                if (hero != null) WaveDefense.restart(hero)
            }
        }
    }

    fun restart(hero: BaseNPCHero) {
        WaveDefense.wave = 0
        WaveDefense.score = 0
        WaveDefense.enemiesAlive = 0
        WaveDefense.secondsToNext = GameConfig.START_DELAY_SECONDS
        WaveDefense.gameOver = false
        if (!hero.isAlive) hero.respawnHero(false, false)
        WaveDefense.announce("New run! Survive the waves.")
    }

    fun pushState(heroHpPercent: Int) {
        val state =
            WaveState(
                wave = WaveDefense.wave,
                score = WaveDefense.score,
                enemiesAlive = WaveDefense.enemiesAlive,
                secondsToNext = if (WaveDefense.secondsToNext < 0) 0 else WaveDefense.secondsToNext,
                heroHpPercent = heroHpPercent,
                gameOver = WaveDefense.gameOver,
            )
        CustomGameEventManager.sendServerToAllClients(GameConfig.EVENT_STATE, state)
    }

    fun announce(text: String) {
        println(text)
        CustomGameEventManager.sendServerToAllClients(GameConfig.EVENT_MESSAGE, Announcement(text))
    }
}
