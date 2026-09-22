package de.omegazirkel.risingworld.playerfactions;

public record FactionMember(int factionId, int playerDbId, long joinedAt, long contributed, FactionRole role) { }
