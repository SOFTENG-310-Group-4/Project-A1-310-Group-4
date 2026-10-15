/**
 * Display labels for the per-store stock information returned by
 * /api/basket/compare. Keys match the backend StockStatus and MissingReason enums.
 */
export const STOCK_STATUS_LABELS = {
  IN_STOCK: 'In stock',
  LOW_STOCK: 'Low stock',
  OUT_OF_STOCK: 'Out of stock',
}

/**
 * Explains why a store cannot supply a basket item. INSUFFICIENT_STOCK includes
 * how many units the store does have so the user can decide whether to adjust.
 */
export function describeMissingReason(missingItem) {
  switch (missingItem.reason) {
    case 'NOT_STOCKED':
      return 'Not sold here'
    case 'OUT_OF_STOCK':
      return STOCK_STATUS_LABELS.OUT_OF_STOCK
    case 'INSUFFICIENT_STOCK':
      return `Only ${missingItem.availableQuantity} in stock`
    default:
      return null
  }
}
