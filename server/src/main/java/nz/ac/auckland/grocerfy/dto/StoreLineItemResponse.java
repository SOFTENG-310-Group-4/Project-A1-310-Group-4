package nz.ac.auckland.grocerfy.dto;

import java.math.BigDecimal;

/**
 * Represents a single product line from a store that is available for the requested basket.
 * unitPrice is the price charged (the member price when one was applied); regularUnitPrice is
 * the shelf price, so the two differ exactly when memberPriceApplied is true.
 */
public record StoreLineItemResponse(
		Long productId,
		String productName,
		int quantity,
		BigDecimal unitPrice,
		BigDecimal lineTotal,
		BigDecimal regularUnitPrice,
		boolean memberPriceApplied) {
}
