package de.omegazirkel.risingworld.playerfactions;

import static org.junit.Assert.assertEquals;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import org.junit.Test;
import de.omegazirkel.risingworld.tools.I18n;
import de.omegazirkel.risingworld.tools.bridge.AuditEvent;
import de.omegazirkel.risingworld.tools.bridge.EventBridge;

public class FactionEventLogSettingsTest {
    @Test public void settingsGateDynamicMembershipEventsAndReload() throws Exception {
        Path file = Files.createTempDirectory("factions-event-settings").resolve("settings.world.json");
        Files.writeString(file, "{\"eventLogs\":{\"tc\":{\"factions\":{\"discord\":{\"joined\":{\"enabled\":false}}}}}}");
        PluginSettings settings = PluginSettings.getInstance();
        settings.initSettings(file.toString());
        var received = new ArrayList<AuditEvent>();
        try (var registration = EventBridge.register(received::add)) {
            FactionDiscordNotifier notifier = new FactionDiscordNotifier(null, settings, I18n.getInstance("test-factions"), "test");
            notifier.member(1, "tc.factions.discord.joined", "Player", "Faction");
            notifier.member(1, "tc.factions.discord.left", "Player", "Faction");
            assertEquals(1, received.size());
            assertEquals("tc.factions.discord.left", received.get(0).code());
            Files.writeString(file, "{}");
            settings.initSettings(file.toString());
            notifier.member(1, "tc.factions.discord.joined", "Player", "Faction");
            assertEquals(2, received.size());
        }
    }
}
