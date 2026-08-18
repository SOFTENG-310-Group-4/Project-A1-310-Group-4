import StoreCard from './StoreCard'

/**
 * Renders each of the store cards for the comparison result.
 * Also shows the cheapest option available.
 */
function ResultsPanel({ comparison }) {
    return (
        <section className="panel results-panel">
          <div className="section-heading">
            <h2>Results</h2>
            <span>{comparison.stores.length} stores compared</span>
          </div>

        
          <div className="cheapest-card">
            <p>Cheapest available option</p>
            {comparison.cheapestAvailableStore ? (
              <div>
                <strong>{comparison.cheapestAvailableStore.storeName}</strong>
                <span>{comparison.cheapestAvailableStore.region}</span>
                <span className="price">${comparison.cheapestAvailableStore.availableSubtotal.toFixed(2)}</span>
              </div>
            ) : (
              <div>
                <strong>No store can fulfil the full basket</strong>
                <span>The response below still shows which items are missing.</span>
              </div>
            )}
          </div>  

          <div className="store-grid">
            {comparison.stores.map((store) => (
                <StoreCard key={store.storeId} store={store} />
            ))}
          </div>
        </section>
    )
}

export default ResultsPanel