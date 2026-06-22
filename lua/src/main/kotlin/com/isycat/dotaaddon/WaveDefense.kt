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
import com.isycat.dota.types.lua.msg
import com.isycat.dota.types.lua.randomVector
import com.isycat.dotaaddon.shared.Announcement
import com.isycat.dotaaddon.shared.GameConfig
import com.isycat.dotaaddon.shared.WaveState
import com.isycat.ktox.dota.lib.onGameEvent

/**
 * "Survival Wave Defense" — the whole game loop.
 *
 * Showcases, end-to-end and strongly typed:
 *  - the game-mode think loop ([com.isycat.dota.types.lua.CBaseEntity.setContextThink])
 *  - typed game-event listening via the ktox-dota-lib [onGameEvent] wrapper
 *  - unit spawning ([createUnitByName]) and entity lookup ([entIndexToHScript])
 *  - pushing state to the Panorama HUD with [CustomGameEventManager]
 *  - shared, cross-target config/state ([GameConfig], [WaveState])
 */
object WaveDefense {
    private var wave = 0
    private var score = 0
    private var enemiesAlive = 0
    private var secondsToNext = GameConfig.START_DELAY_SECONDS
    private var gameOver = false

    fun start() {
        msg("[WaveDefense] starting up")
        registerKillListener()
        registerNovaCommand()

        // Drive the whole match from a single server-side think on the game-mode
        // entity. Returning the interval reschedules; returning null would stop.
        GameRules.gameModeEntity.setContextThink(
            "wd_think",
            { _ -> onThink() },
            GameConfig.THINK_INTERVAL_SECONDS,
        )
    }

    private fun onThink(): Float {
        val hero = PlayerResource.getSelectedHeroEntity(PlayerID(0))

        // Hero not in the world yet (pre-game) — idle until it exists.
        if (hero == null) {
            pushState(0)
            return GameConfig.THINK_INTERVAL_SECONDS
        }

        if (!hero.isAlive) {
            if (!gameOver) {
                gameOver = true
                announce("Game over! You survived to wave $wave with $score points.")
            }
            pushState(0)
            return GameConfig.THINK_INTERVAL_SECONDS
        }

        if (!gameOver) {
            secondsToNext -= 1
            if (secondsToNext <= 0) {
                wave += 1
                spawnWave(hero)
                announce("Wave $wave incoming!")
                secondsToNext = GameConfig.WAVE_INTERVAL_SECONDS
            }
        }

        pushState(hero.healthPercent)
        return GameConfig.THINK_INTERVAL_SECONDS
    }

    private fun spawnWave(hero: BaseNPCHero) {
        val count = GameConfig.enemiesForWave(wave)
        val center = hero.absOrigin
        for (i in 0 until count) {
            val spawnPos = center.add(randomVector(GameConfig.SPAWN_RADIUS))
            val unitName =
                if (i % 3 == 0) GameConfig.ENEMY_RANGED_UNIT else GameConfig.ENEMY_MELEE_UNIT
            createUnitByName(unitName, spawnPos, true, null, null, DOTATeam.BADGUYS)
            enemiesAlive += 1
        }
    }

    private fun registerKillListener() {
        onGameEvent(ENTITY_KILLED, null) { event ->
            val entity = entIndexToHScript(event.entindex_killed)
            if (entity != null) {
                val killed = entity as BaseNPC
                if (killed.teamNumber == DOTATeam.BADGUYS) {
                    score += GameConfig.SCORE_PER_KILL
                    if (enemiesAlive > 0) enemiesAlive -= 1
                }
            }
        }
    }

    /** Type "nova" in all-chat to detonate the showcase AoE around your hero. */
    private fun registerNovaCommand() {
        onGameEvent(PLAYER_CHAT, null) { event ->
            if (event.text.contains("nova")) {
                val caster = PlayerResource.getSelectedHeroEntity(event.playerid)
                if (caster != null && caster.isAlive) {
                    val hits = Nova.cast(caster)
                    announce("Nova hit $hits enemies!")
                }
            }
        }
    }

    private fun pushState(heroHpPercent: Int) {
        val state =
            WaveState(
                wave = wave,
                score = score,
                enemiesAlive = enemiesAlive,
                secondsToNext = if (secondsToNext < 0) 0 else secondsToNext,
                heroHpPercent = heroHpPercent,
                gameOver = gameOver,
            )
        CustomGameEventManager.sendServerToAllClients(GameConfig.EVENT_STATE, state)
    }

    private fun announce(text: String) {
        msg(text)
        CustomGameEventManager.sendServerToAllClients(GameConfig.EVENT_MESSAGE, Announcement(text))
    }
}
