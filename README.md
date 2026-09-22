# OZ - Factions

OZ - Factions lets players found and manage one faction each. It owns
faction membership, applications, faction permission groups, faction accounts
and same-faction PvP protection.

## Dependencies

- Required: `OZ - Tools`
- Optional: `OZ - Wallet` for founding costs and faction accounts
- Optional: `OZ - Discord Connect` for configured event messages

Land Claims, Traders, Criers and Service NPCs remain their own plugins. They
consume the public faction facade in later, separately released integrations.

## Configuration

`settings.default.json` defines member limits, founding cost, default resource
licenses and optional Discord channel IDs. Runtime settings are world-scoped as
`settings.<world>.json` and managed through the shared Tools settings UI.

When a compatible OZ - Shop is already loaded, its plugin-offers tab contains
five leader-only faction extras. They debit the faction account in Wallet's
default currency and credit the world account; duplicate callback delivery is
deduplicated by the durable Shop correlation ID.

## Runtime behavior

- The server's configured default permission group is copied for new factions.
- A faction name is case-insensitively unique and is mapped to a safe group
  filename.
- Faction creation charges the player and creates a Wallet system account.
- On dissolution, every positive faction-account currency balance is settled
  idempotently to the world account before local state is removed. A failed
  transfer leaves the faction intact for a safe retry.
- The plugin exposes its public facade through the entry plugin for optional
  OZ and external consumers; callers must tolerate this plugin being absent.

## Development

Run `mvn -B -DskipTests package` and `mvn -B test`. When the local Maven cache
is unavailable, use an isolated cache with `-Dmaven.repo.local=/tmp/...`.
