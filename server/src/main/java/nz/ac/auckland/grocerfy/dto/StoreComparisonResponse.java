package nz.ac.auckland.grocerfy.dto;

import java.math.BigDecimal;
import java.util.List;

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