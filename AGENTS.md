# AGENTS.md

## Repository Purpose
This repository owns the `OZ - Player Factions` Rising World plugin.

It must remain usable as a standalone template repository. Workspace-root orchestration is optional and must never be required for normal use.

## Ownership
Owns:
- faction lifecycle, membership, applications and faction roles
- faction SQLite persistence, permission-group provisioning and public faction facade
- faction-account orchestration and same-faction PvP protection

Does not own:
- generic UI, settings, persistence, logging or bridge infrastructure
- resource records and consumption checks for Land Claim, Traders, Criers or Service NPCs
- Wallet balance persistence or Shop purchase orchestration

## Mandatory Workflow Rules
- Preserve the Java 20 baseline.
- Preserve Maven-based build and packaging behavior.
- Preserve GitHub tag-release compatibility.
- Keep generated plugin repositories autonomous.
- Route shared UI, i18n, settings, persistence, file-watching, and logger
  conventions through `rw-plugin-oz-tools` unless a repository-local need is
  explicitly documented.
- Follow `.codex/agents.toml` for local agent roles, task classes, context loading, and escalation.
- Follow `docs/policies/repository-policy.md` for reusable governance rules.
- Keep `README.md`, `HISTORY.md`, and `PLANS.md` aligned with structural changes.
- The `plugin.yml` entry class is the sole Rising World `Listener` and the only
  object passed to `registerEventListener(...)`.
- Keep the entry class thin: lifecycle wiring, listener registration, and
  delegation only. Event methods may only dispatch to focused handler/service
  classes; feature workflows, persistence, UI construction, integration code,
  and non-trivial event handling belong in thematic subpackages.
- Do not let delegated classes implement `net.risingworld.api.events.Listener`.
- New plugins must be created by adapting this repository, not by recreating a
  separate skeleton. Rename template packages, classes, assets, descriptor
  metadata, documentation, and player-facing labels to match the feature.
- Preserve the supplied template classes and their responsibilities. Do not
  remove them or replace their logic with a parallel implementation without a
  documented technical reason.
- Keep configuration in the supplied `PluginSettings` class and settings-file
  structure. Do not replace plugin settings with a `record` or a second
  configuration model.

## Validation
- Run `mvn -B -DskipTests package` for build-impacting changes.
- Run `mvn -B test` when tests exist.
- Review `.github/workflows/*` when release behavior or artifact names change.
- Update this template whenever a convention should apply to future plugin repositories.
