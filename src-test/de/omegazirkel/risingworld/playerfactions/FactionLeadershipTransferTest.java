package de.omegazirkel.risingworld.playerfactions;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;

import org.junit.Test;

import net.risingworld.api.Plugin;

public class FactionLeadershipTransferTest {
    @Test
    public void leadershipTransferPersistsTheLeaderAndBothRoles() throws Exception {
        Path file = Files.createTempFile("faction-leadership", ".db");
        try {
            try (Connection db = DriverManager.getConnection("jdbc:sqlite:" + file)) {
                FactionRepository repository = new FactionRepository(db);
                repository.initialize();
                Faction faction = repository.create("Alpha", "alpha", "#FFFFFF", 10, "faction:alpha");
                repository.addMember(faction.id(), 20, FactionRole.MEMBER, 2);
                repository.transferLeadership(faction.id(), 10, 20);
            }
            try (Connection db = DriverManager.getConnection("jdbc:sqlite:" + file)) {
                FactionRepository repository = new FactionRepository(db);
                assertEquals(20, repository.factionFor(20).orElseThrow().leaderDbId());
                assertEquals(10, repository.factionFor(20).orElseThrow().founderDbId());
                assertEquals(FactionRole.OFFICER, repository.member(10).orElseThrow().role());
                assertEquals(FactionRole.LEADER, repository.member(20).orElseThrow().role());
            }
        } finally {
            Files.deleteIfExists(file);
        }
    }
    @Test
    public void serviceRoutesLeadershipChangeThroughAtomicTransfer() throws Exception {
        try (Connection db = DriverManager.getConnection("jdbc:sqlite::memory:")) {
            FactionRepository repository = new FactionRepository(db);
            repository.initialize();
            Faction faction = repository.create("Alpha", "alpha", "#FFFFFF", 10, "faction:alpha");
            repository.addMember(faction.id(), 20, FactionRole.MEMBER, 2);
            var constructor = PluginSettings.class.getDeclaredConstructor();
            constructor.setAccessible(true);
            Plugin plugin = new Plugin() {
                @Override public void onEnable() { }
                @Override public void onDisable() { }
                @Override public String getDescription(String key) { return "FactionLeadershipTest"; }
                @Override public String getPath() { return ""; }
            };
            FactionService service = new FactionService(plugin, repository, constructor.newInstance(), null);
            service.changeRole(10, 20, FactionRole.LEADER);
            assertTrue(service.isLeader(20));
            assertFalse(service.isLeader(10));
            assertEquals(20, repository.faction(faction.id()).orElseThrow().leaderDbId());
        }
    }

    @Test
    public void failuresAtEveryWriteRollBackTheWholeTransfer() throws Exception {
        String[] triggers = {
            "CREATE TRIGGER fail_transfer BEFORE UPDATE OF leader_db_id ON factions BEGIN SELECT RAISE(ABORT, 'injected'); END",
            "CREATE TRIGGER fail_transfer BEFORE UPDATE OF role ON faction_members WHEN OLD.player_db_id=10 BEGIN SELECT RAISE(ABORT, 'injected'); END",
            "CREATE TRIGGER fail_transfer BEFORE UPDATE OF role ON faction_members WHEN OLD.player_db_id=20 BEGIN SELECT RAISE(ABORT, 'injected'); END"
        };
        for (String trigger : triggers) {
            try (Connection db = DriverManager.getConnection("jdbc:sqlite::memory:")) {
                FactionRepository repository = new FactionRepository(db);
                repository.initialize();
                Faction faction = repository.create("Alpha", "alpha", "#FFFFFF", 10, "faction:alpha");
                repository.addMember(faction.id(), 20, FactionRole.MEMBER, 2);
                db.createStatement().execute(trigger);
                try {
                    repository.transferLeadership(faction.id(), 10, 20);
                    fail("Injected write failure must abort transfer");
                } catch (IllegalStateException expected) {
                    assertTrue(db.getAutoCommit());
                }
                assertEquals(10, repository.faction(faction.id()).orElseThrow().leaderDbId());
                assertEquals(FactionRole.LEADER, repository.member(10).orElseThrow().role());
                assertEquals(FactionRole.MEMBER, repository.member(20).orElseThrow().role());
            }
        }
    }

    @Test
    public void staleSelfAndDifferentFactionTransfersAreRejected() throws Exception {
        try (Connection db = DriverManager.getConnection("jdbc:sqlite::memory:")) {
            FactionRepository repository = new FactionRepository(db);
            repository.initialize();
            Faction alpha = repository.create("Alpha", "alpha", "#FFFFFF", 10, "faction:alpha");
            Faction beta = repository.create("Beta", "beta", "#FFFFFF", 30, "faction:beta");
            repository.addMember(alpha.id(), 20, FactionRole.MEMBER, 2);
            assertThrows(() -> repository.transferLeadership(alpha.id(), 10, 10));
            assertThrows(() -> repository.transferLeadership(alpha.id(), 10, 30));
            repository.transferLeadership(alpha.id(), 10, 20);
            assertThrows(() -> repository.transferLeadership(alpha.id(), 10, 20));
            assertEquals(20, repository.faction(alpha.id()).orElseThrow().leaderDbId());
            assertEquals(30, repository.faction(beta.id()).orElseThrow().leaderDbId());
            assertEquals(FactionRole.OFFICER, repository.member(10).orElseThrow().role());
            assertEquals(FactionRole.LEADER, repository.member(20).orElseThrow().role());
        }
    }

