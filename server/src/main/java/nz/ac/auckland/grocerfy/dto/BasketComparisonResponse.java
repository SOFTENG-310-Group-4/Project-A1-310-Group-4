package nz.ac.auckland.grocerfy.dto;

import java.util.List;

public record BasketComparisonResponse(
		List<ProductSummaryResponse> requestedItems,
		List<StoreComparisonResponse> stores,
		StoreComparisonResponse cheapestAvailableStore,
		StoreComparisonResponse cheapestStore) {
}