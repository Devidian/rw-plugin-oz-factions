package de.omegazirkel.risingworld.playerfactions.ui;

import de.omegazirkel.risingworld.tools.ui.BasePlayerPluginSettingsPanel;
import de.omegazirkel.risingworld.tools.ui.PlayerPluginSettings;
import net.risingworld.api.objects.Player;
import net.risingworld.api.ui.UILabel;

/* This is an example, rename <Template> if you use this feature */

public class FactionPlayerPluginSettings extends PlayerPluginSettings {

    public FactionPlayerPluginSettings(String pluginLabel, String pluginVersion) {
        this.pluginLabel = pluginLabel;
        this.pluginVersion = pluginVersion;
    }

    @Override
    public BasePlayerPluginSettingsPanel createPlayerPluginSettingsUIElement(Player uiPlayer) {
        return new BasePlayerPluginSettingsPanel(uiPlayer, pluginLabel) {

            @Override
            protected void redrawContent() {
                flexWrapper.removeAllChilds();
                // TODO: implement actual settings content for PlayerFactions plugin
                UILabel placeholderLabel = new UILabel("PlayerFactions plugin settings will be here.");
                flexWrapper.addChild(placeholderLabel);
            }

        };
    }

}
