# Reusable HUD modules

The custom HUD is built from two self-contained, copy-pasteable systems plus some
wave-defense-specific panels. This documents the two reusable ones — the **ability bar** and the
**inventory bar** — so you can lift either into another ktox-dota project.

Each module spans three layers (the ktox three-target split):

- **Panorama (client)** — the panels + slot views that draw the bar and turn clicks into orders.
- **Shared** — the tiny client↔server event contract (event name + payload), compiled to both lua and JS.
- **Lua (server)** — a single listener the host game implements to apply the one action the client can't
  do with a native engine order.

They depend only on this module-owned contract — **not** on the game's `GameConfig`:

- [`shared/HudEvents.kt`](../shared/src/main/kotlin/com/isycat/dotaaddon/shared/HudEvents.kt) — the event names.
- [`panorama/HudConfig.kt`](src/main/kotlin/com/isycat/dotaaddon/panorama/HudConfig.kt) — client refresh tuning.
- [`styles/_hud_slot.scss`](src/main/styles/_hud_slot.scss) — slot chrome (cooldown spiral/number/charges +
  `WdSlotContent`) shared by **both** bars.

Both bars are placed (as `@PanoramaView`s) in
[`game_hud.dota.xml.kts`](src/main/layout/game_hud.dota.xml.kts) and **must be `hittest = false`** on
their container (only the leaf icons/slots take hits — a full-width hit-testable container freezes the
mouse; see the container declarations).

---

## Ability bar

Draws the hero's abilities + talents, shows live cooldowns/charges, and upgrades on click.

| Layer | Files |
|-------|-------|
| Panorama | [`AbilitiesPanel.kt`](src/main/kotlin/com/isycat/dotaaddon/panorama/AbilitiesPanel.kt) (container, `#WdAbilities`), [`AbilitySlotView.kt`](src/main/kotlin/com/isycat/dotaaddon/panorama/AbilitySlotView.kt) (one slot snippet + the `AbilityUpgrade` order helper) |
| Shared | [`UpgradeRequest.kt`](../shared/src/main/kotlin/com/isycat/dotaaddon/shared/UpgradeRequest.kt) (`{ slot }`), `HudEvents.UPGRADE_ABILITY` |
| CSS | `_abilities.scss` + `_hud_slot.scss` |
| Config | `HudConfig.REFRESH_SECONDS`, `HudConfig.ABILITY_LAYOUT_SCAN_TICKS` |

**Server contract (lua):** listen for `HudEvents.UPGRADE_ABILITY` (`{ slot }`) and upgrade that ability on
the requesting player's hero. **Most** abilities and talents are levelled by a native client
`TRAIN_ABILITY` order (the engine validates points/level/max/tier itself — no server code), so this
listener is needed **only** for cases the engine rejects from a client order — e.g. the hidden +stats
attribute bonus. Always re-validate server-side (it's the attribute bonus, not maxed, hero meets the
level requirement, a point is available) since the raw `UpgradeAbility` force-levels with no checks.
See `registerUpgradeListener` in `WaveDefense.kt` for a reference implementation.

---

## Inventory bar

Draws the 6 carried + 3 backpack slots, live cooldowns/charges, with click-to-use, right-click-to-sell,
and drag-to-rearrange.

| Layer | Files |
|-------|-------|
| Panorama | [`ItemsPanel.kt`](src/main/kotlin/com/isycat/dotaaddon/panorama/ItemsPanel.kt) (container, `#WdInventory`), [`ItemSlotView.kt`](src/main/kotlin/com/isycat/dotaaddon/panorama/ItemSlotView.kt) (one slot snippet + the `ItemUse` / `ItemMove` helpers) |
| Shared | [`SwapItemsRequest.kt`](../shared/src/main/kotlin/com/isycat/dotaaddon/shared/SwapItemsRequest.kt) (`{ fromSlot, toSlot }`), `HudEvents.SWAP_ITEMS` |
| CSS | `_inventory.scss` + `_hud_slot.scss` |
| Config | `HudConfig.REFRESH_SECONDS` |

**Server contract (lua):** listen for `HudEvents.SWAP_ITEMS` (`{ fromSlot, toSlot }`) and call
`swapItems(from, to)` on the requesting player's hero. Re-validate that both are real carried/backpack
slots (0–8) so a forged event can't reach the stash. **Use** (left-click) and **sell** (right-click) are
native engine orders issued client-side — no server handler — but sell only works when the player is in
range of a shop, so the host game must provide one (Wave Defense makes the whole arena a shop). See
`registerSwapListener` in `WaveDefense.kt`.

---

## Lifting a module into another project

1. Copy the module's Panorama file(s) into your `panorama` source, its `*Request.kt` into `shared`, and
   its SCSS partial **plus** `_hud_slot.scss` into your `styles`.
2. Copy `HudEvents.kt` (or merge its constants) and `HudConfig.kt`.
3. `@use` the SCSS partial(s) from your HUD stylesheet.
4. Place the panel `@PanoramaView`(s) in your layout `root`, `hittest = false` on the container.
5. Implement the server listener(s) above in your lua game mode.

Nothing else references the game's `GameConfig`, so there's no wave-defense logic to untangle.
