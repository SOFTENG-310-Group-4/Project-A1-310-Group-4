package nz.ac.auckland.grocerfy.dto;

import java.math.BigDecimal;

public record StoreLineItemResponse(
		Long productId,
		String productName,
		int quantity,
		BigDecimal unitPrice,
		BigDecimal lineTotal) {
}