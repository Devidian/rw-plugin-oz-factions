#!/usr/bin/env python3
"""Inspect legacy faction leadership or write a repaired SQLite copy."""
import argparse
from contextlib import closing
import json
import os
from pathlib import Path
import sqlite3
from tempfile import TemporaryDirectory


def inspect(connection):
    factions = dict(connection.execute("SELECT id, leader_db_id FROM factions ORDER BY id"))
    assigned = {leader: faction for faction, leader in factions.items()}
    candidates = {faction: [] for faction in factions}
    for faction, player in connection.execute("""
        SELECT faction_id, player_db_id FROM faction_members
        WHERE role='LEADER'
        ORDER BY joined_at, player_db_id
        """):
        if faction in candidates:
            candidates[faction].append(player)

    repairs = []
    unresolved = []
    for faction, stored in factions.items():
        leaders = candidates[faction]
        if stored > 0 and leaders == [stored]:
            continue
        eligible = [leader for leader in leaders if leader > 0]
        if not eligible:
            unresolved.append({"faction": faction, "reason": "no eligible leader"})
            continue
        keeper = stored if stored in eligible else eligible[0]
        if keeper in assigned and assigned[keeper] != faction:
            unresolved.append({"faction": faction, "reason": "leader pointer belongs to another faction"})
            continue
        repairs.append({
            "faction": faction, "previous_leader": stored, "retained_leader": keeper,
            "demoted_leaders": [leader for leader in leaders if leader != keeper],
        })
    return {"repairs": repairs, "unresolved": unresolved}


def update_copy(connection, report):
    with connection:
        for repair in report["repairs"]:
            faction = repair["faction"]
            keeper = repair["retained_leader"]
            demoted = connection.execute("""
                UPDATE faction_members SET role='OFFICER'
                WHERE faction_id=? AND role='LEADER' AND player_db_id<>?
                """, (faction, keeper))
            pointer = connection.execute("""
                UPDATE factions SET leader_db_id=? WHERE id=? AND leader_db_id=?
                """, (keeper, faction, repair["previous_leader"]))
            if demoted.rowcount != len(repair["demoted_leaders"]) or pointer.rowcount != 1:
                raise sqlite3.IntegrityError(f"Unexpected leadership row count for faction {faction}")
        for repair in report["repairs"]:
            actual = connection.execute("""
                SELECT m.player_db_id, f.leader_db_id
                FROM faction_members m JOIN factions f ON f.id=m.faction_id
                WHERE m.faction_id=? AND m.role='LEADER'
                """, (repair["faction"],)).fetchall()
            keeper = repair["retained_leader"]
            if actual != [(keeper, keeper)]:
                raise sqlite3.IntegrityError(f"Leadership verification failed for faction {repair['faction']}")


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("database", type=Path, help="isolated SQLite source snapshot")
    parser.add_argument("--output", type=Path, help="new file for the repaired copy")
    args = parser.parse_args()
    source = args.database.resolve(strict=True)
    output = args.output.absolute() if args.output else None
    if output and (output.exists() or output.is_symlink()):
        parser.error("output must be a new file")
    with closing(sqlite3.connect(source.as_uri() + "?mode=ro", uri=True)) as db:
        if output:
            # A separate SQLite backup includes committed WAL data and leaves the source untouched.
            with TemporaryDirectory(prefix=".faction-leadership-", dir=output.parent) as scratch:
                copy = Path(scratch) / "factions.sqlite"
                with closing(sqlite3.connect(copy)) as candidate:
                    db.backup(candidate)
                    candidate.execute("PRAGMA journal_mode=DELETE")
                    candidate.execute("PRAGMA foreign_keys=ON")
                    report = inspect(candidate)
                    update_copy(candidate, report)
                    if candidate.execute("PRAGMA quick_check").fetchall() != [("ok",)]:
                        raise sqlite3.IntegrityError("Repaired SQLite copy failed quick_check")
                # Publish atomically and fail if the destination appeared after the initial check.
                copy.chmod(0o600)
                os.link(copy, output)
        else:
            db.execute("BEGIN")
            report = inspect(db)
    print(json.dumps(report, indent=2))


if __name__ == "__main__":
    main()
