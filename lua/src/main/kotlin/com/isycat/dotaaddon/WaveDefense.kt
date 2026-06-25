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
import com.isycat.dota.types.lua.Vector
import com.isycat.dota.types.lua.createUnitByName
import com.isycat.dota.types.lua.entIndexToHScript
import com.isycat.dota.types.lua.randomVector
import com.isycat.dota.types.lua.worldMaxX
import com.isycat.dota.types.lua.worldMaxY
import com.isycat.dota.types.lua.worldMinX
import com.isycat.dota.types.lua.worldMinY
import com.isycat.dotaaddon.shared.Announcement
import com.isycat.dotaaddon.shared.EliteAlert
import com.isycat.dotaaddon.shared.GameConfig
import com.isycat.dotaaddon.shared.WaveState
import com.isycat.ktox.dota.lib.onGameEvent
import kotlin.math.ceil

/**
 * "Survival Wave Defense" — the whole game loop.
 *
 * Showcases: the game-mode think loop (`setContextThink`), typed game-event listening
 * (`onGameEvent`), unit spawning, entity lookup, pushing state to the Panorama HUD
 * (`CustomGameEventManager`), and shared cross-target state (`GameConfig`, `WaveState`).
 */
object WaveDefense {
    private var wave = 0
    private var score = 0
    private var enemiesAlive = 0
    private var secondsToNext = GameConfig.START_DELAY_SECONDS
    private var gameOver = false

    /** Every enemy spawned this run, so [restart] can clear the board for a fresh attempt. */
    private val spawnedEnemies = mutableListOf<BaseNPC>()

    /** The current boss (boss waves only) or null; the HUD boss bar follows its HP via pushed state. */
    private var boss: BaseNPC? = null
    private var bossName = ""

    /** The Ancient at the map centre that the enemies march on; if it dies the run ends. */
    private var ancient: BaseNPC? = null

    /** Set once the first attempt has been initialised (starting gold granted, board clean). */
    private var started = false

    /** Rolling wave-spawn batch state (see [spawnWave] / [spawnBatch]). */
    private var spawnHero: BaseNPCHero? = null
    private var spawnCountRemaining = 0
    private var spawnBatchIndex = 0
    private var spawnBatchesLeft = 0

    /**
     * Registers event listeners. Safe to call at script-load time (from main()).
     * Does NOT touch the game-mode entity, which doesn't exist yet at load.
     */
    fun start() {
        println("WaveDefense starting up")
        registerKillListener()
        registerNovaCommand()
        registerRestartListener()
    }

    /**
     * Starts the wave-spawning think loop. Must run from Activate(), not main():
     * `GameRules:GetGameModeEntity()` is nil at script load and only becomes valid
     * once the engine has activated the game mode.
     */
    fun beginThink() {
        // Items bought anywhere go straight to the inventory (filling empty slots) instead of being
        // parked in the stash — no manual stash juggling in a single-arena survival mode.
        GameRules.setUseUniversalShopMode(true)
        GameRules.gameModeEntity.setContextThink(
            "wd_think",
            { _ -> onThink() },
            GameConfig.THINK_INTERVAL_SECONDS,
        )
    }

    private fun onThink(): Float {
        val hero = PlayerResource.getSelectedHeroEntity(PlayerID(0))

        if (hero == null) {
            pushState()
            return GameConfig.THINK_INTERVAL_SECONDS
        }

        // Initialise the very first attempt the same as a restart: grant starting gold once the hero
        // exists. (Subsequent runs are reset by restart().)
        if (!started) {
            started = true
            setStartingGold(PlayerID(0))
            spawnAncient()
        }
        // Keep the inventory topped up from the stash (universal shop mode handles new purchases).
        pullStashItems(hero)

        if (!hero.isAlive) {
            if (!gameOver) {
                gameOver = true
                // Single life per run: lock the hero dead until "Play Again" so it can't auto-respawn
                // and walk around behind the game-over screen.
                hero.timeUntilRespawn = GameConfig.GAMEOVER_RESPAWN_LOCK_SECONDS
                announce(
                    "Game over! You survived to wave " +
                        wave + " with " + score + " points.",
                )
            }
            pushState()
            return GameConfig.THINK_INTERVAL_SECONDS
        }

        // The Ancient falling is a second lose condition, alongside the hero dying.
        val standingAncient = ancient
        if (!gameOver && standingAncient != null && (standingAncient.isNull || !standingAncient.isAlive)) {
            gameOver = true
            announce(
                "The Ancient has fallen! You survived to wave " +
                    wave + " with " + score + " points.",
            )
        }

        if (!gameOver) {
            secondsToNext = secondsToNext - 1
            if (secondsToNext <= 0) {
                wave = wave + 1
                spawnWave(hero)
                announce("Wave " + wave + " incoming!")
                secondsToNext = GameConfig.WAVE_INTERVAL_SECONDS
            }
        }

        pushState()
        return GameConfig.THINK_INTERVAL_SECONDS
    }

