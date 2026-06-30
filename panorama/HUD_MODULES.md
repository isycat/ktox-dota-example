# Reusable HUD modules

The custom HUD is built from two self-contained, copy-pasteable systems plus some
wave-defense-specific panels. This documents the two reusable ones — the **ability bar** and the
**inventory bar** — so you can lift either into another ktox-dota project. Each lives in its own
Panorama package (`panorama.abilitybar` / `panorama.inventory`) so the whole package folder drops in
as a unit.

Each module spans three layers (the ktox three-target split):

- **Panorama (client)** — the panels + slot views that draw the bar and turn clicks into orders.
- **Shared** — the tiny client↔server event contract (a typed event key + its payload), compiled to
  both lua and JS.
- **Lua (server)** — a single listener the host game implements to apply the one action the client can't
  do with a native engine order.

The only coupling to the host game is the typed event keys the module imports from the addon's shared
events — **not** the game's `GameConfig`:

- [`shared/events/WdEvents.kt`](../shared/src/main/kotlin/com/isycat/dotaaddon/shared/events/WdEvents.kt) —
  the typed `CustomGameEventKey`s (`WD_UPGRADE`, `WD_SWAP`); the key's generic fixes the payload type.
- Each module owns its own client refresh tuning
  ([`AbilityBarConfig`](src/main/kotlin/com/isycat/dotaaddon/panorama/abilitybar/AbilityBarConfig.kt) /
  [`InventoryConfig`](src/main/kotlin/com/isycat/dotaaddon/panorama/inventory/InventoryConfig.kt)).
- [`styles/_hud_slot.scss`](src/main/styles/_hud_slot.scss) — slot chrome (cooldown spiral/number/charges +
  `WdSlotContent`) shared by **both** bars.

Both bars are placed (as `@PanoramaView`s) in
[`game_hud.dota.xml.kts`](src/main/layout/game_hud.dota.xml.kts) and **must be `hittest = false`** on
their container (only the leaf icons/slots take hits — a full-width hit-testable container freezes the
mouse; see the container declarations).

---

## Ability bar — `panorama.abilitybar`

Draws the hero's abilities + talents, shows live cooldowns/charges, and upgrades on click.

| Layer | Files |
|-------|-------|
| Panorama | [`AbilitiesPanel.kt`](src/main/kotlin/com/isycat/dotaaddon/panorama/abilitybar/AbilitiesPanel.kt) (container, `#WdAbilities`), [`AbilitySlotView.kt`](src/main/kotlin/com/isycat/dotaaddon/panorama/abilitybar/AbilitySlotView.kt) (one slot snippet + the `AbilityUpgrade` order helper), [`AbilityBarConfig.kt`](src/main/kotlin/com/isycat/dotaaddon/panorama/abilitybar/AbilityBarConfig.kt) |
| Shared | [`UpgradeRequest.kt`](../shared/src/main/kotlin/com/isycat/dotaaddon/shared/events/UpgradeRequest.kt) (`{ slot }`), keyed by `WD_UPGRADE` |
| CSS | `_abilities.scss` + `_hud_slot.scss` |
| Config | `AbilityBarConfig.REFRESH_SECONDS`, `AbilityBarConfig.LAYOUT_SCAN_TICKS` |

**Server contract (lua):** `registerListener(WD_UPGRADE) { _, event -> … }` and upgrade `event.slot` on
the requesting player's hero. **Most** abilities and talents are levelled by a native client
`TRAIN_ABILITY` order (the engine validates points/level/max/tier itself — no server code), so this
listener is needed **only** for cases the engine rejects from a client order — e.g. the hidden +stats
attribute bonus. Always re-validate server-side (it's the attribute bonus, not maxed, hero meets the
level requirement, a point is available) since the raw `UpgradeAbility` force-levels with no checks.
See `registerUpgradeListener` in `WaveDefenseController.kt` for a reference implementation.

---

## Inventory bar — `panorama.inventory`

Draws the 6 carried + 3 backpack slots, live cooldowns/charges, with click-to-use, right-click-to-sell,
and drag-to-rearrange.

| Layer | Files |
|-------|-------|
| Panorama | [`ItemsPanel.kt`](src/main/kotlin/com/isycat/dotaaddon/panorama/inventory/ItemsPanel.kt) (container, `#WdInventory`), [`ItemSlotView.kt`](src/main/kotlin/com/isycat/dotaaddon/panorama/inventory/ItemSlotView.kt) (one slot snippet + the `ItemUse` / `ItemMove` helpers), [`ItemContextMenu.kt`](src/main/kotlin/com/isycat/dotaaddon/panorama/inventory/ItemContextMenu.kt) (right-click Sell/Keep confirm), [`InventoryConfig.kt`](src/main/kotlin/com/isycat/dotaaddon/panorama/inventory/InventoryConfig.kt) |
| Shared | [`SwapItemsRequest.kt`](../shared/src/main/kotlin/com/isycat/dotaaddon/shared/events/SwapItemsRequest.kt) (`{ fromSlot, toSlot }`), keyed by `WD_SWAP` |
| CSS | `_inventory.scss` + `_hud_slot.scss` |
| Config | `InventoryConfig.REFRESH_SECONDS` |

**Server contract (lua):** `registerListener(WD_SWAP) { _, event -> … }` and call
`swapItems(event.fromSlot, event.toSlot)` on the requesting player's hero. Re-validate that both are real
carried/backpack slots (0–8) so a forged event can't reach the stash. **Use** (left-click) and **sell**
(right-click) are native engine orders issued client-side — no server handler — but sell only works when
the player is in range of a shop, so the host game must provide one (Wave Defense makes the whole arena a
shop). See `registerSwapListener` in `WaveDefenseController.kt`. Right-click opens `ItemContextMenu`
(a confirm step) rather than selling outright; the Sell button issues the same `SELL_ITEM` order.

---

## Lifting a module into another project

1. Copy the module's Panorama **package folder** (`panorama/abilitybar` or `panorama/inventory`) into your
   `panorama` source, its `*Request.kt` payload into `shared/events`, and its SCSS partial **plus**
   `_hud_slot.scss` into your `styles`.
2. Declare the typed event key it uses in your shared events (e.g.
   `@ReplaceReferencesWithLiteral("my_upgrade") val WD_UPGRADE: CustomGameEventKey<UpgradeRequest> = externalSource()`),
   or point the module's imports at your own key.
3. `@use` the SCSS partial(s) from your HUD stylesheet.
4. Place the panel `@PanoramaView`(s) in your layout `root`, `hittest = false` on the container.
5. Implement the server listener(s) above in your lua game mode.

Nothing else references the game's `GameConfig`, so there's no wave-defense logic to untangle.
