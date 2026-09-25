# Factions Shop icons: Development follow-up

Objective: make all five Shop-extra icons available to the plugin resource loader in both styles.

Owner: OZ Player Factions. Supporting plugins: OZ Shop consumes the registered icon keys; Admin Utils is independently updated for the blueprint permission fix. No public API or persistence change.

- [x] Copy the existing icon assets into the JAR resource tree without changing their keys or images.
- [x] Run tests and package checks; verify all ten icon resources in the JAR (7 tests passed).
- [x] Upload only Factions and Admin Utils to Development and verify reload without Factions icon-resource errors.
- [x] Player confirms the Shop icons and the admin blueprint limit in game (user acceptance).

Risk: Development reload restarts every plugin; unrelated global reload errors can recur. Rollback: restore the previous Factions JAR and icon assets on Development. No database migration.

Development evidence: the uploaded Factions JAR hash matches the local scoped build, contains all ten extra-icon resources, and `OZ Player Factions enabled` plus `RELOADED ALL PLUGINS` appeared at 13:25 UTC without the previous missing-resource messages.
