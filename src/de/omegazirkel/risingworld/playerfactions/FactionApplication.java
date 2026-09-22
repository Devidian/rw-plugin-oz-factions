package de.omegazirkel.risingworld.playerfactions;
public record FactionApplication(int id, int factionId, int playerDbId, FactionApplicationStatus status, long createdAt, long updatedAt) { }
