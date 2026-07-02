package com.isycat.dotaaddon

import com.isycat.dota.types.PlayerID
import com.isycat.dota.types.lua.BaseNPC
import com.isycat.dota.types.lua.BaseNPCHero
import com.isycat.dota.types.lua.CustomGameEventManager
import com.isycat.dota.types.lua.AbilityTypes
import com.isycat.dota.types.lua.DOTATeam
import com.isycat.dota.types.lua.DOTAUnitAttackCapability
import com.isycat.dota.types.lua.DOTAUnitMoveCapability
import com.isycat.dota.types.lua.DotaAbilityBehavior
import com.isycat.dota.types.lua.DotaShopType
import com.isycat.dota.types.lua.ENTITY_KILLED
import com.isycat.dota.types.lua.GameRules
import com.isycat.dota.types.lua.PLAYER_CHAT
import com.isycat.dota.types.lua.PlayerResource
import com.isycat.dota.types.lua.Vector
import com.isycat.dota.types.lua.behaviorFlags
import com.isycat.dota.types.lua.createUnitByName
import com.isycat.dota.types.lua.emitGlobalSound
import com.isycat.dota.types.lua.entIndexToHScript
import com.isycat.dota.types.lua.randomFloat
import com.isycat.dota.types.lua.randomInt
import com.isycat.dota.types.lua.registerListener
import com.isycat.dota.types.lua.sendServerToAllClients
import com.isycat.dota.types.lua.spawnDOTAShopTriggerRadiusApproximate
import com.isycat.dota.types.lua.worldMaxX
import com.isycat.dota.types.lua.worldMaxY
import com.isycat.dota.types.lua.worldMinX
import com.isycat.dota.types.lua.worldMinY
import com.isycat.dotaaddon.WaveDefenseController.bossCastThink
import com.isycat.dotaaddon.WaveDefenseController.heroSpawnPos
import com.isycat.dotaaddon.WaveDefenseController.onThink
import com.isycat.dotaaddon.WaveDefenseController.registerCheatListener
import com.isycat.dotaaddon.WaveDefenseController.restart
import com.isycat.dotaaddon.WaveDefenseController.spawnBatch
import com.isycat.dotaaddon.WaveDefenseController.spawnWave
import com.isycat.dotaaddon.modifiers.UnselectableModifier
import com.isycat.dotaaddon.shared.GameConfig
import com.isycat.dotaaddon.shared.events.Announcement
import com.isycat.dotaaddon.shared.events.EliteAlert
import com.isycat.dotaaddon.shared.events.WD_ELITE
import com.isycat.dotaaddon.shared.events.WD_MESSAGE
import com.isycat.dotaaddon.shared.events.WD_RESTART
import com.isycat.dotaaddon.shared.events.WD_STATE
import com.isycat.dotaaddon.shared.events.WD_SWAP
import com.isycat.dotaaddon.shared.events.WD_UPGRADE
import com.isycat.dotaaddon.shared.events.WaveState
import com.isycat.dotaaddon.units.eliteKindFor
import com.isycat.ktox.dota.lib.addNewModifier
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
object WaveDefenseController {
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
        println("WaveDefenseController starting up")
        registerKillListener()
        registerRestartListener()
        registerUpgradeListener()
        registerSwapListener()
        registerCheatListener()
    }

    /** Dev chat command `-skip N` — skips N waves AHEAD (wave += N). Gated on cheats, so it no-ops normally. */
    private fun registerCheatListener() {
        onGameEvent(PLAYER_CHAT, null) { event ->
            if (GameRules.isCheatMode) {
                val parts = event.text.trim().split(" ")
                if (parts.size > 1 && parts[0] == "-skip") {
                    val count = parts[1].toIntOrNull()
                    if (count != null && count > 0) skipToWave(wave + count)
                }
            }
        }
    }

    /**
     * Clears the board and rewinds the wave counter so the next think tick spawns wave [target].
     * Cheats-only (see [registerCheatListener]); no-op before the run has started or once it's over.
     */
    private fun skipToWave(target: Int) {
        if (!started || gameOver) return
        spawnedEnemies.forEach { if (!it.isNull) it.removeSelf() }
        spawnedEnemies.clear()
        enemiesAlive = 0
        boss = null
        bossName = ""
        spawnBatchesLeft = 0
        spawnCountRemaining = 0
        wave = target - 1
        secondsToNext = 1
        announce("[cheat] Skipping to wave $target")
    }

    /**
     * The abilities panel sends [WD_UPGRADE] only for the hidden +stats bonus (all else levels via native
     * TRAIN_ABILITY orders). upgradeAbility force-levels with no checks, so re-validate as the engine would.
     */
    private fun registerUpgradeListener() {
        CustomGameEventManager.registerListener(WD_UPGRADE) { _, event ->
            val hero = PlayerResource.getSelectedHeroEntity(PlayerID(0))
            if (hero != null && hero.isAlive) {
                val ability = hero.getAbilityByIndex(event.slot)
                if (ability != null &&
                    ability.isAttributeBonus &&
                    hero.abilityPoints > 0 &&
                    ability.level < ability.maxLevel &&
                    hero.level >= ability.heroLevelRequiredToUpgrade
                ) {
                    val before = hero.abilityPoints
                    hero.upgradeAbility(ability)
                    if (hero.abilityPoints == before) hero.abilityPoints--
                }
            }
        }
    }

    /**
     * The inventory bar sends [WD_SWAP] on a drag. Runs server-side on the player's own hero, re-validated
     * to real carried/backpack slots (0-8) that differ — so a forged event can't reach the stash.
     */
    private fun registerSwapListener() {
        CustomGameEventManager.registerListener(WD_SWAP) { _, event ->
            val hero = PlayerResource.getSelectedHeroEntity(PlayerID(0))
            if (hero != null && hero.isAlive) {
                val from = event.fromSlot
                val to = event.toSlot
                if (from != to && from >= 0 && from < 9 && to >= 0 && to < 9) {
                    hero.swapItems(from, to)
                }
            }
        }
    }

    /** Starts the wave-spawning think loop. Must run from Activate() — the game-mode entity is nil at load. */
    fun beginThink() {
        // Bought items go straight to the inventory, not the stash (single-arena survival).
        GameRules.setUseUniversalShopMode(true)
        // Make the whole arena a shop so buying/selling works anywhere (no fountain trip).
        val shop = spawnDOTAShopTriggerRadiusApproximate(mapCenter(), GameConfig.SHOP_RADIUS)
        shop.shopType = DotaShopType.HOME
        // Co-op survival: all players on Radiant vs the creeps — no Dire slots.
        GameRules.setCustomGameTeamMaxPlayers(DOTATeam.BADGUYS, 0)
        // Single life per run: no auto-respawn (only the restart flow revives).
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

        // Initialise the first attempt exactly like a restart (replace with a fresh level-1 hero, grant
        // starting gold). Done here because the hero only exists now; return so this tick's rest skips it.
        if (!started) {
            started = true
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
                // Lock the hero dead until "Play Again" so it can't auto-respawn behind the game-over screen.
                hero.timeUntilRespawn = GameConfig.GAMEOVER_RESPAWN_LOCK_SECONDS
                announce(
                    "Game over! You survived to wave " +
                        wave + " with " + score + " points.",
                )
            }
            pushState()
            return GameConfig.THINK_INTERVAL_SECONDS
        }

        // Second lose condition: if the Ancient falls, kill the hero with it.
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
            // Wave fully spawned AND cleared: snap the countdown down instead of waiting out the timer.
            if (wave > 0 &&
                enemiesAlive <= 0 &&
                spawnCountRemaining <= 0 &&
                secondsToNext > GameConfig.CLEARED_NEXT_WAVE_SECONDS
            ) {
                secondsToNext = GameConfig.CLEARED_NEXT_WAVE_SECONDS
            }
            secondsToNext--
            if (secondsToNext <= 0) {
                wave++
                spawnWave(hero)
                announce(
                    "Wave " + wave + " incoming from the " + GameConfig.DIRECTION_NAMES[spawnDirIndex] + "!",
                )
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
        // Each wave tops up the hero's Bottle (if carried) back to full charges.
        val bottle = hero.findItemInInventory(GameConfig.BOTTLE_ITEM)
        if (bottle != null) bottle.currentCharges = GameConfig.BOTTLE_CHARGES
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

    /** Map centre — midpoint of the world bounds (correct on any map). Enemies ring it and march inward. */
    private fun mapCenter(): Vector = Vector((worldMinX + worldMaxX) / 2f, (worldMinY + worldMaxY) / 2f, 0f)

    /** A spawn position [radius] out, within this wave's 90° arc — so the wave pours in from one side. */
    private fun arcSpawnPos(radius: Float): Vector {
        val baseDeg = spawnDirIndex * 90.0
        val angle = (baseDeg + randomFloat(-45f, 45f)) * (PI / 180.0)
        return mapCenter() + Vector((cos(angle) * radius).toFloat(), (sin(angle) * radius).toFloat(), 0f)
    }

    /** Attack-moves [unit] to the Ancient (creeps have no vision to attack-target it directly) — engaging anything en route. */
    private fun orderToAncient(unit: BaseNPC) {
        // A freshly spawned unit drops orders given the same frame it's created, so issue it a beat later.
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

    /** Spawns one ring of this wave's enemies (each a bit further out), attack-moving inward; re-arms until the wave is done. */
    private fun spawnBatch(): Float? {
        if (spawnHero == null || spawnBatchesLeft <= 0 || spawnCountRemaining <= 0) {
            return null
        }
        val batchCount = ceil(spawnCountRemaining.toFloat() / spawnBatchesLeft).toInt()
        val radius = GameConfig.SPAWN_RADIUS + spawnBatchIndex * GameConfig.SPAWN_RING_STEP
        for (i in 0 until batchCount) {
            val spawnPos = arcSpawnPos(radius)
            val unitName =
                if ((spawnBatchIndex + i) % 3 ==
                    0
                ) {
                    GameConfig.ENEMY_RANGED_UNIT
                } else {
                    GameConfig.ENEMY_MELEE_UNIT
                }
            val unit = createUnitByName(unitName, spawnPos, true, null, null, DOTATeam.BADGUYS)
            val hp = GameConfig.creepHpForWave(wave)
            unit.baseMaxHealth = hp.toFloat()
            unit.health = hp
            orderToAncient(unit)
            spawnedEnemies.add(unit)
            enemiesAlive++
        }
        spawnCountRemaining -= batchCount
        spawnBatchIndex++
        spawnBatchesLeft--
        return if (spawnBatchesLeft > 0 && spawnCountRemaining > 0) GameConfig.SPAWN_BATCH_INTERVAL else null
    }

    /**
     * Spawns the Ancient objective at the map centre on the player's team (enemies attack-move onto it).
     * It never moves and the run ends if it dies (see [onThink]). Re-created fresh on every [restart].
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
        // Make it unselectable. The typed KClass overload imports the modifier module, so the transpiler
        // requires it and its @Dota2Class registration runs — no hand-written LinkLuaModifier needed.
        a.addNewModifier(a, null, UnselectableModifier::class, null)
        ancient = a
    }

    /**
     * Spawns this boss wave's boss — a real hero (cycled from [GameConfig.BOSS_ROSTER]) force-levelled to
     * [GameConfig.bossLevelForWave] with its full kit maxed, that marches on the Ancient and casts at the
     * player (see [bossCastThink]). Its HP rides in [WaveState] each tick so the HUD boss bar needs no handle.
     */
    private fun spawnBoss() {
        val roster = GameConfig.BOSS_ROSTER
        val spec = roster[(wave / GameConfig.BOSS_WAVE_INTERVAL - 1) % roster.size]
        val spawnPos = arcSpawnPos(GameConfig.SPAWN_RADIUS)
        val bossUnit =
            createUnitByName(
                spec.unitName,
                spawnPos,
                true,
                null,
                null,
                DOTATeam.BADGUYS,
            ) as BaseNPCHero
        // Give the boss real levels (stats + a mana pool) SCALED to the wave — roughly a notch above the
        // player, not a flat 20 — then spend its skill points like a REAL hero of that level: basics
        // rotate lowest-first under the every-other-level cap, the ultimate takes priority at 6/12/18.
        // A level-4 boss fights with 2/1/1 and no ult — never a maxed kit.
        val bossLevel = GameConfig.bossLevelForWave(wave)
        (1 until bossLevel).forEach { _ -> bossUnit.heroLevelUp(false) }
        val kit =
            (0 until bossUnit.abilityCount)
                .mapNotNull { bossUnit.getAbilityByIndex(it) }
                .filter { !it.isHidden && !it.isAttributeBonus }
        val ultimate = kit.firstOrNull { it.abilityType == AbilityTypes.ULTIMATE.value }
        val basics = kit.filter { it.abilityType != AbilityTypes.ULTIMATE.value }
        for (heroLevel in 1..bossLevel) {
            if (ultimate != null &&
                ultimate.level < ultimate.maxLevel &&
                heroLevel >= 6 * (ultimate.level + 1)
            ) {
                ultimate.level += 1
                continue
            }
            basics
                .filter { it.level < it.maxLevel && it.level < (heroLevel + 1) / 2 }
                .minByOrNull { it.level }
                ?.let { it.level += 1 }
        }
        bossUnit.modelScale = GameConfig.bossScaleForLevel(bossLevel)
        // Set HP AFTER levelling — heroLevelUp resets max health to the level's value.
        val hp = GameConfig.bossHpForWave(wave)
        bossUnit.baseMaxHealth = hp.toFloat()
        bossUnit.health = hp
        orderToAncient(bossUnit)
        bossUnit.setContextThink(
            "wd_boss_cast",
            { _ -> bossCastThink(bossUnit) },
            GameConfig.BOSS_CAST_INTERVAL_SECONDS,
        )
        spawnedEnemies.add(bossUnit)
        enemiesAlive++
        boss = bossUnit
        bossName = spec.displayName
        announce("${spec.displayName} has arrived!")
    }

    /**
     * Boss AI: casts the first ready ability at the player, aimed by its behavior (no-target/unit/point).
     * Mana is topped up so casts never fizzle; each ability's cooldown paces them. Returns null (stops the
     * think) once the boss is dead. playerIndex -1 = a script-controlled cast.
     */
    private fun bossCastThink(bossUnit: BaseNPCHero): Float? {
        if (bossUnit.isNull || !bossUnit.isAlive) return null
        var castThisTick = false
        val target = PlayerResource.getSelectedHeroEntity(PlayerID(0))
        if (target != null && target.isAlive && !target.isNull) {
            bossUnit.mana = bossUnit.maxMana
            val ability =
                (0 until bossUnit.abilityCount)
                    .mapNotNull { bossUnit.getAbilityByIndex(it) }
                    .firstOrNull {
                        it.level > 0 && !it.isHidden && !it.isAttributeBonus && it.isFullyCastable
                    }
            if (ability != null) {
                val behavior = ability.behaviorFlags()
                castThisTick =
                    when {
                        behavior and DotaAbilityBehavior.NO_TARGET.value != 0 -> {
                            bossUnit.castAbilityNoTarget(ability, -1)
                            true
                        }

                        behavior and DotaAbilityBehavior.UNIT_TARGET.value != 0 -> {
                            bossUnit.castAbilityOnTarget(target, ability, -1)
                            true
                        }

                        behavior and DotaAbilityBehavior.POINT.value != 0 -> {
                            bossUnit.castAbilityOnPosition(target.absOrigin, ability, -1)
                            true
                        }

                        else -> {
                            false
                        }
                    }
            }
        }
        // On a non-cast tick, re-issue orders (a cast consumes the previous ones). Re-ordering the
        // MARCH every tick would yank the boss off any fight - so when the player's hero is close,
        // the boss attacks THE PLAYER; only otherwise does it resume the march on the Ancient.
        if (!castThisTick) {
            if (target != null && target.isAlive && !target.isNull &&
                (target.absOrigin - bossUnit.absOrigin).len() < GameConfig.BOSS_ENGAGE_RANGE
            ) {
                bossUnit.moveToTargetToAttack(target)
            } else {
                val standing = ancient
                val dest = if (standing != null && !standing.isNull) standing.absOrigin else mapCenter()
                bossUnit.moveToPositionAggressive(dest)
            }
        }
        return GameConfig.BOSS_CAST_INTERVAL_SECONDS
    }

    /** Spawns this wave's elites and fires [WD_ELITE] for each (the HUD shows a transient pop-up per alert). */
    private fun spawnElites() {
        val count = GameConfig.elitesForWave(wave)
        for (i in 0 until count) {
            val spawnPos = arcSpawnPos(GameConfig.SPAWN_RADIUS)
            // Cycle the elite kinds — each a custom ANCIENT creep (Midas-immune by the engine's native
            // rule) whose KeyValues are generated programmatically in EliteUnits (@KvSource).
            val kind = eliteKindFor(i)
            val elite =
                createUnitByName(kind.unitName, spawnPos, true, null, null, DOTATeam.BADGUYS)
            val hp = GameConfig.eliteHpForWave(wave)
            elite.baseMaxHealth = hp.toFloat()
            elite.health = hp
            // A KV-granted ability spawns at level 0 - INERT. Learn it or the elite never uses it.
            elite.findAbilityByName(kind.signatureAbility)?.level = 1
            orderToAncient(elite)
            spawnedEnemies.add(elite)
            enemiesAlive++
            CustomGameEventManager.sendServerToAllClients(WD_ELITE, EliteAlert(kind.displayName))
        }
    }

    private fun registerKillListener() {
        onGameEvent(ENTITY_KILLED, null) { event ->
            val entity = entIndexToHScript(event.entindex_killed)
            if (entity != null) {
                val killed = entity as BaseNPC
                if (killed.teamNumber == DOTATeam.BADGUYS) {
                    score += GameConfig.SCORE_PER_KILL
                    if (enemiesAlive > 0) {
                        enemiesAlive--
                    }
                    // Stop retaining dead creeps: drop the handle from spawnedEnemies so it stays bounded to
                    // LIVING units across a long run (otherwise every creep ever spawned lingers until the
                    // next restart — a slow leak + an ever-growing list for restart's cleanup sweep). The
                    // Midas-immune marker needs no cleanup — it lives on the unit and dies with it.
                    spawnedEnemies.remove(killed)
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

    /** The HUD's PlayAgainButton sends [WD_RESTART]; reset the run on receipt. */
    private fun registerRestartListener() {
        CustomGameEventManager.registerListener(WD_RESTART) { _, _ ->
            // Guard a rapid click-burst: restart() clears gameOver immediately, so extra clicks no-op.
            if (gameOver) {
                val hero = PlayerResource.getSelectedHeroEntity(PlayerID(0))
                if (hero != null) restart(hero)
            }
        }
    }

    fun restart(hero: BaseNPCHero) {
        // removeSelf is a clean delete (no death event → doesn't feed the kill listener).
        spawnedEnemies.forEach { if (!it.isNull) it.removeSelf() }
        spawnedEnemies.clear()
        boss = null
        bossName = ""
        wave = 0
        score = 0
        enemiesAlive = 0
        spawnBatchesLeft = 0
        spawnCountRemaining = 0
        secondsToNext = GameConfig.START_DELAY_SECONDS
        gameOver = false
        // No "level down" API, so replace the hero with a fresh level-1 copy to reset level/gold/items.
        PlayerResource.replaceHeroWithNoTransfer(PlayerID(0), hero.unitName, 0, 0)
        setStartingGold(PlayerID(0))
        placeHeroAtSpawn()
        spawnAncient()
        announceBattlePrep()
    }

    /**
     * Moves the freshly-replaced hero back to [heroSpawnPos] a beat after a restart. The new hero only
     * exists next frame (and spawns where the old one died), so the reposition is deferred one think.
     */
    private fun placeHeroAtSpawn() {
        val pos = heroSpawnPos ?: return
        GameRules.gameModeEntity.setContextThink(
            "wd_place_hero",
            { _ ->
                val h = PlayerResource.getSelectedHeroEntity(PlayerID(0))
                if (h != null && !h.isNull) {
                    h.absOrigin = pos
                    // Wipe every slot (inventory, backpack, stash) so nothing carries into the new run.
                    for (slot in 0 until 15) {
                        val item = h.getItemInSlot(slot)
                        if (item != null) h.removeItem(item)
                    }
                }
                null
            },
            0.1f,
        )
    }

    /** Sets total gold to exactly [GameConfig.STARTING_GOLD]. SetGold writes one bucket, so write both. */
    private fun setStartingGold(playerId: PlayerID) {
        PlayerResource.setGold(playerId, GameConfig.STARTING_GOLD, true)
        PlayerResource.setGold(playerId, 0, false)
    }

    /**
     * Pulls stash items (slots 9-14) into the main inventory, assembling recipes on the way. takeItem
     * removes from the stash without destroying, then addItem runs the engine's recipe-combine check.
     */
    private fun pullStashItems(hero: BaseNPCHero) {
        for (stashSlot in 9 until 15) {
            val item = hero.getItemInSlot(stashSlot)
            if (item != null) hero.addItem(hero.takeItem(item))
        }
    }

    private fun pushState() {
        // Clear the boss handle once it has died so the HUD bar hides.
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
        CustomGameEventManager.sendServerToAllClients(WD_STATE, state)
    }

    private fun announce(text: String) {
        println(text)
        CustomGameEventManager.sendServerToAllClients(WD_MESSAGE, Announcement(text))
    }

    /** Banner + announcer sound that opens each attempt's pre-battle countdown (first run and restart). */
    private fun announceBattlePrep() {
        announce(GameConfig.PREPARE_MESSAGE)
        emitGlobalSound(GameConfig.PREPARE_SOUND)
    }
}
