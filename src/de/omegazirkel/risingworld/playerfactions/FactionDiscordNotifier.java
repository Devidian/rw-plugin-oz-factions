package de.omegazirkel.risingworld.playerfactions;

import de.omegazirkel.risingworld.tools.I18n;

/** Optional event forwarding; a missing Discord Connect installation is a no-op. */
final class FactionDiscordNotifier {
    private final DiscordBridge discord;
    private final PluginSettings settings;
    private final I18n i18n;
    FactionDiscordNotifier(DiscordBridge discord, PluginSettings settings, I18n i18n) { this.discord=discord;this.settings=settings;this.i18n=i18n; }
    void founded(String playerName,String factionName){send(settings.discordLifecycleChannelId,"tc.factions.discord.founded",playerName,factionName);}
    void dissolved(String playerName,String factionName){send(settings.discordLifecycleChannelId,"tc.factions.discord.dissolved",playerName,factionName);}
    void member(String key,String playerName,String factionName){send(settings.discordMembershipChannelId,key,playerName,factionName);}
    void extra(String playerName,String factionName,String extra){send(settings.discordExtrasChannelId,"tc.factions.discord.extra",playerName,factionName,extra);}
    private void send(long channel,String key,String playerName,String factionName){if(channel<=0L||!discord.isAvailable())return;String text=i18n.get(key,discord.getBotLanguage()).replace("PH_PLAYER",playerName).replace("PH_FACTION",factionName);discord.sendTextMessage(text,channel);}
    private void send(long channel,String key,String playerName,String factionName,String extra){if(channel<=0L||!discord.isAvailable())return;String text=i18n.get(key,discord.getBotLanguage()).replace("PH_PLAYER",playerName).replace("PH_FACTION",factionName).replace("PH_EXTRA",extra);discord.sendTextMessage(text,channel);}
}
