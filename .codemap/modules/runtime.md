# Lifecycle and player UI
<!-- codemap-id: rw-plugin-oz-player-factions/runtime -->

## Responsibility

Connect faction events, permission groups, settings and UI.

## Source and tests

- [`FactionPluginRuntime`](../../src/de/omegazirkel/risingworld/playerfactions/runtime/FactionPluginRuntime.java) — lifecycle wiring.
- [`PermissionGroupProvisioner`](../../src/de/omegazirkel/risingworld/playerfactions/PermissionGroupProvisioner.java) — permission-group provisioning.
- [`FactionOverlay`](../../src/de/omegazirkel/risingworld/playerfactions/ui/FactionOverlay.java) — player UI.
- [`PluginSettings`](../../src/de/omegazirkel/risingworld/playerfactions/PluginSettings.java) — settings.
- [Tests](../../src-test/de/omegazirkel/risingworld/playerfactions/) cover repository behavior. Verify relevant scenarios before implementing changes.
- [Repository index](../README.md) and [settings defaults](../../src/settings.default.json).
