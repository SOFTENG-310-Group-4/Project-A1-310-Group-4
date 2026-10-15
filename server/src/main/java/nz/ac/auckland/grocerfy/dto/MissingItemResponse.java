package nz.ac.auckland.grocerfy.dto;

/**
 * Represents a requested product that is missing from a given store.
 * Includes the requested quantity so the frontend can show what is unavailable,
 * the reason it is missing, and how many units the store does have on hand.
 */
public record MissingItemResponse(
		Long productId,
		String productName,
		int quantity,
		MissingReason reason,
		int availableQuantity) {

	/**
	 * Why a store cannot supply a requested item.
	 * NOT_STOCKED: the store does not sell the product at all.
	 * OUT_OF_STOCK: the store sells it but has none left.
	 * INSUFFICIENT_STOCK: the store has some, but fewer than requested.
	 */
	public enum MissingReason {
		NOT_STOCKED,
		OUT_OF_STOCK,
		INSUFFICIENT_STOCK
	}
}
