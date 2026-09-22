# Roadmap Plan 04 Template Runtime Standards

## Objective
Align future plugin scaffolding with Plan 04 runtime standards for persistence, i18n loading, shortcut visibility, and explicit overlay close controls.

## Ownership
Primary repository: `rw-plugin-player-factions`

Supporting repositories:
- `rw-plugin-oz-tools` for shared conventions and helper APIs.

## Dependencies
- Final Tools decisions for `SQLiteConnectionFactory`, shortcut visibility, and explicit close controls.

## Phases
- [x] Phase 1: Replace any deprecated persistence examples with the current `SQLiteConnectionFactory` pattern.
- [x] Phase 2: Add the default player setting that controls whether the plugin shortcut is visible in `/ozt open` and the inventory panel; default is visible.
- [x] Phase 3: Ensure scaffolded menu/inventory shortcut registration observes the visibility setting.
- [x] Phase 4: Document the baseline Escape-close API limitation for plugin overlays.
- [x] Phase 5: Document that i18n files are loaded once during `onEnable`.
- [x] Phase 6: Update README/HISTORY and validate the template.

## Progress Notes
- Template registers a default-visible `PluginShortcutVisibility` provider and unregisters it on disable.
- Template menu items use the plugin-aware `MenuItem` constructor so `/ozt` and inventory shortcut filtering can share the same visibility decision.
- README documents one-time i18n loading, `SQLiteConnectionFactory` persistence, and defers custom-overlay Escape behavior to the future Rising World API layer.
- The template has no concrete overlay class, so Escape-close handling is documented as the baseline pattern for generated overlays rather than wired to a placeholder panel.
- Validation passed with `mvn -B test` and `mvn -B -DskipTests package`.

## Risks
- Template drift can cause future plugins to reintroduce deprecated persistence or missing player settings.
- Template code should remain generic and not include feature-plugin business logic.

## Validation Strategy
- Run `mvn -B test` in the template.
- Compare template conventions with one implemented Plan 04 feature plugin before finalizing.

## Affected Repositories/Plugins
- `rw-plugin-player-factions`
- Future Rising World plugin repositories.

## Rollback Considerations
Template changes are not runtime-installed. If shared Tools decisions change during implementation, update the template before marking the root rollout complete.
