package de.omegazirkel.risingworld.playerfactions;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Properties;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import net.risingworld.api.Plugin;
import net.risingworld.api.Server;
import net.risingworld.api.objects.Player;

/** Owns only faction-created group files; it never edits the server default group. */
final class PermissionGroupProvisioner {
    private final Plugin plugin;
    private final Path serverRoot;
    PermissionGroupProvisioner(Plugin plugin) {
        this.plugin = plugin;
        Path pluginPath = Path.of(plugin.getPath() == null ? "." : plugin.getPath()).toAbsolutePath().normalize();
        serverRoot = pluginPath.getParent() != null && pluginPath.getParent().getParent() != null ? pluginPath.getParent().getParent() : pluginPath;
    }
    void create(String groupName, String factionName, String factionColor) throws IOException {
        Path target = groupsDirectory().resolve(groupName + ".json");
        if (Files.exists(target)) throw new IOException("Permission group already exists: " + groupName);
        Files.createDirectories(target.getParent());
        Files.copy(defaultTemplate(), target);
        updateMetadata(target, factionName, factionColor);
    }
    void reloadThrough(Player player) {
        Object previous = player.getPermissionValue("command_reloadpermissions", false);
        boolean allowed = Boolean.TRUE.equals(previous);
        if (!allowed) player.setPermissionValue("command_reloadpermissions", true);
        if (!Boolean.TRUE.equals(player.getPermissionValue("command_reloadpermissions", false))) {
            if (!allowed) player.setPermissionValue("command_reloadpermissions", previous);
            throw new IllegalStateException("Could not grant the temporary reloadpermissions permission.");
        }
        try { player.executeCommand("reloadpermissions"); }
        catch (RuntimeException ex) {
            if (!allowed) player.setPermissionValue("command_reloadpermissions", previous);
            throw ex;
        }
        if (!allowed) plugin.executeDelayed(1f, () -> player.setPermissionValue("command_reloadpermissions", previous));
    }
    void updateMetadata(String groupName, String factionName, String factionColor) throws IOException {
        updateMetadata(groupsDirectory().resolve(groupName + ".json"), factionName, factionColor);
        reload();
    }
    void remove(String groupName) throws IOException { Files.deleteIfExists(groupsDirectory().resolve(groupName + ".json")); reload(); }
    void assign(Player player, String groupName) { player.setPermissionGroup(groupName); }
    void assignDefault(Player player) { player.setPermissionGroup(defaultGroupName()); }
    String defaultGroupName() {
        String configured = property("Permissions_DefaultNewPlayerPermissionGroup");
        return configured == null ? "" : configured.trim();
    }
    private Path defaultTemplate() { String group = defaultGroupName(); return group.isBlank() ? serverRoot.resolve("Permissions/default.json") : groupsDirectory().resolve(group + ".json"); }
    private Path groupsDirectory() { return serverRoot.resolve("Permissions/Groups"); }
    private String property(String key) { Properties p=new Properties(); try(var input=Files.newInputStream(serverRoot.resolve("server.properties"))){p.load(input);return p.getProperty(key);}catch(IOException ex){return null;} }
    private void updateMetadata(Path file, String factionName, String factionColor) throws IOException {
        String content = Files.readString(file, StandardCharsets.UTF_8);
        writeAtomically(file, withFactionMetadata(content, factionName, factionColor));
    }
    static String withFactionMetadata(String content, String factionName, String factionColor) throws IOException {
        int infoKey = content.indexOf("\"info\"");
        int start = infoKey < 0 ? -1 : content.indexOf('{', infoKey);
        int end = start < 0 ? -1 : matchingBrace(content, start);
        if (end < 0) throw new IOException("Permission group has no valid info object");
        String info = content.substring(start + 1, end);
        info = setStringField(info, "group", factionName);
        info = setStringField(info, "groupcolor", factionColor);
        info = setStringField(info, "chatnameprefix", "");
        info = setStringField(info, "chatnamesuffix", "");
        info = setStringField(info, "nametagprefix", "");
        info = setStringField(info, "nametagsuffix", "");
        return content.substring(0, start + 1) + info + content.substring(end);
    }
    private static int matchingBrace(String content, int start) {
        boolean quoted = false; boolean escaped = false; int depth = 0;
        for (int index = start; index < content.length(); index++) {
            char value = content.charAt(index);
            if (quoted) { if (escaped) escaped = false; else if (value == '\\') escaped = true; else if (value == '"') quoted = false; continue; }
            if (value == '"') quoted = true;
            else if (value == '{') depth++;
            else if (value == '}' && --depth == 0) return index;
        }
        return -1;
    }
    private static String setStringField(String object, String key, String value) {
        Pattern field = Pattern.compile("(\\\"" + Pattern.quote(key) + "\\\"\\s*:\\s*)\\\"(?:\\\\.|[^\\\"])*\\\"");
        Matcher matcher = field.matcher(object);
        String replacement = "$1\"" + escape(value) + "\"";
        if (matcher.find()) return matcher.replaceFirst(replacement);
        String separator = object.isBlank() || object.stripTrailing().endsWith(",") ? "" : ",";
        return object + separator + "\n        \"" + key + "\": \"" + escape(value) + "\"";
    }
    private static String escape(String value) { return value.replace("\\", "\\\\").replace("\"", "\\\""); }
    private static void writeAtomically(Path file, String content) throws IOException {
        Path temporary = Files.createTempFile(file.getParent(), file.getFileName().toString(), ".tmp");
        Files.writeString(temporary, content, StandardCharsets.UTF_8);
        try { Files.move(temporary, file, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING); }
        catch (AtomicMoveNotSupportedException ignored) { Files.move(temporary, file, StandardCopyOption.REPLACE_EXISTING); }
    }
    private void reload() { Server.sendInputCommand("reloadpermissions"); }
}
