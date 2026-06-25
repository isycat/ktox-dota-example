package com.isycat.dotaaddon

import com.isycat.dota.types.GameEvent
import com.isycat.dota.types.PlayerID
import com.isycat.dota.types.lua.BaseNPC
import com.isycat.dota.types.lua.BaseNPCHero
import com.isycat.dota.types.lua.CustomGameEventManager
import com.isycat.dota.types.lua.DOTATeam
import com.isycat.dota.types.lua.DOTAUnitAttackCapability
import com.isycat.dota.types.lua.DOTAUnitMoveCapability
import com.isycat.dota.types.lua.ENTITY_KILLED
import com.isycat.dota.types.lua.GameRules
import com.isycat.dota.types.lua.PLAYER_CHAT
import com.isycat.dota.types.lua.PlayerResource
import com.isycat.dota.types.lua.Vector
import com.isycat.dota.types.lua.createUnitByName
import com.isycat.dota.types.lua.emitGlobalSound
import com.isycat.dota.types.lua.entIndexToHScript
import com.isycat.dota.types.lua.randomFloat
import com.isycat.dota.types.lua.randomInt
import com.isycat.dota.types.lua.worldMaxX
import com.isycat.dota.types.lua.worldMaxY
import com.isycat.dota.types.lua.worldMinX
import com.isycat.dota.types.lua.worldMinY
import com.isycat.dotaaddon.shared.Announcement
import com.isycat.dotaaddon.shared.EliteAlert
import com.isycat.dotaaddon.shared.GameConfig
import com.isycat.dotaaddon.shared.WaveState
import com.isycat.ktox.dota.lib.onGameEvent
import kotlin.math.PI
import kotlin.math.ceil
import kotlin.math.cos
import kotlin.math.sin

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

    /**
     * The hero's spawn position, captured on the very first attempt and reused on every restart so a
     * new run always begins where the first one did — not wherever the previous run's hero died.
     */
    private var heroSpawnPos: Vector? = null

    /** Rolling wave-spawn batch state (see [spawnWave] / [spawnBatch]). */
    private var spawnHero: BaseNPCHero? = null
    private var spawnCountRemaining = 0
    private var spawnBatchIndex = 0
    private var spawnBatchesLeft = 0

    /** The cardinal direction the current wave pours in from (index into [GameConfig.DIRECTION_NAMES]). */
    private var spawnDirIndex = 0

    /**
     * Registers event listeners. Safe to call at script-load time (from main()).
     * Does NOT touch the game-mode entity, which doesn't exist yet at load.
     */
    fun start() {
        println("WaveDefense starting up")
        registerKillListener()
        registerNovaCommand()
        registerRestartListener()
        registerUpgradeListener()
    }

    /** Marker for reading the [GameConfig.EVENT_UPGRADE_ABILITY] payload off the raw [GameEvent]. */
    private interface AbilityUpgradeEvent : GameEvent {
        val slot: Int
    }

    /**
     * The abilities panel sends [GameConfig.EVENT_UPGRADE_ABILITY] ONLY for the +stats attribute bonus.
     * Every normal ability and talent is upgraded by a native client `TRAIN_ABILITY` order instead (the
     * engine validates points, hero level, max level, and talent-tier exclusivity itself) — the +stats
     * bonus is a *hidden* ability that the engine rejects from a TRAIN_ABILITY order ("ability is
     * hidden"), so it is the one case we must level with `UpgradeAbility` server-side.
     *
     * UpgradeAbility force-levels with no checks, so we re-validate here exactly as the engine would for
     * a normal ability: it must be the attribute bonus (so a forged event can't level anything else),
     * not already at max, the hero must meet its level requirement (this is what was missing — you could
     * take +stats far too early), and a spare point must be spent (UpgradeAbility doesn't deduct one).
     * `canAbilityBeUpgraded` is deliberately NOT used: its binding is typed Boolean but the engine call
     * returns a button-state enum (0 = upgradeable), which is always truthy in Lua and never gates.
     */
    private fun registerUpgradeListener() {
        CustomGameEventManager.registerListener(GameConfig.EVENT_UPGRADE_ABILITY) { _, event ->
            val hero = PlayerResource.getSelectedHeroEntity(PlayerID(0))
            if (hero != null && hero.isAlive) {
                val ability = hero.getAbilityByIndex((event as AbilityUpgradeEvent).slot)
                if (ability != null &&
                    ability.isAttributeBonus &&
                    hero.abilityPoints > 0 &&
                    ability.level < ability.maxLevel &&
                    hero.level >= ability.heroLevelRequiredToUpgrade
                ) {
                    val before = hero.abilityPoints
                    hero.upgradeAbility(ability)
                    if (hero.abilityPoints == before) hero.abilityPoints = before - 1
                }
            }
        }
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
        // Co-op survival: everyone plays on Radiant against the spawned creeps — give the Dire side no
        // player slots at all.
        GameRules.setCustomGameTeamMaxPlayers(DOTATeam.BADGUYS, 0)
        // Single-life survival: the engine must never auto-respawn the hero on its normal timer — only
        // our own logic (the restart flow) or a bought-back/item revive may bring it back. Without this
        // a dead hero pops back up behind the game-over screen.
        GameRules.isHeroRespawnEnabled = false
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

        // Initialise the very first attempt EXACTLY like a restart: replace the picked hero with a
        // fresh level-1 copy so there is no carried-over inventory and no random-pick bonus gold, then
        // grant our starting gold. (Done here, not at spawn, because the hero only exists now.) Return
        // afterwards — the rest of this tick would run against the old, now-replaced hero; the next
        // tick picks up the fresh one.
        if (!started) {
            started = true
            // Remember where the picked hero spawned; every restart will respawn the fresh hero here.
            heroSpawnPos = hero.absOrigin
            PlayerResource.replaceHeroWithNoTransfer(PlayerID(0), hero.unitName, 0, 0)
            setStartingGold(PlayerID(0))
            spawnAncient()
            announceBattlePrep()
            pushState()
            return GameConfig.THINK_INTERVAL_SECONDS
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

        // The Ancient falling is a second lose condition, alongside the hero dying: when it dies the
        // hero dies with it (respawn is disabled, so it stays down until the restart flow).
        val standingAncient = ancient
        if (!gameOver && standingAncient != null && (standingAncient.isNull || !standingAncient.isAlive)) {
            gameOver = true
            if (hero.isAlive) hero.forceKill(false)
            announce(
                "The Ancient has fallen! You survived to wave " +
                    wave + " with " + score + " points.",
            )
        }

        if (!gameOver) {
            // Once a wave is fully spawned AND cleared, don't make the player wait out the long timer —
            // snap the countdown down so the next wave arrives in a few seconds.
            if (wave > 0 && enemiesAlive <= 0 && spawnCountRemaining <= 0 &&
                secondsToNext > GameConfig.CLEARED_NEXT_WAVE_SECONDS
            ) {
                secondsToNext = GameConfig.CLEARED_NEXT_WAVE_SECONDS
            }
            secondsToNext = secondsToNext - 1
            if (secondsToNext <= 0) {
                wave = wave + 1
                spawnWave(hero)
                announce("Wave " + wave + " incoming from the " + GameConfig.DIRECTION_NAMES[spawnDirIndex] + "!")
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
        // This whole wave pours in from one randomly-chosen cardinal direction.
        spawnDirIndex = randomInt(0, GameConfig.DIRECTION_NAMES.size - 1)
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
     * A spawn position [radius] units from the map centre, within the 90° arc (±45°) of this wave's
     * cardinal direction — so the whole wave pours in from one side rather than surrounding the player.
     */
    private fun arcSpawnPos(radius: Float): Vector {
        val baseDeg = spawnDirIndex * 90.0
        val angle = (baseDeg + randomFloat(-45f, 45f)) * (PI / 180.0)
        return mapCenter() + Vector((cos(angle) * radius).toFloat(), (sin(angle) * radius).toFloat(), 0f)
    }

    /**
     * Sends [unit] to attack-MOVE to the Ancient's location (attack-ground), NOT a direct attack-target
     * order. The enemy creeps have no vision of the Ancient when they spawn at the map edge, so a
     * direct attack order can't be issued against it — but an attack-move to its position makes them
     * march there and engage it (and anything in the way) on arrival. The Ancient is static, so its
     * position is stable.
     */
    private fun orderToAncient(unit: BaseNPC) {
        // Issue the attack-move a few frames LATER, not in the unit's creation frame: a freshly spawned
        // unit silently drops orders given the same frame it is created, leaving it standing in place.
        unit.setContextThink(
            "wd_charge",
            { _ ->
                if (!unit.isNull) {
                    val target = ancient
                    val dest = if (target != null && !target.isNull) target.absOrigin else mapCenter()
                    unit.moveToPositionAggressive(dest)
                }
                null
            },
            0.1f,
        )
    }

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
        val radius = GameConfig.SPAWN_RADIUS + spawnBatchIndex * GameConfig.SPAWN_RING_STEP
        for (i in 0 until batchCount) {
            val spawnPos = arcSpawnPos(radius)
            val unitName =
                if ((spawnBatchIndex + i) % 3 == 0) GameConfig.ENEMY_RANGED_UNIT else GameConfig.ENEMY_MELEE_UNIT
            val unit = createUnitByName(unitName, spawnPos, true, null, null, DOTATeam.BADGUYS)
            val hp = GameConfig.creepHpForWave(wave)
            unit.baseMaxHealth = hp.toFloat()
            unit.health = hp
            orderToAncient(unit)
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
        // The Ancient is a static objective: it must never wander or fight, only be attacked.
        a.setMoveCapability(DOTAUnitMoveCapability.NONE)
        a.attackCapability = DOTAUnitAttackCapability.CAP_NO_ATTACK
        ancient = a
    }

    /**
     * Spawns a single, much tankier boss (every [GameConfig.BOSS_WAVE_INTERVAL] waves). The HUD
     * shows a dedicated boss HP bar while it lives (see BossHpPanel); its HP is pushed in [WaveState]
     * each tick so the client needs no entity handle.
     */
    private fun spawnBoss() {
        val spawnPos = arcSpawnPos(GameConfig.SPAWN_RADIUS)
        val bossUnit = createUnitByName(GameConfig.ENEMY_MELEE_UNIT, spawnPos, true, null, null, DOTATeam.BADGUYS)
        bossUnit.modelScale = GameConfig.BOSS_MODEL_SCALE
        val hp = GameConfig.bossHpForWave(wave)
        bossUnit.baseMaxHealth = hp.toFloat()
        bossUnit.health = hp
        orderToAncient(bossUnit)
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
        for (i in 0 until count) {
            val spawnPos = arcSpawnPos(GameConfig.SPAWN_RADIUS)
            val elite =
                createUnitByName(GameConfig.ENEMY_MELEE_UNIT, spawnPos, true, null, null, DOTATeam.BADGUYS)
            val hp = GameConfig.eliteHpForWave(wave)
            elite.baseMaxHealth = hp.toFloat()
            elite.health = hp
            elite.modelScale = 1.6f
            orderToAncient(elite)
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
        placeHeroAtSpawn()
        // Rebuild the objective for the fresh run (the previous Ancient was destroyed or stale).
        spawnAncient()
        announceBattlePrep()
    }

    /**
     * Moves the freshly-replaced hero back to [heroSpawnPos] a beat after a restart.
     * `replaceHeroWithNoTransfer` spawns the new hero where the old one stood (i.e. where it died), and
     * the new hero only exists next frame, so the reposition is deferred via a one-shot think.
     */
    private fun placeHeroAtSpawn() {
        val pos = heroSpawnPos ?: return
        GameRules.gameModeEntity.setContextThink(
            "wd_place_hero",
            { _ ->
                val h = PlayerResource.getSelectedHeroEntity(PlayerID(0))
                if (h != null && !h.isNull) h.absOrigin = pos
                null
            },
            0.1f,
        )
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
     *
     * Uses `swapItems` (a pure relocation): `AddItem` was tried to also trigger recipe assembly, but on
     * an already-owned stash item it DUPLICATES the item rather than moving it. Recipe combining from
     * the stash is left as a separate problem.
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
                running = started,
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

    /** Banner + announcer sound that opens each attempt's pre-battle countdown (first run and restart). */
    private fun announceBattlePrep() {
        announce(GameConfig.PREPARE_MESSAGE)
        emitGlobalSound(GameConfig.PREPARE_SOUND)
    }
}