    private fun spawnWave(hero: BaseNPCHero) {
        // Pour this wave's enemies in from the map edges in rolling batches (see [spawnBatch]), then
        // add the wave's elites and, on boss waves, the boss.
        spawnHero = hero
        spawnCountRemaining = GameConfig.enemiesForWave(wave)
        spawnBatchIndex = 0
        spawnBatchesLeft = GameConfig.SPAWN_BATCHES
        GameRules.gameModeEntity.setContextThink("wd_spawn_batch", { _ -> spawnBatch() }, 0f)
        spawnElites()
        if (GameConfig.isBossWave(wave)) {
            spawnBoss()
        }
    }

    /**
     * The centre of the playable map — the midpoint of the world bounds, so it's correct on any map,
     * not just one centred on the origin. Every enemy spawns in a ring around it and advances toward
     * it. (worldMinX/MaxX/etc. are top-level function-getter bindings that lower to GetWorldMinX().)
     */
    private fun mapCenter(): Vector =
        Vector((worldMinX + worldMaxX) / 2f, (worldMinY + worldMaxY) / 2f, 0f)

    /**
     * Spawns one batch of this wave's enemies in a ring around the map centre (each successive batch a
     * little further out), each ordered to attack-move to the centre so they advance instead of
     * standing where they spawned. Re-arms itself [GameConfig.SPAWN_BATCH_INTERVAL]s later until the
     * whole wave is spawned, then returns null to stop the think.
     */
    private fun spawnBatch(): Float? {
        if (spawnHero == null || spawnBatchesLeft <= 0 || spawnCountRemaining <= 0) {
            return null
        }
        val batchCount = ceil(spawnCountRemaining.toFloat() / spawnBatchesLeft).toInt()
        val center = mapCenter()
        val radius = GameConfig.SPAWN_RADIUS + spawnBatchIndex * GameConfig.SPAWN_RING_STEP
        for (i in 0 until batchCount) {
            val spawnPos = center + randomVector(radius)
            val unitName =
                if ((spawnBatchIndex + i) % 3 == 0) GameConfig.ENEMY_RANGED_UNIT else GameConfig.ENEMY_MELEE_UNIT
            val unit = createUnitByName(unitName, spawnPos, true, null, null, DOTATeam.BADGUYS)
            unit.moveToPositionAggressive(center)
            spawnedEnemies.add(unit)
            enemiesAlive = enemiesAlive + 1
        }
        spawnCountRemaining = spawnCountRemaining - batchCount
        spawnBatchIndex = spawnBatchIndex + 1
        spawnBatchesLeft = spawnBatchesLeft - 1
        return if (spawnBatchesLeft > 0 && spawnCountRemaining > 0) GameConfig.SPAWN_BATCH_INTERVAL else null
    }

    /**
     * Spawns the Ancient at the map centre — the objective the enemies converge on and attack when
     * they arrive (their attack-move order to the centre engages it automatically). It sits on the
     * player's team so the enemy creeps treat it as hostile, and it never moves. The run ends if it
     * dies (see [onThink]). Re-created fresh on every [restart].
     */
    private fun spawnAncient() {
        ancient?.let { if (!it.isNull) it.removeSelf() }
        val center = mapCenter()
        val a = createUnitByName(GameConfig.ANCIENT_UNIT, center, true, null, null, DOTATeam.GOODGUYS)
        a.baseMaxHealth = GameConfig.ANCIENT_HP.toFloat()
        a.health = GameConfig.ANCIENT_HP
        // Re-skin the creep as the Ancient building (both the original and current model, so it sticks).
        a.setOriginalModel(GameConfig.ANCIENT_MODEL)
        a.setModel(GameConfig.ANCIENT_MODEL)
        a.modelScale = GameConfig.ANCIENT_MODEL_SCALE
        ancient = a
    }

