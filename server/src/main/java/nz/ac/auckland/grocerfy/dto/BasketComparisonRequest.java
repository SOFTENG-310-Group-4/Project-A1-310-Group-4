package nz.ac.auckland.grocerfy.dto;

import java.util.List;
import java.util.Set;

import nz.ac.auckland.grocerfy.model.LoyaltyProgram;

/**
 * Request payload for basket comparison.
 * Contains a list of product IDs and quantities from the user's cart, and optionally the loyalty
 * programmes the user belongs to (e.g. ["EVERYDAY_REWARDS"]) so member prices can be applied.
 * Omitting loyaltyPrograms compares regular shelf prices only.
 */
public record BasketComparisonRequest(List<BasketItemRequest> items, Set<LoyaltyProgram> loyaltyPrograms) {
}
