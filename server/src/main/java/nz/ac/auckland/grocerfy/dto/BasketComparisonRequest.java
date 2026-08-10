package nz.ac.auckland.grocerfy.dto;

import java.util.List;

/**
 * Request payload for basket comparison.
 * Contains a list of product IDs and quantities from the user's cart.
 */
public record BasketComparisonRequest(List<BasketItemRequest> items) {
}
