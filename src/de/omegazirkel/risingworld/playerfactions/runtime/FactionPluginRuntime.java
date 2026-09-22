package de.omegazirkel.risingworld.playerfactions.runtime;

import java.nio.file.Path;
import java.sql.Connection;

import de.omegazirkel.risingworld.PlayerFactions;
import de.omegazirkel.risingworld.playerfactions.FactionRepository;
import de.omegazirkel.risingworld.playerfactions.FactionService;
import de.omegazirkel.risingworld.playerfactions.PluginGUI;
import de.omegazirkel.risingworld.playerfactions.PluginSettings;
import de.omegazirkel.risingworld.playerfactions.WalletBridge;
import de.omegazirkel.risingworld.playerfactions.ui.FactionPluginInfoStatusProvider;
import de.omegazirkel.risingworld.tools.I18n;
import de.omegazirkel.risingworld.tools.db.SQLiteConnectionFactory;
import de.omegazirkel.risingworld.tools.settings.PlayerPluginAdminSettings;
import de.omegazirkel.risingworld.tools.ui.InventoryOverlayButtons;
import de.omegazirkel.risingworld.tools.ui.MenuItem;
import de.omegazirkel.risingworld.tools.ui.PlayerPluginSettingsOverlay;
import de.omegazirkel.risingworld.tools.ui.PluginMenuManager;
import de.omegazirkel.risingworld.tools.ui.PluginInfoStatusProviders;
import de.omegazirkel.risingworld.tools.ui.PluginShortcutVisibility;

/** Runtime composition root; it owns feature services, never event registration. */
public final class FactionPluginRuntime {
    public static final String COMMAND = "faction";
    private final PlayerFactions plugin;
    private final PluginSettings settings;
    private FactionRepository repository;
    private FactionService service;
    private FactionPlayerEventHandler commands;
    private FactionCombatHandler combat;
    public FactionPluginRuntime(PlayerFactions plugin) { this.plugin = plugin; settings = PluginSettings.getInstance(plugin); }
    public void enable() {
        settings.initSettings();
        Connection database = SQLiteConnectionFactory.open(plugin);
        repository = new FactionRepository(database);
        repository.initialize();
        service = new FactionService(plugin, repository, settings, new WalletBridge(plugin));
        PluginGUI.getInstance(plugin).configure(service);
        commands = new FactionPlayerEventHandler(plugin, service, PluginGUI.getInstance(plugin), I18n.getInstance(plugin));
        combat = new FactionCombatHandler(service);
        String name = plugin.getDescription("name");
        PluginMenuManager.registerPluginMenu(new MenuItem(name, "player-factions", name, p -> PluginGUI.getInstance().openMainMenu(p)));
        PluginShortcutVisibility.register(name, p -> true);
        InventoryOverlayButtons.registerButton(name, name, "player-factions", e -> PluginGUI.getInstance().openMainMenu(e.getPlayer()));
        PlayerPluginSettingsOverlay.registerPlayerPluginAdminSettings(new PlayerPluginAdminSettings(name, plugin.getDescription("version"), settings::adminSettingsEntries, settings::initSettings));
        PluginInfoStatusProviders.registerProvider(new FactionPluginInfoStatusProvider(name, plugin.getDescription("version"), COMMAND));
        PlayerFactions.logger().info("OZ Player Factions enabled");
    }
    public void disable() { String name = plugin.getDescription("name"); InventoryOverlayButtons.unregisterButtons(name); PluginShortcutVisibility.unregister(name); PluginInfoStatusProviders.unregisterProvider(name); }
    public void reloadSettings(Path path) { settings.initSettings(path.toString()); }
    public FactionService service() { return service; }
    public FactionPlayerEventHandler commands() { return commands; }
    public FactionCombatHandler combat() { return combat; }
}
