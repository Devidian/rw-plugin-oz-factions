# Roadmap Plan 01 Template Baseline

## Objective
Prepare `rw-plugin-player-factions` before creating new plugins so Shop and Marketplace start with the current Tools dependency, shared design guidance, settings reload support, and admin settings tab integration.

## Ownership
Primary repository: `rw-plugin-player-factions`.

Supporting repositories:
- `rw-plugin-oz-tools` supplies the shared UI/settings APIs and `DESIGN.md` guidance.

## Dependencies
- Template should reference the latest compatible `rw-plugin-oz-tools` release after the Tools prework is implemented.
- Java 20, Maven packaging, CI, release, and documentation conventions remain unchanged.

## Confirmed Decisions
- New plugin repositories should be created on GitHub directly and pushed after this baseline is ready.
- `reloadOnChange` defaults to `true`.
- Template should include the admin `PluginSettings` tab integration and metadata pattern, including hidden sensitive values.
- `DESIGN.md` must be present as a synchronized local copy in each generated repository, with root as the source of truth.
- Admin settings editor v1 supports booleans, integers, and strings only.
- New plugin repositories should be created only after the Tools and Template baselines are validated and pushed.

## Work Packages
- [x] Package 1: Update the template to the latest released/validated Tools dependency after the Tools roadmap lands.
- [x] Package 2: Add template-level `DESIGN.md` as a synchronized copy of the root design guide.
- [x] Package 3: Ensure the template implements settings reload through the shared watcher mechanism.
- [x] Package 4: Add sample metadata registration for the admin-only `PluginSettings` tab.
- [x] Package 5: Keep `README.md`, `HISTORY.md`, `PLANS.md`, policies, and runtime smoke-test docs aligned.
- [x] Package 6: Validate a generated plugin skeleton can compile and package before Shop/Marketplace are created.

## Step 3 Result
- Updated the template Tools dependency to the validated 0.18.0 shared settings baseline.
- Kept the synchronized `DESIGN.md` copy in the template repository.
- Added `reloadOnChange=true` to the default settings file and kept `FileChangeListener` reload wiring in the template main plugin.
- Registered sample admin-only `PluginSettings` metadata with editable boolean/string values and a hidden sensitive-value example.
- Updated README and HISTORY with the new baseline expectations.
- Validated the template skeleton with Maven package and test goals.

## Risks
- New repositories created before this baseline would need immediate cleanup.
- Template code must remain generic and not mention Shop or Marketplace business logic.

## Validation Strategy
- Run Maven package and tests in the template.
- Create a temporary generated plugin from the template and verify package names, plugin metadata, settings reload, and admin settings tab sample wiring.
- Verify GitHub workflow placeholders remain clear for new repositories.

## Affected Repositories/Plugins
- `rw-plugin-player-factions`
- future `rw-plugin-oz-shop`
- future `rw-plugin-oz-marketplace`

## Rollback Considerations
Template changes are structural. Roll back by reverting the template baseline commit before creating new repositories from it.

## Open Questions
- None.
