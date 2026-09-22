package de.omegazirkel.risingworld.playerfactions;

import java.util.ArrayList;
import java.util.List;

import de.omegazirkel.risingworld.tools.ui.AssetManager;
import de.omegazirkel.risingworld.tools.I18n;
import de.omegazirkel.risingworld.tools.ui.MenuItem;
import de.omegazirkel.risingworld.tools.ui.PluginInfoStatusProviders;
import de.omegazirkel.risingworld.tools.ui.PluginMenuManager;
import de.omegazirkel.risingworld.playerfactions.ui.FactionOverlay;
import net.risingworld.api.Plugin;
import net.risingworld.api.objects.Player;
import net.risingworld.api.ui.UITarget;

public class PluginGUI {
    private static PluginGUI instance = null;
    private String pluginName;
    private FactionService factions;

    private PluginGUI() {

    }

    public static PluginGUI getInstance(Plugin p) {

        AssetManager.loadIconFromPlugin(p, "player-factions");
        AssetManager.loadIconFromPlugin(p, "my-faction");
        PluginGUI gui = getInstance();
        gui.pluginName = p.getDescription("name");
        return gui;
    }

    public static PluginGUI getInstance() {
        if (instance == null) {
            instance = new PluginGUI();
        }
        return instance;
    }

    public void openMainMenu(Player uiPlayer) {
        List<MenuItem> menuItems = new ArrayList<>();
        menuItems.add(new MenuItem(pluginName, "my-faction", text("tc.factions.ui.menu.my_faction", uiPlayer), player -> { player.hideRadialMenu(true); openFactionOverlay(player); }));
        menuItems.add(new MenuItem(pluginName, "info-status", text("tc.factions.ui.menu.info_status", uiPlayer), player -> {
            player.hideRadialMenu(true);
            PluginInfoStatusProviders.show(player, pluginName);
        }));
        menuItems.add(MenuItem.closeMenu(uiPlayer));
        PluginMenuManager.showMenu(uiPlayer, menuItems);
    }
    public void configure(FactionService service) { factions = service; }
    public void openFactionOverlay(Player player) {
        if (factions == null) return;
        Object current = player.getAttribute("player-factions.overlay");
        if (current instanceof FactionOverlay overlay) player.removeUIElement(overlay);
        FactionOverlay overlay = new FactionOverlay(player, pluginName, factions);
        player.addUIElement(overlay, UITarget.Modal);
        player.setAttribute("player-factions.overlay", overlay);
    }

    private String text(String key, Player player) { return I18n.getInstance(pluginName).get(key, player); }

}
