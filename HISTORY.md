# History

## Unreleased

## [0.2.1] - 2026-09-25 | Shop extra icons

- fix: include all five Shop extra icons in the plugin JAR for both modern and classic styles.

## [0.2.0] - 2026-09-22 | Shop extras

- feat: register five leader-only faction extras with compatible OZ - Shop
  installations. Payments move from the faction account to the world account;
  durable correlation IDs prevent a repeated completion from adding capacity twice.
- fix: retain non-members' existing permission groups during login/spawn
  reconciliation; only explicit faction exits reset a player to the default group.

## [0.1.0] - 2026-09-22 | Initial release

- feat: let players found, manage and dissolve factions with applications,
  roles, permission groups and same-faction PvP protection.
- feat: provide faction accounts with selectable currencies and transfer all
  positive balances safely to the world account on dissolution.
- fix: provision the faction permission group for non-administrators through
  the temporary `reloadpermissions` permission workaround.
