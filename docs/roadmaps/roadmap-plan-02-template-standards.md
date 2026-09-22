# Roadmap Plan 02 Template Standards

## Objective
Align the template with the Roadmap Plan 02 shared conventions after Tools defines stable APIs for plugin access buttons, indicators, info/status panels, settings metadata, and logger naming.

## Ownership
Primary repository: `rw-plugin-player-factions`.

Supporting repositories:
- `rw-plugin-oz-tools` provides the shared runtime contracts.
- Existing plugins prove the conventions before they become the future-plugin baseline.

## Dependencies
- Tools shared UI/settings contracts must be decided before the template scaffolds examples.

## Work Packages
- [x] Package 1: Add baseline documentation for registering plugin buttons in the shared inventory helper panel.
- [x] Package 2: Add baseline documentation for registering shared indicator providers.
- [x] Package 3: Add baseline documentation and placeholders for plugin info/status provider integration.
- [x] Package 4: Update settings metadata examples to include grouped sections and numeric-entry safety expectations.
- [x] Package 5: Update logger examples to use exactly one main plugin logger.
- [x] Package 6: Update README/HISTORY/AGENTS guidance so new plugins route shared UI, i18n, settings, and persistence through Tools.

## Progress Notes
- Template standards packages are complete: the template now scaffolds shared Tools inventory buttons, shared indicator providers, plugin info/status providers, grouped admin settings metadata, one-main-logger usage, and documentation for future plugin adoption.

## Risks
- Updating the template before the Tools API is stable would create churn for future plugins.
- Template examples must remain generic and not encode Shop, Marketplace, GPS, or Intercom business behavior.

## Validation Strategy
- Verify the template package still builds.
- Verify generated or copied baseline docs match the adopted portfolio conventions.

## Affected Repositories/Plugins
- `rw-plugin-player-factions`
- `rw-plugin-oz-tools`

## Rollback Considerations
Template-only changes can be reverted without changing released plugins, as long as no new plugin was generated from the intermediate template.
