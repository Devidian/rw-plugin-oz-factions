# PLANS.md

- [ ] Validate the next-300926 change on Development with a controlled player check.

Planning is stored in repository-local docs.

Current Development test work: [docs/active/next-250926b-icons.md](docs/active/next-250926b-icons.md).

- Active implementation tasks: [docs/active/](docs/active/)
- Faction NPC accounts: [docs/active/faction-npc-accounts.md](docs/active/faction-npc-accounts.md)
- Roadmaps and larger plans: [docs/roadmaps/](docs/roadmaps/)
- Completed phase summaries: [docs/phase-archive.md](docs/phase-archive.md)
- Planning and documentation standards: [docs/policies/repository-policy.md](docs/policies/repository-policy.md)

Every implementation plan must include objective, ownership, dependencies, risks, validation strategy, affected repositories/plugins, rollback considerations, and a markdown checkbox checklist.

Discord event thread routing is implemented locally; Development Discord validation and the separately authorized release remain open. Configuration is documented in README.md.

- [x] Verify offline historical leadership classification and repair against fabricated SQLite snapshots.
- [ ] Inspect an authorized historical snapshot if a live inconsistency is reported, then review the repaired copy before any maintenance replacement.
