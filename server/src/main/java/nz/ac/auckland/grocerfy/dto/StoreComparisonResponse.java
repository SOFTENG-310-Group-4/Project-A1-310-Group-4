package nz.ac.auckland.grocerfy.dto;

import java.math.BigDecimal;
import java.util.List;

/**
 * Store-level comparison details for a single store.
 * Includes whether the full basket is available, the computed total,
 * available line items, and any missing items.
 */
public record StoreComparisonResponse(
		Long storeId,
		String storeName,
		String region,
		String address,
		boolean available,
		BigDecimal availableSubtotal,
		List<StoreLineItemResponse> lineItems,
		List<MissingItemResponse> missingItems) {
}