# Roadmap Plan 03 Template Wallet And Menu Standards

## Objective
Update the template so future Rising World plugins inherit the canonical WalletBridge integration pattern and a radial-menu Info/Status button baseline.

## Ownership
Primary repository: `rw-plugin-player-factions`

Supporting repositories:
- `rw-plugin-oz-wallet` for public Wallet API and bridge contract.
- `rw-plugin-oz-tools` for shared radial menu and Info/Status panel contracts.

## Dependencies
- Template examples must preserve the Java 20 baseline.
- WalletBridge examples must not create a hard compile-time dependency on Wallet for optional integrations.
- Info/Status button examples must route through Tools shared UI conventions.
- The template should reference the Tools-registered shared Info/Status icon key instead of registering its own copy.

## Phases
- [x] Phase 1: Add or update a template WalletBridge example that includes all canonical Wallet methods from Roadmap Plan 03.
- [x] Phase 2: Document when plugins should use the bridge, when they should keep Wallet as a hard functional dependency, and how missing Wallet disables economy features.
- [x] Phase 3: Add a template radial-menu Info/Status action that opens the shared Tools Info/Status panel for the plugin and uses the shared Tools-registered Info/Status icon.
- [x] Phase 4: Update template README/PLANS/HISTORY guidance for the new standards.
- [x] Phase 5: Validate template build/tests.

## Risks
- Template bridge examples can quickly become stale if Wallet public methods change without coordinated documentation.
- Adding Wallet examples must not imply every future plugin is an economy plugin.
- Menu examples must stay generic and avoid feature-plugin business logic.

## Validation Strategy
- Run `mvn -B -DskipTests package`.
- Run `mvn -B test`.
- Verify generated/example code compiles without requiring Wallet classes directly.

## Affected Repositories/Plugins
- `rw-plugin-player-factions`
- Future plugin repositories created from the template

## Rollback Considerations
Template changes do not affect deployed plugins directly. If a convention is rejected, remove or revise the scaffold before creating new plugin repositories from it.

## Progress Notes
- Phase 1-4 complete: the template includes a reflection-based `WalletBridge` scaffold with Wallet availability, default currency, currency listing, currency registration, deposit, withdraw, balance, default convenience methods, and public currency metadata parsing.
- The template plugin-owned radial menu now includes an `Info / Status` action that uses the shared Tools `info-status` key and opens the existing Tools Info/Status panel.
- Phase 5 complete: `mvn -B test` passed.
