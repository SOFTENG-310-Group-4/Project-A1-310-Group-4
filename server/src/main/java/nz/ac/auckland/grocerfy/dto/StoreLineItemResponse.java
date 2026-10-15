package nz.ac.auckland.grocerfy.dto;

import java.math.BigDecimal;

import nz.ac.auckland.grocerfy.model.StockStatus;

/**
 * Represents a single product line from a store that is available for the requested basket.
 * unitPrice is the price charged (the member price when one was applied); regularUnitPrice is
 * the shelf price, so the two differ exactly when memberPriceApplied is true.
 * stockStatus lets the frontend warn when a store is running low, and is null when the
 * store's stock level is unknown.
 */
public record StoreLineItemResponse(
		Long productId,
		String productName,
		int quantity,
		BigDecimal unitPrice,
		BigDecimal lineTotal,
		BigDecimal regularUnitPrice,
		boolean memberPriceApplied,
		StockStatus stockStatus) {
}
