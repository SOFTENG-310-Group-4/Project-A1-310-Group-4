package nz.ac.auckland.grocerfy.model;

/**
 * Stock level of a product at a single store location.
 * Derived from the store's stock quantity so the frontend can show a simple badge
 * instead of raw numbers.
 */
public enum StockStatus {
	IN_STOCK,
	LOW_STOCK,
	OUT_OF_STOCK;

	/** Quantities at or below this value (but above zero) are reported as low stock. */
	public static final int LOW_STOCK_THRESHOLD = 5;

	public static StockStatus fromQuantity(int stockQuantity) {
		if (stockQuantity <= 0) {
			return OUT_OF_STOCK;
		}
		if (stockQuantity <= LOW_STOCK_THRESHOLD) {
			return LOW_STOCK;
		}
		return IN_STOCK;
	}
}
