package de.omegazirkel.risingworld;

import java.nio.file.Path;

import de.omegazirkel.risingworld.playerfactions.runtime.FactionPluginRuntime;
import de.omegazirkel.risingworld.tools.FileChangeListener;
import de.omegazirkel.risingworld.tools.OZLogger;
import net.risingworld.api.Plugin;
import net.risingworld.api.events.EventMethod;
import net.risingworld.api.events.Listener;
import net.risingworld.api.events.player.PlayerCommandEvent;
import net.risingworld.api.events.player.PlayerConnectEvent;
import net.risingworld.api.events.player.PlayerHitPlayerEvent;
import net.risingworld.api.events.player.PlayerSpawnEvent;

/** Sole Rising World listener; feature behavior is delegated to the runtime. */
public final class PlayerFactions extends Plugin implements Listener, FileChangeListener {
    private FactionPluginRuntime runtime;
    public static OZLogger logger() { return OZLogger.getInstance("OZPlayerFactions"); }
    @Override public void onEnable() { runtime = new FactionPluginRuntime(this); runtime.enable(); registerEventListener(this); }
    @Override public void onDisable() { unregisterEventListener(this); if (runtime != null) runtime.disable(); }
    @Override public void onSettingsChanged(Path path) { if (runtime != null) runtime.reloadSettings(path); }
    @EventMethod public void command(PlayerCommandEvent event) { runtime.commands().onPlayerCommand(event); }
    @EventMethod public void playerHit(PlayerHitPlayerEvent event) { runtime.combat().onPlayerHit(event); }
    @EventMethod public void playerSpawn(PlayerSpawnEvent event) { runtime.service().reconcilePermissionGroup(event.getPlayer()); }
    @EventMethod public void playerConnect(PlayerConnectEvent event) { runtime.service().reconcilePermissionGroup(event.getPlayer()); }
    /** Optional public facade for sibling plugins through the Tools bridge. */
    public Integer getPlayerFactionId(int playerDbId) { return runtime == null ? null : runtime.service().factionIdFor(playerDbId); }
    public String getPlayerFactionRole(int playerDbId) { return runtime == null ? null : runtime.service().roleFor(playerDbId); }
    public String getPlayerFactionAccountId(int playerDbId) { return runtime == null ? null : runtime.service().accountIdFor(playerDbId); }
    public Long getPlayerFactionAccountBalance(int playerDbId) { return runtime == null ? null : runtime.service().accountBalanceFor(playerDbId); }
    public Integer getPlayerFactionClaimLicenseCount(int playerDbId) { return runtime == null || runtime.service().factionIdFor(playerDbId) == null ? null : runtime.service().claimLicenseCount(playerDbId); }
    public Integer getPlayerFactionTraderLicenseCount(int playerDbId) { return runtime == null || runtime.service().factionIdFor(playerDbId) == null ? null : runtime.service().traderLicenseCount(playerDbId); }
    public Integer getPlayerFactionCrierLicenseCount(int playerDbId) { return runtime == null || runtime.service().factionIdFor(playerDbId) == null ? null : runtime.service().crierLicenseCount(playerDbId); }
    public Integer getPlayerFactionServiceLicenseCount(int playerDbId) { return runtime == null || runtime.service().factionIdFor(playerDbId) == null ? null : runtime.service().serviceNpcLicenseCount(playerDbId); }
    public String getFactionName(int factionId) { return runtime == null ? null : runtime.service().factionName(factionId); }
    public boolean isPlayerFactionLeader(int playerDbId) { return runtime != null && runtime.service().isLeader(playerDbId); }
    public boolean canPlayerFactionLeader(int playerDbId) { return isPlayerFactionLeader(playerDbId); }
    public boolean canPlayerFactionManageClaim(int playerDbId) { return isPlayerFactionLeader(playerDbId); }
    public boolean canPlayerFactionManageTrader(int playerDbId) { return isPlayerFactionLeader(playerDbId); }
    public boolean canPlayerFactionManageCrier(int playerDbId) { return isPlayerFactionLeader(playerDbId); }
    public boolean canPlayerFactionManageServiceNPC(int playerDbId) { return isPlayerFactionLeader(playerDbId); }
}
