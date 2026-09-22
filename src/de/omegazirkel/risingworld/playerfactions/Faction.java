package de.omegazirkel.risingworld.playerfactions;

public record Faction(int id, String name, String groupName, String color, long foundedAt, int founderDbId,
        int leaderDbId, String accountId, int extraMemberSlots, int extraClaimLicenses, int extraTraderLicenses,
        int extraCrierLicenses, int extraServiceNpcLicenses) { }
