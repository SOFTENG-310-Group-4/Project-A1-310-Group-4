import StoreCard from './StoreCard'
import { distanceKm, MAX_STORE_DISTANCE_KM } from '../utils/distance'

/**
 * Renders each of the store cards for the comparison result.
 * Also shows the cheapest option available.
 */
function ResultsPanel({ comparison, userLocation = null, maxDistanceKm = MAX_STORE_DISTANCE_KM }) {
    const stores = userLocation
      ? comparison.stores.filter((store) =>
          store.latitude != null &&
          store.longitude != null &&
          distanceKm(userLocation, { latitude: store.latitude, longitude: store.longitude }) <= maxDistanceKm,
        )
      : comparison.stores
    const cheapestAvailableStore = stores
      .filter((store) => store.available)
      .reduce((cheapest, store) =>
        !cheapest || Number(store.availableSubtotal) < Number(cheapest.availableSubtotal) ? store : cheapest,
      null)

    return (
        <section className="panel results-panel">
          <div className="section-heading">
            <h2>Results</h2>
            <span>
              {userLocation
                ? `${stores.length} ${stores.length === 1 ? 'store' : 'stores'} within ${maxDistanceKm} km`
                : `${stores.length} stores compared`}
            </span>
          </div>

        
          <div className="cheapest-card">
            <p>Cheapest available option</p>
            {userLocation && stores.length === 0 ? (
              <div>
                <strong>No stores within {maxDistanceKm} km</strong>
                <span>Try widening the distance filter.</span>
              </div>
            ) : cheapestAvailableStore ? (
              <div>
                <strong>{cheapestAvailableStore.storeName}</strong>
                <span>{cheapestAvailableStore.region}</span>
                <span className="price">${Number(cheapestAvailableStore.availableSubtotal).toFixed(2)}</span>
              </div>
            ) : (
              <div>
                <strong>No store can fulfil the full basket</strong>
                <span>The response below still shows which items are missing.</span>
              </div>
            )}
          </div>  

          <div className="store-grid">
            {stores.map((store) => (
                <StoreCard key={store.storeId} store={store} userLocation={userLocation} />
            ))}
          </div>
        </section>
    )
}

export default ResultsPanel