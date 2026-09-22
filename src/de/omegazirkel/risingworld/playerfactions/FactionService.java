package de.omegazirkel.risingworld.playerfactions;

import java.util.Locale;
import java.util.Optional;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.HashMap;
import java.util.Arrays;

import de.omegazirkel.risingworld.PlayerFactions;
import de.omegazirkel.risingworld.tools.PlayerDatabaseHelper;
import net.risingworld.api.Plugin;
import net.risingworld.api.objects.Player;
import net.risingworld.api.Server;

/** Domain queries used by UI, combat and optional integrations. Lifecycle mutations follow in focused handlers. */
public final class FactionService {
    private final FactionRepository repository;
    private final WalletBridge wallet;
    private final PluginSettings settings;
    private final PermissionGroupProvisioner permissions;
    private final FactionDiscordNotifier discord;
    private final Plugin plugin;
    public FactionService(Plugin plugin, FactionRepository repository, PluginSettings settings, WalletBridge wallet) {
        this.plugin = plugin; this.repository = repository; this.wallet = wallet; this.settings = settings; this.permissions = new PermissionGroupProvisioner(plugin); this.discord = new FactionDiscordNotifier(new DiscordBridge(plugin), settings, de.omegazirkel.risingworld.tools.I18n.getInstance(plugin));
    }
    public Integer factionIdFor(int playerDbId) { return repository.member(playerDbId).map(FactionMember::factionId).orElse(null); }
    public String roleFor(int playerDbId) { return repository.member(playerDbId).map(member -> member.role().name()).orElse(null); }
    public String accountIdFor(int playerDbId) { return repository.factionFor(playerDbId).map(Faction::accountId).orElse(null); }
    public Long accountBalanceFor(int playerDbId) {
        String accountId = accountIdFor(playerDbId); if (accountId == null) return null;
        String currency = wallet.defaultCurrencyIdentifier();
        return wallet.systemAccountBalances(accountId).stream().filter(balance -> balance.currencyIdentifier().equalsIgnoreCase(currency)).mapToLong(WalletBridge.SystemBalanceInfo::balance).findFirst().orElse(0L);
    }
    public List<FactionAccountBalance> accountBalancesFor(int playerDbId) {
        String accountId = accountIdFor(playerDbId);
        if (accountId == null) return List.of();
        Map<String, Long> balances = new HashMap<>();
        for (WalletBridge.SystemBalanceInfo balance : wallet.systemAccountBalances(accountId)) balances.put(balance.currencyIdentifier(), balance.balance());
        List<FactionAccountBalance> values = new java.util.ArrayList<>();
        for (WalletBridge.CurrencyInfo currency : wallet.listCurrencies()) values.add(new FactionAccountBalance(currency.identifier(), currency.name(), balances.getOrDefault(currency.identifier(), 0L)));
        if (values.isEmpty()) values.add(new FactionAccountBalance(wallet.defaultCurrencyIdentifier(), wallet.defaultCurrencyIdentifier(), balances.getOrDefault(wallet.defaultCurrencyIdentifier(), 0L)));
        return List.copyOf(values);
    }
    public boolean isLeader(int playerDbId) { return repository.member(playerDbId).map(m -> m.role() == FactionRole.LEADER).orElse(false); }
    public boolean canManageApplications(int playerDbId) { return repository.member(playerDbId).map(member -> member.role().canManageApplications()).orElse(false); }
    public boolean canManageMembers(int playerDbId) { return repository.member(playerDbId).map(member -> member.role().canManageMembers()).orElse(false); }
    public String factionName(int factionId) { return repository.faction(factionId).map(Faction::name).orElse(null); }
    public long foundationCost() { return settings.foundationCost; }
    public String currencyIdentifier() { return wallet.defaultCurrencyIdentifier(); }
    public List<WalletBridge.CurrencyInfo> currencies() { return wallet.listCurrencies(); }
    public int claimLicenseCount(int playerDbId) { return repository.factionFor(playerDbId).map(f -> settings.defaultClaimLicenses + f.extraClaimLicenses()).orElse(0); }
    public int traderLicenseCount(int playerDbId) { return repository.factionFor(playerDbId).map(f -> settings.defaultTraderLicenses + f.extraTraderLicenses()).orElse(0); }
    public int crierLicenseCount(int playerDbId) { return repository.factionFor(playerDbId).map(f -> settings.defaultCrierLicenses + f.extraCrierLicenses()).orElse(0); }
    public int serviceNpcLicenseCount(int playerDbId) { return repository.factionFor(playerDbId).map(f -> settings.defaultServiceNpcLicenses + f.extraServiceNpcLicenses()).orElse(0); }
    public int memberLimit(Faction faction) { return settings.defaultMemberLimit + faction.extraMemberSlots(); }
    public Map<Integer, PlayerDatabaseHelper.PlayerRecord> playerRecords(Set<Integer> playerDbIds) { return PlayerDatabaseHelper.findPlayersByDbIds(plugin, playerDbIds); }
    public boolean sameFaction(int firstPlayerDbId, int secondPlayerDbId) {
        Integer first = factionIdFor(firstPlayerDbId); return first != null && first.equals(factionIdFor(secondPlayerDbId));
    }
    public static Optional<String> validateName(String value) {
        if (value == null) return Optional.of("tc.factions.error.name_required");
        String name = value.trim();
        if (name.length() < 3 || name.length() > 20) return Optional.of("tc.factions.error.name_length");
        if (name.startsWith(".")) return Optional.of("tc.factions.error.name_starts_dot");
        if (!name.matches("[A-Za-z0-9 .]+")) return Optional.of("tc.factions.error.name_characters");
        int dot = name.indexOf('.');
        if (dot >= 0 && (dot != name.lastIndexOf('.') || dot == 0 || !Character.isDigit(name.charAt(dot - 1)))) return Optional.of("tc.factions.error.name_dot");
        return Optional.empty();
    }
    public static String groupNameFor(String factionName) { return factionName.trim().toLowerCase(Locale.ROOT).replaceAll(" +", "-"); }
    public Faction create(Player founder, String name, String color) {
        Optional<String> error = validateName(name); if (error.isPresent()) throw new IllegalArgumentException(error.get());
        if (factionIdFor(founder.getDbID()) != null) throw new IllegalStateException("tc.factions.error.already_member");
        String group = groupNameFor(name);
        if (repository.factionByName(name.trim()).isPresent()) throw new IllegalStateException("tc.factions.error.name_taken");
        String foundationReference = group + ":" + System.currentTimeMillis();
        String accountId = "player-factions:" + foundationReference;
        if (!wallet.hasSystemAccountApi()) throw new IllegalStateException("tc.factions.error.wallet_account_api_unavailable");
        var account = wallet.createSystemAccount(accountId, "FACTION", name, "OZPlayerFactions");
        if (!account.success()) throw new IllegalStateException("tc.factions.error.account_create_failed");
        String currency = wallet.defaultCurrencyIdentifier(); String correlation = "faction-foundation:" + founder.getDbID() + ":" + foundationReference;
        var paid = wallet.transferPlayerToSystemIdempotent(founder.getDbID(), accountId, settings.foundationCost, "Faction foundation: " + name, currency, "OZPlayerFactions", correlation);
        if (!paid.success()) throw new IllegalStateException("tc.factions.error.foundation_payment_failed");
        String factionColor = color == null || color.isBlank() ? "#FFFFFF" : color;
        boolean provisioned = false;
        try { permissions.create(group, name.trim(), factionColor); provisioned = true; permissions.reloadThrough(founder); Faction faction = repository.create(name.trim(), group, factionColor, founder.getDbID(), accountId); assignFactionMembersAfterPermissionsReload(faction); discord.founded(founder.getName(), faction.name()); return faction; }
        catch (Exception ex) {
            if (provisioned) try { permissions.remove(group); } catch (Exception ignored) { PlayerFactions.logger().error("Could not remove failed faction group " + group); }
            wallet.reverseAccountTransferIdempotent(correlation, correlation + ":reversal", "Faction foundation failed", "OZPlayerFactions");
            wallet.archiveSystemAccount(accountId, "OZPlayerFactions");
            PlayerFactions.logger().error("Could not create faction " + group + ": " + ex.getMessage());
            throw new IllegalStateException("tc.factions.error.create_failed", ex);
        }
    }
    public List<Faction> factions() { return repository.factions(); }
    public List<FactionApplication> applicationsForPlayer(int playerDbId) { return repository.applicationsForPlayer(playerDbId); }
    public List<FactionApplication> openApplications(int factionId) { return repository.applicationsForFaction(factionId, FactionApplicationStatus.OPEN, 10); }
    public void apply(int playerDbId, int factionId) {
        if (factionIdFor(playerDbId) != null) throw new IllegalStateException("tc.factions.error.member_cannot_apply");
        if (repository.faction(factionId).isEmpty()) throw new IllegalArgumentException("tc.factions.error.faction_not_found");
        if (repository.applicationsForPlayer(playerDbId).stream().anyMatch(a -> a.status() == FactionApplicationStatus.OPEN)) throw new IllegalStateException("tc.factions.error.application_already_open");
        if (repository.applicationsForPlayer(playerDbId).stream().anyMatch(a -> a.factionId() == factionId)) throw new IllegalStateException("tc.factions.error.application_remove_before_reapply");
        repository.apply(factionId, playerDbId);
    }
    public void accept(int actorDbId, int applicationId) {
        FactionApplication application = repository.application(applicationId).orElseThrow(() -> new IllegalArgumentException("Application does not exist."));
        Faction faction = repository.faction(application.factionId()).orElseThrow();
        requireManager(actorDbId, faction.id());
        if (application.status() != FactionApplicationStatus.OPEN) throw new IllegalStateException("Application is no longer open.");
        if (repository.memberCount(faction.id()) >= settings.defaultMemberLimit + faction.extraMemberSlots()) throw new IllegalStateException("Faction member limit reached.");
        repository.addMember(faction.id(), application.playerDbId(), FactionRole.NEWCOMER, System.currentTimeMillis());
        repository.updateApplication(applicationId, FactionApplicationStatus.WITHDRAWN);
        assignFactionMember(application.playerDbId(), faction); discord.member("tc.factions.discord.joined", playerName(application.playerDbId()), faction.name());
    }
    public void reject(int actorDbId, int applicationId) { FactionApplication a=repository.application(applicationId).orElseThrow(); requireManager(actorDbId,a.factionId()); if(a.status()!=FactionApplicationStatus.OPEN)throw new IllegalStateException("Application is no longer open."); repository.updateApplication(applicationId,FactionApplicationStatus.REJECTED); }
    public void withdraw(int playerDbId,int applicationId) { FactionApplication a=repository.application(applicationId).orElseThrow(); if(a.playerDbId()!=playerDbId)throw new IllegalArgumentException("Not your application."); if(a.status()!=FactionApplicationStatus.OPEN)throw new IllegalStateException("This application cannot be withdrawn."); repository.updateApplication(applicationId,FactionApplicationStatus.WITHDRAWN); }
    public void removeApplication(int playerDbId,int applicationId) { FactionApplication a=repository.application(applicationId).orElseThrow(); if(a.playerDbId()!=playerDbId)throw new IllegalArgumentException("Not your application."); if(a.status()==FactionApplicationStatus.OPEN)throw new IllegalStateException("Withdraw the application first."); repository.deleteApplication(applicationId); }
    public void updateColor(Player actor,String color) {
        Faction faction=repository.factionFor(actor.getDbID()).orElseThrow(() -> new IllegalStateException("Not in a faction."));
        if(faction.leaderDbId()!=actor.getDbID()) throw new IllegalStateException("tc.factions.error.only_leader_change_color");
        String normalized = color == null ? "" : color.trim();
        if(!normalized.startsWith("#")) normalized = "#" + normalized;
        if(!normalized.matches("#[0-9A-Fa-f]{6}")) throw new IllegalArgumentException("tc.factions.error.invalid_color");
        String factionColor = normalized.toUpperCase(Locale.ROOT);
        try { permissions.updateMetadata(faction.groupName(), faction.name(), factionColor); }
        catch (java.io.IOException ex) { throw new IllegalStateException("Could not update faction permission group", ex); }
        repository.updateColor(faction.id(), factionColor);
    }
    public void contribute(Player player,long value,String currency) {
        if(value<=0) throw new IllegalArgumentException("Amount must be positive.");
        Faction faction=repository.factionFor(player.getDbID()).orElseThrow(() -> new IllegalStateException("Not in a faction."));
        String correlation="faction-contribution:"+faction.id()+":"+player.getDbID()+":"+System.currentTimeMillis();
        String selectedCurrency = currency == null || currency.isBlank() ? wallet.defaultCurrencyIdentifier() : currency;
        var result=wallet.transferPlayerToSystemIdempotent(player.getDbID(),faction.accountId(),value,"Faction contribution: "+faction.name(),selectedCurrency,"OZPlayerFactions",correlation);
        if(!result.success()) throw new IllegalStateException(result.message());
        repository.addContribution(player.getDbID(),value);
    }
    public void leave(Player player) { FactionMember member=repository.member(player.getDbID()).orElseThrow(() -> new IllegalStateException("Not in a faction.")); if(member.role()==FactionRole.LEADER)throw new IllegalStateException("tc.factions.error.transfer_leadership_before_leaving"); String name=repository.faction(member.factionId()).orElseThrow().name(); repository.removeMember(player.getDbID()); permissions.assignDefault(player); discord.member("tc.factions.discord.left",player.getName(),name); }
    public void remove(int actorDbId, int targetDbId) { FactionMember actor=repository.member(actorDbId).orElseThrow(); FactionMember member=repository.member(targetDbId).orElseThrow(); Faction faction=repository.faction(member.factionId()).orElseThrow(); if(actor.factionId()!=member.factionId()||!actor.role().canManageMembers()||member.role().ordinal()>=actor.role().ordinal())throw new IllegalStateException("You cannot remove this member."); repository.removeMember(targetDbId); Player target=Server.getPlayerByDbID(targetDbId); if(target!=null)permissions.assignDefault(target); discord.member("tc.factions.discord.removed",playerName(targetDbId),faction.name()); }
    public void reconcilePermissionGroup(Player player) {
        repository.factionFor(player.getDbID()).ifPresentOrElse(f -> {
            if (!f.groupName().equals(player.getPermissionGroup()) && permissionGroupAvailable(f.groupName())) permissions.assign(player, f.groupName());
        }, () -> {
            String defaultGroup = permissions.defaultGroupName();
            if (!defaultGroup.equals(player.getPermissionGroup())) permissions.assignDefault(player);
        });
    }
    private void reconcilePermissionGroups() {
        for (Player player : Server.getAllPlayers()) if (player != null) reconcilePermissionGroup(player);
    }
    private void assignFactionMembersAfterPermissionsReload(Faction faction) {
        assignFactionMembersWhenAvailable(faction, 0);
    }
    private void assignFactionMembersWhenAvailable(Faction faction, int attempts) {
        if (!permissionGroupAvailable(faction.groupName())) {
            if (attempts < 5) plugin.executeDelayed(1f, () -> assignFactionMembersWhenAvailable(faction, attempts + 1));
            return;
        }
        for (Player player : Server.getAllPlayers()) {
            if (player != null && repository.factionFor(player.getDbID()).map(current -> current.id() == faction.id()).orElse(false)) permissions.assign(player, faction.groupName());
        }
    }
    private boolean permissionGroupAvailable(String groupName) { return Arrays.stream(Server.getAllPermissionGroups()).anyMatch(groupName::equals); }
    private void assignFactionMember(int playerDbId, Faction faction) {
        if (permissionGroupAvailable(faction.groupName())) {
            Player player = Server.getPlayerByDbID(playerDbId);
            if (player != null) permissions.assign(player, faction.groupName());
        }
        plugin.executeDelayed(1f, () -> {
            if (!repository.factionFor(playerDbId).map(current -> current.id() == faction.id()).orElse(false) || !permissionGroupAvailable(faction.groupName())) return;
            Player current = Server.getPlayerByDbID(playerDbId);
            if (current != null) permissions.assign(current, faction.groupName());
        });
    }
    public List<FactionMember> membersFor(int playerDbId) { return repository.factionFor(playerDbId).map(f -> repository.members(f.id())).orElse(List.of()); }
    public void changeRole(int actorDbId, int targetDbId, FactionRole targetRole) {
        FactionMember actor=repository.member(actorDbId).orElseThrow(); FactionMember target=repository.member(targetDbId).orElseThrow();
        if(actor.factionId()!=target.factionId()) throw new IllegalArgumentException("Member is not in your faction.");
        if(actor.role()==FactionRole.LEADER) {
            if(targetRole==FactionRole.LEADER) { repository.changeRole(actorDbId,FactionRole.OFFICER); repository.changeRole(targetDbId,FactionRole.LEADER); return; }
            if(target.role()==FactionRole.LEADER) throw new IllegalStateException("Leadership must be transferred.");
            repository.changeRole(targetDbId,targetRole); return;
        }
        if(actor.role()==FactionRole.OFFICER && target.role()==FactionRole.NEWCOMER && targetRole==FactionRole.MEMBER) { repository.changeRole(targetDbId,targetRole); return; }
        throw new IllegalStateException("Your faction role cannot make this change.");
    }
    public void dissolve(Player leader) {
        Faction faction=repository.factionFor(leader.getDbID()).orElseThrow(() -> new IllegalStateException("Not in a faction."));
        if(faction.leaderDbId()!=leader.getDbID()) throw new IllegalStateException("Only the faction leader can dissolve it.");
        List<WalletBridge.SystemBalanceInfo> balances = wallet.systemAccountBalances(faction.accountId());
        boolean hasPositiveBalance = balances.stream().anyMatch(balance -> balance.balance() > 0);
        String world=wallet.worldSystemAccountId();
        if(hasPositiveBalance && (world==null||world.isBlank())) throw new IllegalStateException("tc.factions.error.world_account_unavailable");
        for (WalletBridge.SystemBalanceInfo balance : balances) {
            if (balance.balance() <= 0) continue;
            var transfer=wallet.transferSystemToSystemIdempotent(faction.accountId(),world,balance.balance(),"Faction dissolved: "+faction.name(),balance.currencyIdentifier(),"OZPlayerFactions","faction-dissolve:"+faction.id()+":"+balance.currencyIdentifier());
            if(!transfer.success()) throw new IllegalStateException("tc.factions.error.dissolution_transfer_failed");
        }
        for(FactionMember member:repository.members(faction.id())) { Player online=Server.getPlayerByDbID(member.playerDbId()); if(online!=null) permissions.assignDefault(online); }
        try { permissions.remove(faction.groupName()); } catch(Exception ex) { throw new IllegalStateException("Could not remove faction permission group",ex); }
        repository.deleteFaction(faction.id()); discord.dissolved(leader.getName(),faction.name());
        wallet.archiveSystemAccount(faction.accountId(),"OZPlayerFactions");
    }
    public record FactionAccountBalance(String currencyIdentifier, String currencyName, long balance) { }
    private void requireManager(int actorDbId,int factionId){FactionMember member=repository.member(actorDbId).orElseThrow(() -> new IllegalStateException("Not a faction member."));if(member.factionId()!=factionId||!member.role().canManageApplications())throw new IllegalStateException("Faction role is not allowed.");}
    private static String playerName(int playerDbId){String name=Server.getLastKnownPlayerName(playerDbId);return name==null||name.isBlank()?"#"+playerDbId:name;}
}
