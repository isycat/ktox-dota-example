package com.isycat.dotaaddon

import com.isycat.dota.types.PlayerID
import com.isycat.dota.types.lua.BaseAbility
import com.isycat.dota.types.lua.BaseNPC
import com.isycat.dota.types.lua.BaseNPCHero
import com.isycat.dota.types.lua.CScriptPrecacheContext
import com.isycat.dota.types.lua.CustomGameEventManager
import com.isycat.dota.types.lua.DOTATeam
import com.isycat.dota.types.lua.DOTAUnitAttackCapability
import com.isycat.dota.types.lua.DOTAUnitMoveCapability
import com.isycat.dota.types.lua.DotaShopType
import com.isycat.dota.types.lua.Dotaunitorder
import com.isycat.dota.types.lua.ENTITY_KILLED
import com.isycat.dota.types.lua.ExecuteOrderFilterEvent
import com.isycat.dota.types.lua.GameRules
import com.isycat.dota.types.lua.PLAYER_CHAT
import com.isycat.dota.types.lua.PlayerResource
import com.isycat.dota.types.lua.Vector
import com.isycat.dota.types.lua.createUnitByName
import com.isycat.dota.types.lua.emitGlobalSound
import com.isycat.dota.types.lua.entIndexToHScript
import com.isycat.dota.types.lua.precacheUnitByNameSync
import com.isycat.dota.types.lua.randomFloat
import com.isycat.dota.types.lua.randomInt
import com.isycat.dota.types.lua.registerListener
import com.isycat.dota.types.lua.sendServerToAllClients
import com.isycat.dota.types.lua.spawnDOTAShopTriggerRadiusApproximate
import com.isycat.dota.types.lua.worldMaxX
import com.isycat.dota.types.lua.worldMaxY
import com.isycat.dota.types.lua.worldMinX
import com.isycat.dota.types.lua.worldMinY
import com.isycat.dotaaddon.WaveDefenseController.heroSpawnPos
import com.isycat.dotaaddon.WaveDefenseController.midasOrderFilter
import com.isycat.dotaaddon.WaveDefenseController.midasProtected
import com.isycat.dotaaddon.WaveDefenseController.onThink
import com.isycat.dotaaddon.WaveDefenseController.restart
import com.isycat.dotaaddon.WaveDefenseController.spawnBatch
import com.isycat.dotaaddon.WaveDefenseController.spawnWave
import com.isycat.dotaaddon.bosses.BossCast
import com.isycat.dotaaddon.bosses.BossSpec
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

    /**
     * The boss roster — real heroes spawned as bosses, cycled by boss wave. Each casts ONE signature
     * ability at the player on a timer (see [spawnBoss] / [bossCastThink]); the three [BossCast] kinds
     * cover the no-target / unit-target / point cast paths. Hero units + their abilities are precached
     * in [precacheBossHeroes].
     */
    private val bossRoster =
        listOf(
            BossSpec(
                "npc_dota_hero_tidehunter",
                "tidehunter_ravage",
                BossCast.NO_TARGET,
                "Leviathan, the Tidehunter",
            ),
            BossSpec("npc_dota_hero_lina", "lina_laguna_blade", BossCast.TARGET, "Lina, the Slayer"),
            BossSpec(
                "npc_dota_hero_jakiro",
                "jakiro_macropyre",
                BossCast.POSITION,
                "Jakiro, the Twin Dragon",
            ),
            BossSpec("npc_dota_hero_lion", "lion_finger_of_death", BossCast.TARGET, "Lion, the Demon Witch"),
        )

    /**
     * Bosses + elites that may NOT be killed with Hand of Midas (it would convert them to instant gold,
     * trivialising the fight). Tracked by unit handle (entity `==` works server-side); enforced in
     * [midasOrderFilter]; cleared on restart.
     */
    private val midasProtected = mutableSetOf<BaseNPC>()

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

    /**
     * Dev chat command `-skip N` — jumps straight to wave N (boss waves included) so the later waves
     * can be reached for testing without grinding there. Gated on cheats (`sv_cheats 1`) being on, so
     * it does nothing in a normal game even if someone types it.
     */
    private fun registerCheatListener() {
        onGameEvent(PLAYER_CHAT, null) { event ->
            if (GameRules.isCheatMode) {
                val parts = event.text.split(" ")
                if (parts.size == 2 && parts[0] == "-skip") {
                    val target = parts[1].toIntOrNull()
                    if (target != null && target > 0) skipToWave(target)
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
        midasProtected.clear()
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
     * The abilities panel sends [WD_UPGRADE] ONLY for the +stats attribute bonus.
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
     * The inventory bar sends [WD_SWAP] when the player drags one item onto another
     * slot. The swap runs server-side on the player's own hero (SwapItems force-swaps with no checks),
     * re-validated here: both must be real carried/backpack slots (0-8), so a forged event can't reach
     * the stash or out-of-range slots, and they must differ.
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

    /**
     * Starts the wave-spawning think loop. Must run from Activate(), not main():
     * `GameRules:GetGameModeEntity()` is nil at script load and only becomes valid
     * once the engine has activated the game mode.
     */
    fun beginThink() {
        // Items bought anywhere go straight to the inventory (filling empty slots) instead of being
        // parked in the stash — no manual stash juggling in a single-arena survival mode.
        GameRules.setUseUniversalShopMode(true)
        // Make the whole arena a shop so the player can buy AND sell anywhere (no fountain trip): a
        // home-shop trigger centred on the arena with a radius covering the play area. Without an
        // in-range shop, SELL_ITEM orders fail ("Can't sell item outside range of a shop").
        val shop = spawnDOTAShopTriggerRadiusApproximate(mapCenter(), GameConfig.SHOP_RADIUS)
        shop.shopType = DotaShopType.HOME
        // Co-op survival: everyone plays on Radiant against the spawned creeps — give the Dire side no
        // player slots at all.
        GameRules.setCustomGameTeamMaxPlayers(DOTATeam.BADGUYS, 0)
        // Single-life survival: the engine must never auto-respawn the hero on its normal timer — only
        // our own logic (the restart flow) or a bought-back/item revive may bring it back. Without this
        // a dead hero pops back up behind the game-over screen.
        GameRules.isHeroRespawnEnabled = false
        // Cheat-proofing: reject Hand of Midas cast on a boss/elite (server-side order validation).
        GameRules.gameModeEntity.setExecuteOrderFilter(
            { event -> midasOrderFilter(event) },
            GameRules.gameModeEntity,
        )
        GameRules.gameModeEntity.setContextThink(
            "wd_think",
            { _ -> onThink() },
            GameConfig.THINK_INTERVAL_SECONDS,
        )
    }

    /**
     * Server-side order filter: blocks a `item_hand_of_midas` cast targeting a [midasProtected]
     * boss/elite (instant-killing them for gold would trivialise the run). All other orders pass through.
     * The client can only *request* the cast; the backend decides — so it can't be bypassed.
     */
    private fun midasOrderFilter(event: ExecuteOrderFilterEvent): Boolean {
        if (event.order_type == Dotaunitorder.CAST_TARGET) {
            val target = entIndexToHScript(event.entindex_target) as? BaseNPC
            if (target != null && target in midasProtected) {
                val ability = entIndexToHScript(event.entindex_ability) as? BaseAbility
                if (ability != null && ability.abilityName == GameConfig.MIDAS_ITEM) {
                    return false
                }
            }
        }
        return true
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
        // (Whirling Death is swapped in on Timbersaw via the npc_spawned listener, not polled here.)
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

    /**
     * The centre of the playable map — the midpoint of the world bounds, so it's correct on any map,
     * not just one centred on the origin. Every enemy spawns in a ring around it and advances toward
     * it. (worldMinX/MaxX/etc. are top-level function-getter bindings that lower to GetWorldMinX().)
     */
    private fun mapCenter(): Vector = Vector((worldMinX + worldMaxX) / 2f, (worldMinY + worldMaxY) / 2f, 0f)

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
        // ...and it can't be selected (a stray click on it must not steal the hero selection). The typed
        // KClass overload (not a name string) is what makes this work end-to-end: referencing the class
        // imports its module, so the transpiler requires it, so its `@Dota2Class` engine registration
        // (ktox_link_modifier) actually runs — no hand-written LinkLuaModifier needed.
        a.addNewModifier(a, null, UnselectableModifier::class, null)
        ancient = a
    }

    /**
     * Spawns this boss wave's boss — a real hero (cycled from [bossRoster]) that marches on the Ancient
     * and repeatedly casts its signature ability at the player (see [bossCastThink]). It is force-levelled
     * to [GameConfig.BOSS_HERO_LEVEL] for a real stat block + mana pool, its signature ability maxed so it
     * can cast immediately, then given the wave-scaled boss HP. The HUD shows a dedicated boss HP bar while
     * it lives (see BossHpPanel); its HP is pushed in [WaveState] each tick so the client needs no handle.
     */
    private fun spawnBoss() {
        val spec = bossRoster[(wave / GameConfig.BOSS_WAVE_INTERVAL - 1) % bossRoster.size]
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
        // Give the boss a real level (stats + a mana pool), then force its signature ability to max so it
        // can cast from wave one it appears (UpgradeAbility/points aren't needed — SetLevel force-levels).
        for (i in 1 until GameConfig.BOSS_HERO_LEVEL) bossUnit.heroLevelUp(false)
        bossUnit.findAbilityByName(spec.abilityName)?.let { it.level = it.maxLevel }
        bossUnit.modelScale = GameConfig.BOSS_HERO_SCALE
        // Set HP AFTER levelling — heroLevelUp resets max health to the level's value.
        val hp = GameConfig.bossHpForWave(wave)
        bossUnit.baseMaxHealth = hp.toFloat()
        bossUnit.health = hp
        orderToAncient(bossUnit)
        bossUnit.setContextThink(
            "wd_boss_cast",
            { _ -> bossCastThink(bossUnit, spec) },
            GameConfig.BOSS_CAST_INTERVAL_SECONDS,
        )
        spawnedEnemies.add(bossUnit)
        midasProtected.add(bossUnit)
        enemiesAlive++
        boss = bossUnit
        bossName = spec.displayName
        announce("${spec.displayName} has arrived!")
    }

    /**
     * Boss AI think: every [GameConfig.BOSS_CAST_INTERVAL_SECONDS], casts the boss's signature ability at
     * the player hero when it's off cooldown. The boss's mana is topped up first so it never fizzles for
     * mana — the ability's own cooldown is what paces the casts. Returns null (stops the think) once the
     * boss is dead/gone. [BossCast] selects the matching `CastAbility*` order; playerIndex -1 = a non-player
     * (script-controlled) cast.
     */
    private fun bossCastThink(
        bossUnit: BaseNPCHero,
        spec: BossSpec,
    ): Float? {
        if (bossUnit.isNull || !bossUnit.isAlive) return null
        val target = PlayerResource.getSelectedHeroEntity(PlayerID(0))
        val ability = bossUnit.findAbilityByName(spec.abilityName)
        var castThisTick = false
        if (ability != null && ability.level > 0 && target != null && target.isAlive && !target.isNull) {
            bossUnit.mana = bossUnit.maxMana
            if (ability.isFullyCastable) {
                when (spec.cast) {
                    BossCast.NO_TARGET -> bossUnit.castAbilityNoTarget(ability, -1)
                    BossCast.TARGET -> bossUnit.castAbilityOnTarget(target, ability, -1)
                    BossCast.POSITION -> bossUnit.castAbilityOnPosition(target.absOrigin, ability, -1)
                }
                castThisTick = true
            }
        }
        // On ticks where it didn't cast, re-issue the attack-move on the Ancient so the boss keeps
        // advancing and never stalls between spells — a cast consumes its move order, and aggressive-move
        // re-engages anything in the way. (When it's already at the Ancient this is a near no-op, so its
        // attacks aren't perpetually interrupted.)
        if (!castThisTick) {
            val standing = ancient
            val dest = if (standing != null && !standing.isNull) standing.absOrigin else mapCenter()
            bossUnit.moveToPositionAggressive(dest)
        }
        return GameConfig.BOSS_CAST_INTERVAL_SECONDS
    }

    /**
     * Precaches every boss hero unit (called from the engine Precache hook). Heroes pull in a lot of
     * assets, so loading them up front keeps the first boss wave from hitching.
     */
    fun precacheBossHeroes(context: CScriptPrecacheContext) {
        bossRoster.forEach { precacheUnitByNameSync(it.unitName, context, null) }
    }

    /**
     * Spawns this wave's elite enemies — larger, tracked creeps — and fires
     * [WD_ELITE] for each. The HUD turns each event into a transient pop-up that is
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
            midasProtected.add(elite)
            enemiesAlive++
            val name = GameConfig.ELITE_NAMES[i % GameConfig.ELITE_NAMES.size]
            CustomGameEventManager.sendServerToAllClients(WD_ELITE, EliteAlert(name))
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
                    // Stop retaining dead creeps: drop the handle from the tracking collections so they
                    // stay bounded to LIVING units across a long run. Otherwise every creep ever spawned
                    // lingers here until the next restart (a slow memory leak, and an ever-growing list
                    // for restart's cleanup sweep to walk). midasProtected only holds elites/bosses.
                    spawnedEnemies.remove(killed)
                    midasProtected.remove(killed)
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

    /**
     * The HUD's PlayAgainButton sends [WD_RESTART] from the
     * client; reset the run on receipt. This replaces the old "type 'restart' in chat" command with
     * a real button + a typed client→server event.
     */
    private fun registerRestartListener() {
        CustomGameEventManager.registerListener(WD_RESTART) { _, _ ->
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
        midasProtected.clear()
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
                if (h != null && !h.isNull) {
                    h.absOrigin = pos
                    // Fresh run: wipe EVERY slot — inventory, backpack AND stash (0-14) — so nothing
                    // carries into the new run (the hero-replace can leave backpack items behind, and the
                    // stash-pull would otherwise re-add stash items into the new hero). Server-side only.
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
     * Pulls items out of the stash into the hero's inventory in this single-arena mode (stash slots
     * 9-14), AND assembles recipes (e.g. Hand of Midas) on the way.
     *
     * For each stash item: `TakeItem` removes it from the stash WITHOUT destroying it, then `AddItem`
     * re-adds it to the main inventory — and AddItem runs the engine's recipe-combine check, so a
     * completed recipe actually assembles. The take-then-add is what avoids the duplication seen when
     * AddItem is called on a still-owned stash item; plain `swapItems` relocated but never combined.
     */
    private fun pullStashItems(hero: BaseNPCHero) {
        for (stashSlot in 9 until 15) {
            val item = hero.getItemInSlot(stashSlot)
            if (item != null) hero.addItem(hero.takeItem(item))
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
