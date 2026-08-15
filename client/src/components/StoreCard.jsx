/**
 * Component that returns the comparison result for each store.
 * Shows basket total, availability status, and store details.
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
                        {store.missingItems.map((missingItem) => (
                            <li key={missingItem.productId}>
                                {missingItem.productName} x {missingItem.quantity}
                            </li>
                        ))}
                    </ul>
                </div>
            ) : null}

            <div className="list-block">
                <h4>Available basket lines</h4>
                <ul>
                    {store.lineItems.map((lineItem) => (
                        <li key={lineItem.productId}>
                            {lineItem.productName} x {lineItem.quantity} - ${lineItem.lineTotal.toFixed(2)}
                        </li>
                    ))}
                </ul>
            </div>
        </article>
    )
}

export default StoreCard