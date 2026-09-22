package de.omegazirkel.risingworld.playerfactions;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Optional;
import java.util.List;

/** Local SQLite persistence; no Wallet or permissions state is duplicated here. */
public final class FactionRepository {
    private final Connection db;
    public FactionRepository(Connection db) { this.db = db; }
    public void initialize() {
        try (Statement s = db.createStatement()) {
            s.execute("PRAGMA foreign_keys = ON");
            s.executeUpdate("CREATE TABLE IF NOT EXISTS factions (id INTEGER PRIMARY KEY AUTOINCREMENT, name TEXT NOT NULL COLLATE NOCASE UNIQUE, group_name TEXT NOT NULL UNIQUE, color TEXT NOT NULL, founded_at INTEGER NOT NULL, founder_db_id INTEGER NOT NULL, leader_db_id INTEGER NOT NULL UNIQUE, account_id TEXT NOT NULL UNIQUE, extra_member_slots INTEGER NOT NULL DEFAULT 0, extra_claim_licenses INTEGER NOT NULL DEFAULT 0, extra_trader_licenses INTEGER NOT NULL DEFAULT 0, extra_crier_licenses INTEGER NOT NULL DEFAULT 0, extra_service_npc_licenses INTEGER NOT NULL DEFAULT 0)");
            s.executeUpdate("CREATE TABLE IF NOT EXISTS faction_members (faction_id INTEGER NOT NULL REFERENCES factions(id) ON DELETE CASCADE, player_db_id INTEGER NOT NULL UNIQUE, joined_at INTEGER NOT NULL, contributed INTEGER NOT NULL DEFAULT 0, role TEXT NOT NULL, PRIMARY KEY(faction_id, player_db_id))");
            s.executeUpdate("CREATE TABLE IF NOT EXISTS faction_applications (id INTEGER PRIMARY KEY AUTOINCREMENT, faction_id INTEGER NOT NULL REFERENCES factions(id) ON DELETE CASCADE, player_db_id INTEGER NOT NULL, status TEXT NOT NULL, created_at INTEGER NOT NULL, updated_at INTEGER NOT NULL, UNIQUE(faction_id, player_db_id))");
            s.executeUpdate("DELETE FROM faction_members WHERE faction_id NOT IN (SELECT id FROM factions)");
            s.executeUpdate("DELETE FROM faction_applications WHERE faction_id NOT IN (SELECT id FROM factions)");
        } catch (SQLException ex) { throw new IllegalStateException("Cannot initialize faction database", ex); }
    }
    public Optional<FactionMember> member(int playerDbId) {
        String sql = "SELECT faction_id,player_db_id,joined_at,contributed,role FROM faction_members WHERE player_db_id=?";
        try (PreparedStatement s = db.prepareStatement(sql)) { s.setInt(1, playerDbId); try (ResultSet r=s.executeQuery()) { return r.next() ? Optional.of(new FactionMember(r.getInt(1),r.getInt(2),r.getLong(3),r.getLong(4),FactionRole.valueOf(r.getString(5)))) : Optional.empty(); } }
        catch (SQLException ex) { throw new IllegalStateException("Cannot load faction member", ex); }
    }
    public Optional<Faction> faction(int factionId) { return faction("SELECT * FROM factions WHERE id=?", factionId); }
    public Optional<Faction> factionByName(String name) {
        try (PreparedStatement s = db.prepareStatement("SELECT * FROM factions WHERE name=? COLLATE NOCASE")) { s.setString(1, name); try (ResultSet r = s.executeQuery()) { return r.next() ? Optional.of(readFaction(r)) : Optional.empty(); } }
        catch (SQLException ex) { throw new IllegalStateException("Cannot load faction", ex); }
    }
    public Optional<Faction> factionFor(int playerDbId) { return faction("SELECT f.* FROM factions f JOIN faction_members m ON m.faction_id=f.id WHERE m.player_db_id=?", playerDbId); }
    private Optional<Faction> faction(String sql, int value) {
        try (PreparedStatement s=db.prepareStatement(sql)) { s.setInt(1,value); try(ResultSet r=s.executeQuery()){ return r.next()?Optional.of(readFaction(r)):Optional.empty(); } }
        catch(SQLException ex){ throw new IllegalStateException("Cannot load faction",ex); }
    }
    private static Faction readFaction(ResultSet r) throws SQLException { return new Faction(r.getInt("id"),r.getString("name"),r.getString("group_name"),r.getString("color"),r.getLong("founded_at"),r.getInt("founder_db_id"),r.getInt("leader_db_id"),r.getString("account_id"),r.getInt("extra_member_slots"),r.getInt("extra_claim_licenses"),r.getInt("extra_trader_licenses"),r.getInt("extra_crier_licenses"),r.getInt("extra_service_npc_licenses")); }
    public synchronized Faction create(String name, String groupName, String color, int founderDbId, String accountId) {
        long now = System.currentTimeMillis();
        try (PreparedStatement statement = db.prepareStatement("INSERT INTO factions(name,group_name,color,founded_at,founder_db_id,leader_db_id,account_id) VALUES(?,?,?,?,?,?,?)", Statement.RETURN_GENERATED_KEYS)) {
            statement.setString(1, name); statement.setString(2, groupName); statement.setString(3, color); statement.setLong(4, now); statement.setInt(5, founderDbId); statement.setInt(6, founderDbId); statement.setString(7, accountId); statement.executeUpdate();
            try (ResultSet keys = statement.getGeneratedKeys()) { if (!keys.next()) throw new SQLException("No faction id returned"); int id = keys.getInt(1); addMember(id, founderDbId, FactionRole.LEADER, now); return faction(id).orElseThrow(); }
        } catch (SQLException ex) { throw new IllegalStateException("Cannot create faction", ex); }
    }
    public synchronized void addMember(int factionId, int playerDbId, FactionRole role, long joinedAt) {
        try (PreparedStatement s = db.prepareStatement("INSERT INTO faction_members(faction_id,player_db_id,joined_at,role) VALUES(?,?,?,?)")) { s.setInt(1,factionId);s.setInt(2,playerDbId);s.setLong(3,joinedAt);s.setString(4,role.name());s.executeUpdate(); }
        catch(SQLException ex){ throw new IllegalStateException("Cannot add faction member",ex); }
    }
    public synchronized int memberCount(int factionId) { try(PreparedStatement s=db.prepareStatement("SELECT COUNT(*) FROM faction_members WHERE faction_id=?")){s.setInt(1,factionId);try(ResultSet r=s.executeQuery()){return r.next()?r.getInt(1):0;}}catch(SQLException ex){throw new IllegalStateException("Cannot count faction members",ex);} }
    public List<Faction> factions() { try(PreparedStatement s=db.prepareStatement("SELECT * FROM factions ORDER BY founded_at ASC");ResultSet r=s.executeQuery()){java.util.ArrayList<Faction> all=new java.util.ArrayList<>();while(r.next())all.add(readFaction(r));return List.copyOf(all);}catch(SQLException ex){throw new IllegalStateException("Cannot list factions",ex);} }
    public synchronized void apply(int factionId, int playerDbId) {
        long now=System.currentTimeMillis();
        try(PreparedStatement s=db.prepareStatement("INSERT INTO faction_applications(faction_id,player_db_id,status,created_at,updated_at) VALUES(?,?,?,?,?)")){s.setInt(1,factionId);s.setInt(2,playerDbId);s.setString(3,FactionApplicationStatus.OPEN.name());s.setLong(4,now);s.setLong(5,now);s.executeUpdate();}
        catch(SQLException ex){throw new IllegalStateException("Cannot submit faction application",ex);}
    }
    public List<FactionApplication> applicationsForFaction(int factionId, FactionApplicationStatus status, int limit) { return applications("SELECT * FROM faction_applications WHERE faction_id=? AND status=? ORDER BY created_at ASC LIMIT ?", factionId,status,limit); }
    public List<FactionApplication> applicationsForPlayer(int playerDbId) { try(PreparedStatement s=db.prepareStatement("SELECT * FROM faction_applications WHERE player_db_id=? ORDER BY updated_at DESC")){s.setInt(1,playerDbId);try(ResultSet r=s.executeQuery()){java.util.ArrayList<FactionApplication> all=new java.util.ArrayList<>();while(r.next())all.add(readApplication(r));return List.copyOf(all);}}catch(SQLException ex){throw new IllegalStateException("Cannot load player applications",ex);} }
    private List<FactionApplication> applications(String sql,int factionId,FactionApplicationStatus status,int limit){try(PreparedStatement s=db.prepareStatement(sql)){s.setInt(1,factionId);s.setString(2,status.name());s.setInt(3,limit);try(ResultSet r=s.executeQuery()){java.util.ArrayList<FactionApplication> all=new java.util.ArrayList<>();while(r.next())all.add(readApplication(r));return List.copyOf(all);}}catch(SQLException ex){throw new IllegalStateException("Cannot load faction applications",ex);}}
    private static FactionApplication readApplication(ResultSet r)throws SQLException{return new FactionApplication(r.getInt("id"),r.getInt("faction_id"),r.getInt("player_db_id"),FactionApplicationStatus.valueOf(r.getString("status")),r.getLong("created_at"),r.getLong("updated_at"));}
    public synchronized Optional<FactionApplication> application(int id){try(PreparedStatement s=db.prepareStatement("SELECT * FROM faction_applications WHERE id=?")){s.setInt(1,id);try(ResultSet r=s.executeQuery()){return r.next()?Optional.of(readApplication(r)):Optional.empty();}}catch(SQLException ex){throw new IllegalStateException("Cannot load application",ex);}}
    public synchronized void updateApplication(int id,FactionApplicationStatus status){try(PreparedStatement s=db.prepareStatement("UPDATE faction_applications SET status=?,updated_at=? WHERE id=?")){s.setString(1,status.name());s.setLong(2,System.currentTimeMillis());s.setInt(3,id);s.executeUpdate();}catch(SQLException ex){throw new IllegalStateException("Cannot update application",ex);}}
    public synchronized void deleteApplication(int id){try(PreparedStatement s=db.prepareStatement("DELETE FROM faction_applications WHERE id=?")){s.setInt(1,id);s.executeUpdate();}catch(SQLException ex){throw new IllegalStateException("Cannot delete application",ex);}}
    public synchronized void removeMember(int playerDbId){try(PreparedStatement s=db.prepareStatement("DELETE FROM faction_members WHERE player_db_id=?")){s.setInt(1,playerDbId);s.executeUpdate();}catch(SQLException ex){throw new IllegalStateException("Cannot remove faction member",ex);}}
    public synchronized void changeRole(int playerDbId,FactionRole role){try(PreparedStatement s=db.prepareStatement("UPDATE faction_members SET role=? WHERE player_db_id=?")){s.setString(1,role.name());s.setInt(2,playerDbId);s.executeUpdate();}catch(SQLException ex){throw new IllegalStateException("Cannot change faction role",ex);}}
    public synchronized void updateColor(int factionId,String color){try(PreparedStatement s=db.prepareStatement("UPDATE factions SET color=? WHERE id=?")){s.setString(1,color);s.setInt(2,factionId);s.executeUpdate();}catch(SQLException ex){throw new IllegalStateException("Cannot update faction color",ex);}}
    public synchronized void addContribution(int playerDbId,long value){try(PreparedStatement s=db.prepareStatement("UPDATE faction_members SET contributed=contributed+? WHERE player_db_id=?")){s.setLong(1,value);s.setInt(2,playerDbId);s.executeUpdate();}catch(SQLException ex){throw new IllegalStateException("Cannot update faction contribution",ex);}}
    public List<FactionMember> members(int factionId){try(PreparedStatement s=db.prepareStatement("SELECT faction_id,player_db_id,joined_at,contributed,role FROM faction_members WHERE faction_id=? ORDER BY joined_at ASC")){s.setInt(1,factionId);try(ResultSet r=s.executeQuery()){java.util.ArrayList<FactionMember> all=new java.util.ArrayList<>();while(r.next())all.add(new FactionMember(r.getInt(1),r.getInt(2),r.getLong(3),r.getLong(4),FactionRole.valueOf(r.getString(5))));return List.copyOf(all);}}catch(SQLException ex){throw new IllegalStateException("Cannot load faction members",ex);}}
    public synchronized void deleteFaction(int factionId){
        try {
            db.setAutoCommit(false);
            try (PreparedStatement applications=db.prepareStatement("DELETE FROM faction_applications WHERE faction_id=?");
                 PreparedStatement members=db.prepareStatement("DELETE FROM faction_members WHERE faction_id=?");
                 PreparedStatement faction=db.prepareStatement("DELETE FROM factions WHERE id=?")) {
                applications.setInt(1,factionId); applications.executeUpdate();
                members.setInt(1,factionId); members.executeUpdate();
                faction.setInt(1,factionId); faction.executeUpdate();
            }
            db.commit();
        } catch(SQLException ex){
            try { db.rollback(); } catch(SQLException rollback) { ex.addSuppressed(rollback); }
            throw new IllegalStateException("Cannot delete faction",ex);
        } finally { try { db.setAutoCommit(true); } catch(SQLException ex) { throw new IllegalStateException("Cannot restore database transaction mode",ex); } }
    }
}
