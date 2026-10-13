package nz.ac.auckland.grocerfy.model;

/**
 * Supermarket loyalty programmes whose members get a lower shelf price on some products.
 * Only programmes with public, per-product member prices belong here: New World / PAK'nSAVE Club+
 * gives points and personalised deals instead, so it has no entry.
 */
public enum LoyaltyProgram {
    EVERYDAY_REWARDS("Everyday Rewards"); // Woolworths NZ

    private final String displayName;

    LoyaltyProgram(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
}