    /**
     * Spawns a single, much tankier boss (every [GameConfig.BOSS_WAVE_INTERVAL] waves). The HUD
     * shows a dedicated boss HP bar while it lives (see BossHpPanel); its HP is pushed in [WaveState]
     * each tick so the client needs no entity handle.
     */
    private fun spawnBoss() {
        val center = mapCenter()
        val spawnPos = center + randomVector(GameConfig.SPAWN_RADIUS)
        val bossUnit = createUnitByName(GameConfig.ENEMY_MELEE_UNIT, spawnPos, true, null, null, DOTATeam.BADGUYS)
        bossUnit.modelScale = GameConfig.BOSS_MODEL_SCALE
        val hp = GameConfig.bossHpForWave(wave)
        bossUnit.baseMaxHealth = hp.toFloat()
        bossUnit.health = hp
        bossUnit.moveToPositionAggressive(center)
        spawnedEnemies.add(bossUnit)
        enemiesAlive = enemiesAlive + 1
        boss = bossUnit
        bossName =
            GameConfig.ELITE_NAMES[(wave / GameConfig.BOSS_WAVE_INTERVAL) % GameConfig.ELITE_NAMES.size]
        announce(bossName + " has arrived!")
    }

    /**
     * Spawns this wave's elite enemies — larger, tracked creeps — and fires
     * [GameConfig.EVENT_ELITE] for each. The HUD turns each event into a transient pop-up that is
     * created and disposed on the fly (see EliteSpawnPopup / EliteFeedPanel) — multiple elites mean
     * multiple live pop-ups, which is the whole point of the showcase.
     */
    private fun spawnElites() {
        val count = GameConfig.elitesForWave(wave)
        val center = mapCenter()
        for (i in 0 until count) {
            val spawnPos = center + randomVector(GameConfig.SPAWN_RADIUS)
            val elite =
                createUnitByName(GameConfig.ENEMY_MELEE_UNIT, spawnPos, true, null, null, DOTATeam.BADGUYS)
            elite.modelScale = 1.6f
            elite.moveToPositionAggressive(center)
            spawnedEnemies.add(elite)
            enemiesAlive = enemiesAlive + 1
            val name = GameConfig.ELITE_NAMES[i % GameConfig.ELITE_NAMES.size]
            CustomGameEventManager.sendServerToAllClients(GameConfig.EVENT_ELITE, EliteAlert(name))
        }
    }

    private fun registerKillListener() {
        onGameEvent(ENTITY_KILLED, null) { event ->
            val entity = entIndexToHScript(event.entindex_killed)
            if (entity != null) {
                val killed = entity as BaseNPC
                if (killed.teamNumber == DOTATeam.BADGUYS) {
                    score = score + GameConfig.SCORE_PER_KILL
                    if (enemiesAlive > 0) {
                        enemiesAlive = enemiesAlive - 1
                    }
                } else if (killed.isRealHero) {
                    // End the run the instant the hero dies — handling it on the kill event (not the
                    // 1s think) is what stops the occasional auto-respawn before the lock is applied.
                    onHeroDeath(killed as BaseNPCHero)
                }
            }
        }
    }

    private fun onHeroDeath(hero: BaseNPCHero) {
        if (!gameOver) {
            gameOver = true
            hero.timeUntilRespawn = GameConfig.GAMEOVER_RESPAWN_LOCK_SECONDS
            announce(
                "Game over! You survived to wave " +
                    wave + " with " + score + " points.",
            )
            pushState()
        }
    }

    /** Type "nova" in all-chat to detonate the showcase AoE around your hero. */
    private fun registerNovaCommand() {
        onGameEvent(PLAYER_CHAT, null) { event ->
            if (event.text.contains("nova")) {
                val caster = PlayerResource.getSelectedHeroEntity(event.playerid)
                if (caster != null && caster.isAlive) {
                    val hits = Nova.cast(caster)
                    announce("Nova hit " + hits + " enemies!")
                }
            }
        }
    }

