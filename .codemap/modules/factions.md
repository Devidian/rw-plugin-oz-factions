# Faction service and public facade
<!-- codemap-id: rw-plugin-oz-player-factions/factions -->

## Responsibility

Own faction membership, applications, roles and SQLite. The facade exposes faction identity, role, accounts and resource license queries to sibling plugins.

## Source and tests

- [`PlayerFactions`](../../src/de/omegazirkel/risingworld/PlayerFactions.java) — public facade and event listener.
- [`FactionService`](../../src/de/omegazirkel/risingworld/playerfactions/FactionService.java) — faction workflows.
- [`FactionRepository`](../../src/de/omegazirkel/risingworld/playerfactions/FactionRepository.java) — SQLite data.
- [`FactionCombatHandler`](../../src/de/omegazirkel/risingworld/playerfactions/runtime/FactionCombatHandler.java) — PvP event handler.
- [Leadership transfer tests](../../src-test/de/omegazirkel/risingworld/playerfactions/FactionLeadershipTransferTest.java) cover atomic role and leader updates, failure rollback and stale requests. [Other tests](../../src-test/de/omegazirkel/risingworld/playerfactions/) cover repository behavior.
- [Historical leadership repair](../../scripts/repair-faction-leadership.py) inspects isolated SQLite snapshots and can write a separately reviewed copy.
- [Repository index](../README.md) and [settings defaults](../../src/settings.default.json).

## Dependencies

- `rw-plugin-oz-wallet/ledger` — runtime faction system accounts. [Integration source](../../src/de/omegazirkel/risingworld/PlayerFactions.java)
- `rw-plugin-oz-shop/api` — optional faction offer registration. [Integration source](../../src/de/omegazirkel/risingworld/playerfactions/FactionShopBridge.java)
- `rw-plugin-oz-service-npc/npcs` — optional faction resource counting through `getFactionServiceNpcCount`. [Call site](../../src/de/omegazirkel/risingworld/playerfactions/FactionService.java)
