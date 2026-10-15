package nz.ac.auckland.grocerfy.dto;

import java.math.BigDecimal;
import java.util.List;

import nz.ac.auckland.grocerfy.model.LoyaltyProgram;

/**
 * Store-level comparison details for a single store.
 * Includes whether the full basket is available, the computed total,
 * available line items, and any missing items.
 * <p>
 * Member prices: loyaltyProgram is the programme offering member prices on this basket at this
 * store (null if none). memberSavingApplied is how much the applied member prices saved;
 * memberSavingAvailable is how much more would be saved by joining (or enabling) that programme.
 */
public record StoreComparisonResponse(
		Long storeId,
		String storeName,
		String region,
		String address,
		boolean available,
		BigDecimal availableSubtotal,
		List<StoreLineItemResponse> lineItems,
		List<MissingItemResponse> missingItems,
		LoyaltyProgram loyaltyProgram,
		BigDecimal memberSavingApplied,
		BigDecimal memberSavingAvailable) {
}
