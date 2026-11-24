# rw-plugin-oz-player-factions
<!-- codemap-id: rw-plugin-oz-player-factions -->

## Purpose

Faction lifecycle, membership, roles, permissions, accounts and same-faction PvP protection. [Descriptor](../src/resources/plugin.yml) names [`PlayerFactions`](../src/de/omegazirkel/risingworld/PlayerFactions.java); [`FactionPluginRuntime`](../src/de/omegazirkel/risingworld/playerfactions/runtime/FactionPluginRuntime.java) handles lifecycle and dispatch.

## Modules

- [Faction service and public facade](modules/factions.md).
- [Lifecycle and player UI](modules/runtime.md).

## Build and tests

[Maven](../pom.xml) builds against the Rising World API and OZ Tools. Explore [tests](../src-test/de/omegazirkel/risingworld/playerfactions/) and follow [repository instructions](../AGENTS.md) for validation.

## Dependencies

- `rw-plugin-oz-tools/shared-api` — build and shared runtime infrastructure. [Build configuration](../pom.xml)