    /**
     * The HUD's "Play Again" button ([PlayAgainButton]) sends [GameConfig.EVENT_RESTART] from the
     * client; reset the run on receipt. This replaces the old "type 'restart' in chat" command with
     * a real button + a typed client→server event.
     */
    private fun registerRestartListener() {
        CustomGameEventManager.registerListener(GameConfig.EVENT_RESTART) { _, _ ->
            // Only restart while the run is actually over. [restart] clears gameOver immediately, so
            // the rest of a rapid click-burst (the button is briefly still visible client-side) is
            // ignored — no double hero-replacement / re-roll spam.
            if (gameOver) {
                val hero = PlayerResource.getSelectedHeroEntity(PlayerID(0))
                if (hero != null) restart(hero)
            }
        }
    }

    fun restart(hero: BaseNPCHero) {
        // Clear the previous run's creeps off the board (removeSelf is a clean delete — no death
        // event, so it doesn't feed the kill listener), then reset state.
        spawnedEnemies.forEach { if (!it.isNull) it.removeSelf() }
        spawnedEnemies.clear()
        boss = null
        bossName = ""
        wave = 0
        score = 0
        enemiesAlive = 0
        // Cancel any in-flight spawn batches from the run that just ended.
        spawnBatchesLeft = 0
        spawnCountRemaining = 0
        secondsToNext = GameConfig.START_DELAY_SECONDS
        gameOver = false
        // Fresh run: replace the (locked-dead) hero with a brand-new level-1 copy of the same hero —
        // no XP, gold, or items carried over. This is the clean way to reset level/items/gold, since
        // there is no "level down" API; the client HUD re-reads the portrait unit so it follows the
        // new hero automatically.
        PlayerResource.replaceHeroWithNoTransfer(PlayerID(0), hero.unitName, 0, 0)
        setStartingGold(PlayerID(0))
        // Rebuild the objective for the fresh run (the previous Ancient was destroyed or stale).
        spawnAncient()
        announce("New run! Survive the waves.")
    }

    /**
     * Sets the player's *total* gold to exactly [GameConfig.STARTING_GOLD]. `SetGold` writes a single
     * bucket (reliable or unreliable), so both are written — otherwise the value lands on top of the
     * default unreliable starting gold and reads as "added" rather than "set".
     */
    private fun setStartingGold(playerId: PlayerID) {
        PlayerResource.setGold(playerId, GameConfig.STARTING_GOLD, true)
        PlayerResource.setGold(playerId, 0, false)
    }

    /**
     * Moves items waiting in the stash into any empty main-inventory slots, so picked-up/bought items
     * don't get stranded in the stash in this single-arena mode. Main slots are 0-5, stash 9-14.
     */
    private fun pullStashItems(hero: BaseNPCHero) {
        for (mainSlot in 0 until 6) {
            if (hero.getItemInSlot(mainSlot) == null) {
                for (stashSlot in 9 until 15) {
                    if (hero.getItemInSlot(stashSlot) != null) {
                        hero.swapItems(mainSlot, stashSlot)
                        break
                    }
                }
            }
        }
    }

    private fun pushState() {
        // Resolve the boss bar from the tracked boss, clearing the handle once it has died.
        val b = boss
        var bossAlive = false
        var bossHp = 0
        if (b != null && !b.isNull && b.isAlive) {
            bossAlive = true
            bossHp = b.healthPercent
        } else {
            boss = null
        }
        val state =
            WaveState(
                wave = wave,
                score = score,
                enemiesAlive = enemiesAlive,
                secondsToNext = if (secondsToNext < 0) 0 else secondsToNext,
                gameOver = gameOver,
                bossActive = bossAlive,
                bossHpPercent = bossHp,
                bossName = bossName,
            )
        CustomGameEventManager.sendServerToAllClients(GameConfig.EVENT_STATE, state)
    }

    private fun announce(text: String) {
        println(text)
        CustomGameEventManager.sendServerToAllClients(GameConfig.EVENT_MESSAGE, Announcement(text))
    }
}
