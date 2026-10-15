import { STOCK_STATUS_LABELS, describeMissingReason } from '../constants/stockStatus'

/**
 * Component that returns the comparison result for each store.
 * Shows basket total, availability status, store details, and the stock status
 * of each basket item at this location.
 */
function StoreCard({ store }) {
    return (
        <article className={`store-card ${store.available ? 'available' : 'unavailable'}`}>
            <div className="store-header">
                <div>
                    <h3>{store.storeName}</h3>
                    <p>{store.region}</p>
                </div>
                <span>{store.available ? 'Basket available' : 'Missing items'}</span>
            </div>

            <p className="store-address">{store.address}</p>

            <div className="store-total">
                <span>Basket total</span>
                <strong>${store.availableSubtotal.toFixed(2)}</strong>
            </div>

            {store.missingItems.length > 0 ? (
                <div className="list-block">
                    <h4>Missing items</h4>
                    <ul>
                        {store.missingItems.map((missingItem) => {
                            const reason = describeMissingReason(missingItem)
                            return (
                                <li key={missingItem.productId}>
                                    {missingItem.productName} x {missingItem.quantity}
                                    {reason ? (
                                        <em className="stock-badge stock-out">{reason}</em>
                                    ) : null}
                                </li>
                            )
                        })}
                    </ul>
                </div>
            ) : null}

            <div className="list-block">
                <h4>Available basket lines</h4>
                <ul>
                    {store.lineItems.map((lineItem) => (
                        <li key={lineItem.productId}>
                            {lineItem.productName} x {lineItem.quantity} - ${lineItem.lineTotal.toFixed(2)}
                            {/* Only flag low stock, a badge on every in-stock line would be noise. */}
                            {lineItem.stockStatus === 'LOW_STOCK' ? (
                                <em className="stock-badge stock-low">{STOCK_STATUS_LABELS.LOW_STOCK}</em>
                            ) : null}
                        </li>
                    ))}
                </ul>
            </div>
        </article>
    )
}

export default StoreCard
