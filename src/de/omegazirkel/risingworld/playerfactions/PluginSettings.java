package de.omegazirkel.risingworld.playerfactions;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import de.omegazirkel.risingworld.PlayerFactions;
import de.omegazirkel.risingworld.tools.settings.AdminSettingsEntry;
import de.omegazirkel.risingworld.tools.settings.AdminSettingsType;
import de.omegazirkel.risingworld.tools.settings.JsonSettingsFile;
import de.omegazirkel.risingworld.tools.settings.SettingsFileEditor;
import net.risingworld.api.World;

/** Template JSON-settings responsibility adapted to faction balancing. */
public final class PluginSettings {
    private static PluginSettings instance;
    private static PlayerFactions plugin;
    public int defaultMemberLimit = 10;
    public long foundationCost = 5000L;
    public long memberSlotBaseCost = 1000L;
    public double memberSlotIncrease = .1d;
    public int defaultClaimLicenses = 1, defaultTraderLicenses = 0, defaultCrierLicenses = 1, defaultServiceNpcLicenses = 2;
    public long discordLifecycleChannelId, discordMembershipChannelId, discordExtrasChannelId;
    private Path settingsFile;
    private Map<String, String> current = Map.of(), defaults = Map.of();
    public static PluginSettings getInstance(PlayerFactions value) { plugin = value; return getInstance(); }
    public static PluginSettings getInstance() { if (instance == null) instance = new PluginSettings(); return instance; }
    private PluginSettings() { }
    public void initSettings() { Path path = Path.of(plugin.getPath() == null ? "." : plugin.getPath()); initSettings(path.resolve("settings." + safeWorldName() + ".json").toString()); }
    public void initSettings(String filePath) {
        settingsFile = Path.of(filePath); Path defaultFile = settingsFile.resolveSibling("settings.default.json");
        try {
            if (Files.notExists(settingsFile) && Files.exists(defaultFile)) JsonSettingsFile.writeFlatAtomically(settingsFile, JsonSettingsFile.loadFlat(defaultFile));
            current = JsonSettingsFile.loadFlat(settingsFile); defaults = JsonSettingsFile.loadFlat(defaultFile);
            defaultMemberLimit = integer("defaultMemberLimit", 10); foundationCost = positiveLong("foundationCost", 5000L);
            memberSlotBaseCost = positiveLong("memberSlotBaseCost", 1000L); memberSlotIncrease = decimal("memberSlotIncrease", .1d);
            defaultClaimLicenses = integer("defaultClaimLicenses", 1); defaultTraderLicenses = integer("defaultTraderLicenses", 0);
            defaultCrierLicenses = integer("defaultCrierLicenses", 1); defaultServiceNpcLicenses = integer("defaultServiceNpcLicenses", 2);
            discordLifecycleChannelId = positiveLong("discordLifecycleChannelId", 0L); discordMembershipChannelId = positiveLong("discordMembershipChannelId", 0L); discordExtrasChannelId = positiveLong("discordExtrasChannelId", 0L);
        } catch (IOException | NumberFormatException ex) { throw new IllegalStateException("Cannot load faction settings", ex); }
    }
    public long memberSlotCost(int alreadyPurchased) { return Math.round(memberSlotBaseCost + (memberSlotBaseCost * memberSlotIncrease) * Math.max(0, alreadyPurchased)); }
    public List<AdminSettingsEntry> adminSettingsEntries() { return Arrays.asList(
            AdminSettingsEntry.group("factions", "Factions", "Faction limits and prices."),
            entry("defaultMemberLimit", "Default member limit", "Members available before extra slots.", AdminSettingsType.INTEGER),
            entry("foundationCost", "Foundation cost", "Default-currency cost for a new faction.", AdminSettingsType.INTEGER),
            entry("memberSlotBaseCost", "Extra member slot cost", "Base cost for an extra member slot.", AdminSettingsType.INTEGER),
            entry("memberSlotIncrease", "Extra slot price increase", "Increase per previously purchased slot.", AdminSettingsType.STRING),
            entry("defaultClaimLicenses", "Default claim licenses", "Faction claim licenses before extras.", AdminSettingsType.INTEGER),
            entry("defaultTraderLicenses", "Default trader licenses", "Faction trader licenses before extras.", AdminSettingsType.INTEGER),
            entry("defaultCrierLicenses", "Default crier licenses", "Faction crier licenses before extras.", AdminSettingsType.INTEGER),
            entry("defaultServiceNpcLicenses", "Default service NPC licenses", "Faction service licenses before extras.", AdminSettingsType.INTEGER),
            AdminSettingsEntry.group("discord", "Discord", "Optional Discord Connect event channels."),
            entry("discordLifecycleChannelId", "Faction lifecycle channel", "Creation and dissolution events; zero disables it.", AdminSettingsType.STRING),
            entry("discordMembershipChannelId", "Faction membership channel", "Join, leave and removal events; zero disables it.", AdminSettingsType.STRING),
            entry("discordExtrasChannelId", "Faction extras channel", "Extra-license purchase events; zero disables it.", AdminSettingsType.STRING)); }
    private AdminSettingsEntry entry(String key, String label, String description, AdminSettingsType type) { return new AdminSettingsEntry(key, label, description, current.getOrDefault(key, defaults.getOrDefault(key, "")), defaults.getOrDefault(key, ""), type, false, value -> SettingsFileEditor.writeValue(settingsFile, key, value)); }
    private int integer(String key, int fallback) { return Integer.parseInt(current.getOrDefault(key, defaults.getOrDefault(key, Integer.toString(fallback)))); }
    private long positiveLong(String key, long fallback) { return Math.max(0L, Long.parseLong(current.getOrDefault(key, defaults.getOrDefault(key, Long.toString(fallback))))); }
    private double decimal(String key, double fallback) { return Math.max(0d, Double.parseDouble(current.getOrDefault(key, defaults.getOrDefault(key, Double.toString(fallback))))); }
    private static String safeWorldName() { try { String name = World.getName(); return name == null || name.isBlank() ? "default" : name.replaceAll("[^A-Za-z0-9._-]", "_"); } catch (LinkageError ex) { return "default"; } }
}
