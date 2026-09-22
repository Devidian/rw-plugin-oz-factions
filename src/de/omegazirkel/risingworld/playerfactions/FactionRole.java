package de.omegazirkel.risingworld.playerfactions;

public enum FactionRole {
    NEWCOMER, MEMBER, OFFICER, LEADER;
    public boolean canManageApplications() { return this == OFFICER || this == LEADER; }
    public boolean canManageMembers() { return this == OFFICER || this == LEADER; }
}
