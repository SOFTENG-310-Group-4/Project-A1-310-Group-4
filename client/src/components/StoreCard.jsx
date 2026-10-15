import { loyaltyProgramLabel } from '../constants/loyaltyPrograms'
import StoreDistance from './StoreDistance'

/**
 * Component that returns the comparison result for each store.
 * Shows basket total, availability status, and store details.
 * With loyalty cards (issue #78), member-priced lines show the regular price
 * struck through, and stores whose member prices aren't in use show what a card would save.
 */
function StoreCard({ store , userLocation = null }) {
    const programLabel = store.loyaltyProgram ? loyaltyProgramLabel(store.loyaltyProgram) : null
    const savingApplied = Number(store.memberSavingApplied ?? 0)
    const savingAvailable = Number(store.memberSavingAvailable ?? 0)

    return (
        <article className={`store-card ${store.available ? 'available' : 'unavailable'}`}>
            <div className="store-header">
                <div>
                    <h3>{store.storeName}</h3>
                    <p>{store.region}</p>
                </div>
                <div className="store-meta">
                  <span>{store.available ? 'Basket available' : 'Missing items'}</span>
                  <StoreDistance userLocation={userLocation} store={store} />
                </div>
            </div>

            <p className="store-address">{store.address}</p>

            <div className="store-total">
                <span>Basket total</span>
                <strong>${store.availableSubtotal.toFixed(2)}</strong>
            </div>

            {programLabel && savingApplied > 0 ? (
                <p className="member-note">
                    Includes ${savingApplied.toFixed(2)} {programLabel} savings
                </p>
            ) : null}

            {programLabel && savingAvailable > 0 ? (
                <p className="member-note member-hint">
                    Save ${savingAvailable.toFixed(2)} with {programLabel}
                </p>
            ) : null}

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
                            {lineItem.memberPriceApplied ? (
                                <>
                                    {' '}
                                    <s className="regular-price" aria-label="regular price">
                                        ${(lineItem.regularUnitPrice * lineItem.quantity).toFixed(2)}
                                    </s>
                                    {' '}
                                    <span className="member-badge">{programLabel ?? 'Member'} price</span>
                                </>
                            ) : null}
                        </li>
                    ))}
                </ul>
            </div>
        </article>
    )
}

export default StoreCard
