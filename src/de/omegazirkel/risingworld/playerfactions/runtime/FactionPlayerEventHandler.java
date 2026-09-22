package de.omegazirkel.risingworld.playerfactions.runtime;

import java.util.Arrays;
import de.omegazirkel.risingworld.PlayerFactions;
import de.omegazirkel.risingworld.playerfactions.FactionService;
import de.omegazirkel.risingworld.playerfactions.PluginGUI;
import de.omegazirkel.risingworld.tools.I18n;
import net.risingworld.api.Server;
import net.risingworld.api.events.player.PlayerCommandEvent;
import net.risingworld.api.objects.Player;

/** Command dispatch only; UI and domain operations remain below their owners. */
public final class FactionPlayerEventHandler {
    private final PlayerFactions plugin;
    private final FactionService service;
    private final PluginGUI gui;
    private final I18n i18n;
    FactionPlayerEventHandler(PlayerFactions plugin, FactionService service, PluginGUI gui, I18n i18n) {
        this.plugin = plugin; this.service = service; this.gui = gui; this.i18n = i18n;
    }
    public void onPlayerCommand(PlayerCommandEvent event) {
        String[] parts = event.getCommand().trim().split("\\s+", 2);
        if (!parts[0].equalsIgnoreCase("/" + FactionPluginRuntime.COMMAND) && !parts[0].equalsIgnoreCase("/factions")) return;
        Player player = event.getPlayer();
        String action = parts.length == 1 ? "open" : parts[1].toLowerCase(java.util.Locale.ROOT);
        if ("open".equals(action)) gui.openMainMenu(player);
        else if ("status".equals(action)) player.sendTextMessage(plugin.getName() + ": " + (service.factionIdFor(player.getDbID()) == null ? "no faction" : "faction member"));
        else if ("debug-groups".equals(action)) debugGroups(player);
        else player.sendTextMessage(i18n.get("tc.err.cmd.unknown", player).replace("PH_PLUGIN_CMD", FactionPluginRuntime.COMMAND));
    }
    private void debugGroups(Player player) {
        if (!player.isAdmin()) { player.sendTextMessage(i18n.get("tc.factions.debug.admin_only", player)); return; }
        String groups = Arrays.stream(Server.getAllPermissionGroups()).sorted().reduce((left, right) -> left + ", " + right).orElse(i18n.get("tc.factions.debug.none", player));
        player.sendTextMessage(i18n.get("tc.factions.debug.groups", player).replace("PH_CURRENT", String.valueOf(player.getPermissionGroup())).replace("PH_GROUPS", groups));
    }
}
