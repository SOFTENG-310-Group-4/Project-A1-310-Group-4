package nz.ac.auckland.grocerfy.dto;

import java.math.BigDecimal;

/**
 * Represents a single product line from a store that is available for the requested basket.
 */
public record StoreLineItemResponse(
		Long productId,
		String productName,
		int quantity,
		BigDecimal unitPrice,
		BigDecimal lineTotal) {
}