/**
 * Whenever the user's dietary filter updates, we must ensure that any products
 * in the basket that don't meet the filter criteria is removed from the basket.
 */
export function syncBasketWithFilteredProducts(currentLines, visibleProducts) {
    const visibleIds = new Set(visibleProducts.map((product) => product.productId))
    return currentLines.filter(
        (line) => line.productId === '' || visibleIds.has(Number(line.productId)),
    )
}