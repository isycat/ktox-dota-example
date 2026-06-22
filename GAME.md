# Survival Wave Defense — ktox showcase game

A small, complete game mode that exercises every layer of the ktox Dota suite,
replacing the previous placeholder code.

## How it plays

Your hero stands in the map. Every 20 seconds a wave of enemy creeps spawns
around you; each wave is larger than the last. Killing an enemy scores points.
The HUD shows the current wave, score, enemies remaining, the countdown to the
next wave, and your hero's HP. Type **`nova`** in all-chat to detonate an AoE
blast that damages every nearby enemy. If your hero dies, it's game over.

## What each layer demonstrates

| Layer | File | ktox feature |
|-------|------|--------------|
| Shared | `shared/.../GameConfig.kt` | One source of truth (`GameConfig`, `WaveState`, `Announcement`) consumed by **both** the Lua and JS targets |
| Lua | `lua/.../WaveDefense.kt` | Game-mode think loop (`setContextThink`), typed game events via `onGameEvent`, unit spawning, entity lookup, pushing state with `CustomGameEventManager` |
| Lua | `lua/.../Nova.kt` | Spatial query (`findUnitsInRadius`), structured damage (`applyDamage`), particles (`ParticleManager`), and a `@Dota2Class` engine-bound ability |
| Lua | `lua/.../Main.kt`, `addon_game_mode.kt` | Boot entry + precache |
| Panorama | `panorama/.../layout/custom_ui_manifest.xml` | **The default-UI entrypoint** — mounts the custom HUD into the game |
| Panorama | `panorama/.../layout/game_hud.dota.xml.kts` | HUD authored in the Kotlin Panel DSL |
| Panorama | `panorama/.../panorama/GameHud.kt` | Kotlin→JS HUD logic: `GameEvents.subscribe`, `GameUI.setDefaultUIEnabled`, panel lookup, reading the shared `WaveState` |
| Panorama | `panorama/.../styles/game_hud.css` | HUD styling |

## The UI entrypoint

`custom_ui_manifest.xml` is what Dota reads to inject custom panels into the
HUD. It was missing from this project, so no custom HUD could load. It now
mounts `game_hud.xml` as a `type="Hud"` element. The HUD root panel's
`onload="gameHudInit()"` boots the controller once the script bundle is loaded.

## Build & run

This must run on your machine (the agent sandbox has no JDK 21 / Dota install):

```
.\gradlew syncAddon      # transpile + sync to the live addon
# or: .\gradlew dev       # watch + re-sync on every change
```

## Assumptions worth verifying on first build

These couldn't be compile-tested from the agent environment:

1. **Panorama script include order.** `game_hud.dota.xml.kts` includes
   `ktox_panorama.js`, `shared/GameConfig.js`, then `GameHud.js` by path
   (there's no bundle step producing `main-bundle.js` yet). If the transpiler
   emits different filenames/paths, adjust the `<scripts>` includes.
2. **`NovaAbility` binding.** The `@Dota2Class` ability is included to showcase
   the lowering; the working blast is the `nova` chat command. To make it a
   castable ability, add an `npc_abilities_custom.txt` entry
   (`BaseClass "ability_lua"`, `ScriptFile` → transpiled `NovaAbility.lua`) and
   grant it to the hero.
3. **Leftover placeholders.** `PanoramaInit.kt`, `unitCardWrapperCardPanel*`,
   `hello_hud.dota.xml.kts`, and `example_hud.xml` are no longer referenced and
   can be deleted.