    @Test
    public void legacyDisagreementIsLeftUntouched() throws Exception {
        try (Connection db = DriverManager.getConnection("jdbc:sqlite::memory:")) {
            FactionRepository repository = new FactionRepository(db);
            repository.initialize();
            Faction faction = repository.create("Alpha", "alpha", "#FFFFFF", 10, "faction:alpha");
            repository.addMember(faction.id(), 20, FactionRole.MEMBER, 2);
            repository.addMember(faction.id(), 30, FactionRole.MEMBER, 3);
            repository.changeRole(10, FactionRole.OFFICER);
            repository.changeRole(20, FactionRole.LEADER);
            assertThrows(() -> repository.transferLeadership(faction.id(), 20, 30));
            assertEquals(10, repository.faction(faction.id()).orElseThrow().leaderDbId());
            assertEquals(FactionRole.LEADER, repository.member(20).orElseThrow().role());
            assertEquals(FactionRole.MEMBER, repository.member(30).orElseThrow().role());
        }
    }

    @Test
    public void multipleLeaderRolesAreRejectedWithoutModification() throws Exception {
        try (Connection db = DriverManager.getConnection("jdbc:sqlite::memory:")) {
            FactionRepository repository = new FactionRepository(db);
            repository.initialize();
            Faction faction = repository.create("Alpha", "alpha", "#FFFFFF", 10, "faction:alpha");
            repository.addMember(faction.id(), 20, FactionRole.MEMBER, 2);
            repository.addMember(faction.id(), 30, FactionRole.LEADER, 3);
            assertThrows(() -> repository.transferLeadership(faction.id(), 10, 20));
            assertEquals(10, repository.faction(faction.id()).orElseThrow().leaderDbId());
            assertEquals(FactionRole.LEADER, repository.member(10).orElseThrow().role());
            assertEquals(FactionRole.MEMBER, repository.member(20).orElseThrow().role());
            assertEquals(FactionRole.LEADER, repository.member(30).orElseThrow().role());
        }
    }

    @Test
    public void failedNestedTransferPreservesEarlierCallerChanges() throws Exception {
        try (Connection db = DriverManager.getConnection("jdbc:sqlite::memory:")) {
            FactionRepository repository = new FactionRepository(db);
            repository.initialize();
            Faction faction = repository.create("Alpha", "alpha", "#FFFFFF", 10, "faction:alpha");
            repository.addMember(faction.id(), 20, FactionRole.MEMBER, 2);
            db.createStatement().execute("CREATE TRIGGER fail_transfer BEFORE UPDATE OF role ON faction_members "
                    + "WHEN OLD.player_db_id=20 BEGIN SELECT RAISE(ABORT, 'injected'); END");
            db.setAutoCommit(false);
            repository.updateColor(faction.id(), "#123456");
            assertThrows(() -> repository.transferLeadership(faction.id(), 10, 20));
            assertEquals("#123456", repository.faction(faction.id()).orElseThrow().color());
            assertEquals(10, repository.faction(faction.id()).orElseThrow().leaderDbId());
            assertEquals(FactionRole.LEADER, repository.member(10).orElseThrow().role());
            assertEquals(FactionRole.MEMBER, repository.member(20).orElseThrow().role());
            db.commit();
            assertEquals("#123456", repository.faction(faction.id()).orElseThrow().color());
        }
    }

    @Test
    public void nestedTransferDoesNotCommitTheCallersTransaction() throws Exception {
        try (Connection db = DriverManager.getConnection("jdbc:sqlite::memory:")) {
            FactionRepository repository = new FactionRepository(db);
            repository.initialize();
            Faction faction = repository.create("Alpha", "alpha", "#FFFFFF", 10, "faction:alpha");
            repository.addMember(faction.id(), 20, FactionRole.MEMBER, 2);
            db.setAutoCommit(false);
            repository.transferLeadership(faction.id(), 10, 20);
            assertFalse(db.getAutoCommit());
            assertEquals(20, repository.faction(faction.id()).orElseThrow().leaderDbId());
            db.rollback();
            assertEquals(10, repository.faction(faction.id()).orElseThrow().leaderDbId());
            assertEquals(FactionRole.LEADER, repository.member(10).orElseThrow().role());
            assertEquals(FactionRole.MEMBER, repository.member(20).orElseThrow().role());
        }
    }

    private static void assertThrows(Runnable action) {
        try {
            action.run();
            fail("Invalid transfer should be rejected");
        } catch (IllegalArgumentException | IllegalStateException expected) {
            // Expected.
        }
    }

}
