# Survival Wave Defense — ktox showcase game

A small, complete game mode that exercises every layer of the ktox Dota suite (Kotlin → Lua + Panorama JS
+ generated KeyValues), end to end.

## How it plays

Your hero defends the **Ancient** at the centre of the arena. Every wave, enemy creeps pour in from one
cardinal direction and march on the Ancient; each wave is larger and tankier than the last. Killing an
enemy scores points. Tougher **elites** join later waves (each fires a transient HUD pop-up), and every
15th wave is a **boss** — a real enemy hero that casts its signature spell at you on a timer
(Tidehunter's Ravage, Lina's Laguna Blade, Jakiro's Macropyre, Lion's Finger of Death).

The custom HUD shows the wave/score/enemies/countdown, your hero's HP, a boss HP bar, an ability bar, and
an inventory bar. The whole arena is a shop (buy/sell anywhere). Your hero is granted **Whirling Death**
(a custom `@AbilityKv` ability). The run ends if your hero **or** the Ancient dies; the **Play Again**
button starts a fresh attempt. With cheats on (`sv_cheats 1`), `-skip N` jumps to wave N for testing.

## What each layer demonstrates

| Layer | File | ktox feature |
|-------|------|--------------|
| Shared | [`shared/.../GameConfig.kt`](shared/src/main/kotlin/com/isycat/dotaaddon/shared/GameConfig.kt) | One source of truth consumed by **both** the Lua and JS targets |
| Shared | [`shared/.../events/WdEvents.kt`](shared/src/main/kotlin/com/isycat/dotaaddon/shared/events/WdEvents.kt) | Typed `CustomGameEventKey`s — the key's generic fixes each event's payload type on both sides |
| Lua | [`lua/.../WaveDefenseController.kt`](lua/src/main/kotlin/com/isycat/dotaaddon/WaveDefenseController.kt) | Think loop (`setContextThink`), typed `onGameEvent`/`registerListener`, unit spawning, boss AI (cast routine), order filter, pushing state via `CustomGameEventManager` |
| Lua | [`lua/.../abilities/WhirlingDeath.kt`](lua/src/main/kotlin/com/isycat/dotaaddon/abilities/WhirlingDeath.kt) | A `@Dota2Class` engine ability with typed `@AbilityKv` KeyValues (behavior/damage-type/levelled arrays/`AbilityValues`) |
| Lua | [`lua/.../modifiers/UnselectableModifier.kt`](lua/src/main/kotlin/com/isycat/dotaaddon/modifiers/UnselectableModifier.kt) | A `@Dota2Class` Lua modifier (auto-registered; applied via the typed `KClass` overload) |
| Lua | [`lua/.../bosses/`](lua/src/main/kotlin/com/isycat/dotaaddon/bosses) | Typed boss roster (`BossSpec`/`BossCast`) driving the boss-hero spell AI |
| Lua | `Main.kt`, `addon_game_mode.kt` | Boot entry + engine precache hook |
| Panorama | [`layout/game_hud.dota.xml.kts`](panorama/src/main/layout/game_hud.dota.xml.kts) | HUD authored in the Kotlin Panel DSL |
| Panorama | [`panorama/abilitybar/`](panorama/src/main/kotlin/com/isycat/dotaaddon/panorama/abilitybar), [`panorama/inventory/`](panorama/src/main/kotlin/com/isycat/dotaaddon/panorama/inventory) | Self-contained, copy-pasteable HUD modules (see [`HUD_MODULES.md`](panorama/HUD_MODULES.md)) |
| Panorama | [`panorama/panels/`](panorama/src/main/kotlin/com/isycat/dotaaddon/panorama/panels), `Manifest.kt` | Game-specific HUD panels + the default-UI manifest that mounts the HUD |

## Build & run

Requires JDK 21 and a Dota 2 install:

```
.\gradlew syncAddon      # transpile (lua + panorama) + generate KV + sync to the live addon
.\gradlew dev            # watch + re-sync on every change
```
