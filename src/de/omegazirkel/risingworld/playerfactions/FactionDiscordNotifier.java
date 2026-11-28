package de.omegazirkel.risingworld.playerfactions;

import java.time.Instant;
import java.util.Map;

import de.omegazirkel.risingworld.tools.I18n;
import de.omegazirkel.risingworld.tools.bridge.AuditEvent;
import de.omegazirkel.risingworld.tools.bridge.EventMessage;

/** Reports faction changes to the optional event provider. */
final class FactionDiscordNotifier {
    private final I18n i18n;
    private final PluginSettings settings;

    FactionDiscordNotifier(DiscordBridge discord, PluginSettings settings, I18n i18n, String displayName) {
        this.i18n = i18n;
        this.settings = settings;
    }

    void founded(int id, String playerName, String factionName) {
        send(id, playerName, factionName, "tc.factions.discord.founded", "lifecycle", "");
    }

    void dissolved(int id, String playerName, String factionName) {
        send(id, playerName, factionName, "tc.factions.discord.dissolved", "lifecycle", "");
    }

    void member(int id, String key, String playerName, String factionName) {
        send(id, playerName, factionName, key, "membership", "");
    }

    void extra(int id, String playerName, String factionName, String extra) {
        send(id, playerName, factionName, "tc.factions.discord.extra", "extras", extra);
    }

    private void send(int id, String playerName, String factionName, String key, String channel, String extra) {
        settings.eventLogs.publish(key, () -> {
            String snapshot = playerName == null || playerName.equals("#" + id) ? "-" : playerName;
            return new AuditEvent(id, snapshot,
                    EventMessage.localized(i18n, key, Map.of("PH_PLAYER", snapshot, "PH_FACTION", factionName,
                            "PH_EXTRA", extra)), Instant.now(), key, "oz-player-factions", channel);
        });
    }
}
