# Faction NPC ownership and accounts

## Objective and constraints

- [x] Allow leaders and officers to manage faction criers and traders.
- [x] Expose narrowly scoped, idempotent faction-account payout and reversal operations for Shop.
- [x] Refuse dissolution while faction criers or traders exist.

## Affected systems and validation

Faction facade, roles, account service, dissolution. Test roles, missing dependencies, transfer idempotency and dissolution guards; run Maven tests and package.

Java 25 package passed on 2026-10-14 and Development startup was checked. Live payout, role, and dissolution flows remain open for player validation.

## Risks and rollback

Back up the faction database before Development upload. Roll back all four plugin jars together after removing new faction NPCs and resolving open trades. Never let an older plugin dissolve a faction while its NPCs remain.
