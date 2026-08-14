package nz.ac.auckland.grocerfy.dto;

import java.util.List;

/**
 * Response returned by the backend after comparing a basket across stores.
 * Includes the requested items, store-level results, and the cheapest available store.
 */
public record BasketComparisonResponse(
		List<ProductSummaryResponse> requestedItems,
		List<StoreComparisonResponse> stores,
		StoreComparisonResponse cheapestAvailableStore,
		StoreComparisonResponse cheapestStore) {
}
