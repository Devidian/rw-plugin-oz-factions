package de.omegazirkel.risingworld.playerfactions;

/** Extras are capacity grants; consumer plugins retain their own usage records. */
public enum FactionExtra {
    MEMBER_SLOT("memberSlot", "Member slot"), CLAIM_LICENSE("claimLicense", "Land Claim license"),
    TRADER_LICENSE("traderLicense", "Trader license"), CRIER_LICENSE("crierLicense", "Marktschreier license"),
    SERVICE_NPC_LICENSE("serviceNpcLicense", "Service NPC license");
    private final String key, label;
    FactionExtra(String key, String label) { this.key = key; this.label = label; }
    public String key() { return key; }
    /** Stable, lowercase Shop ID and AssetManager key. */
    public String shopOfferId() {
        return "faction-extra-" + key.replaceAll("([a-z])([A-Z])", "$1-$2").toLowerCase(java.util.Locale.ROOT);
    }
    public String label() { return label; }
}
