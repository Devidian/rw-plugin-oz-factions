package de.omegazirkel.risingworld.playerfactions.runtime;

import de.omegazirkel.risingworld.playerfactions.FactionService;
import net.risingworld.api.events.player.PlayerHitPlayerEvent;

/** Cancels direct player damage between members of the same faction. */
public final class FactionCombatHandler {
    private final FactionService factions;
    FactionCombatHandler(FactionService factions) { this.factions = factions; }
    public void onPlayerHit(PlayerHitPlayerEvent event) {
        if (event.getDamage() > 0 && factions.sameFaction(event.getPlayer().getDbID(), event.getTargetPlayer().getDbID())) event.setCancelled(true);
    }
}
