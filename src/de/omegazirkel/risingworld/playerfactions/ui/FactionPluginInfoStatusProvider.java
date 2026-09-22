package de.omegazirkel.risingworld.playerfactions.ui;

import de.omegazirkel.risingworld.playerfactions.PluginSettings;
import de.omegazirkel.risingworld.tools.I18n;
import de.omegazirkel.risingworld.tools.ui.PluginInfoStatusProvider;
import net.risingworld.api.objects.Player;

public class FactionPluginInfoStatusProvider implements PluginInfoStatusProvider {
    private final String pluginName;
    private final String version;
    private final String command;

    public FactionPluginInfoStatusProvider(String pluginName, String version, String command) {
        this.pluginName = pluginName;
        this.version = version;
        this.command = command;
    }

    @Override
    public String getPluginName() {
        return pluginName;
    }

    @Override
    public String getInfo(Player player) {
        return t().get("tc.factions.info", player)
                .replace("PH_PLUGIN_NAME", pluginName)
                .replace("PH_PLUGIN_VERSION", version)
                .replace("PH_PLUGIN_CMD", command);
    }

    @Override
    public String getStatus(Player player) {
        PluginSettings settings = PluginSettings.getInstance();
        return t().get("tc.factions.status", player)
                .replace("PH_MEMBER_LIMIT", String.valueOf(settings.defaultMemberLimit))
                .replace("PH_FOUNDATION_COST", String.valueOf(settings.foundationCost));
    }

    private I18n t() {
        return I18n.getInstance(pluginName);
    }
}
