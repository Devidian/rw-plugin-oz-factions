package de.omegazirkel.risingworld.playerfactions.ui;

import de.omegazirkel.risingworld.tools.ui.BasePlayerPluginDataPanel;
import de.omegazirkel.risingworld.tools.ui.PlayerPluginData;
import net.risingworld.api.objects.Player;

/* This is an example, rename <Template> if you use this feature */

public class FactionPlayerPluginData extends PlayerPluginData {

    public FactionPlayerPluginData(String pluginLabel, String pluginVersion) {
        this.pluginLabel = pluginLabel;
        this.pluginVersion = pluginVersion;
    }

    @Override
    public BasePlayerPluginDataPanel createPlayerPluginDataUIElement(Player uiPlayer) {
        return new BasePlayerPluginDataPanel(uiPlayer, pluginLabel) {
            @Override
            protected void redrawContent() {
                flexWrapper.removeAllChilds();
                flexWrapper.addChild(defaultEmptyStateLabel());
            }
        };
    }
}
