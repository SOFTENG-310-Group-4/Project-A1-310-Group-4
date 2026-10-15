package nz.ac.auckland.grocerfy.dto;

import java.math.BigDecimal;

import nz.ac.auckland.grocerfy.model.StockStatus;

/**
 * Represents a single product line from a store that is available for the requested basket.
 * The stock status lets the frontend warn when a store is running low on an item.
 */
public record StoreLineItemResponse(
		Long productId,
		String productName,
		int quantity,
		BigDecimal unitPrice,
		BigDecimal lineTotal,
		StockStatus stockStatus) {
}
