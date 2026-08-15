import { useEffect, useMemo, useRef, useState } from 'react'
import DietaryFilterBar from './components/DietaryFilterBar'
import { compareBasket } from './api/basketApi'
import { syncBasketWithFilteredProducts } from './utils/basketSync'
import { useProductCatalogue } from './hooks/useProductCatalogue'
import { useCartLines } from './hooks/useCartLines'

import Hero from './components/Hero'
import CatalogueList from './components/CatalogueList'

function App() {
  const [dietaryTags, setDietaryTags] = useState([])
  const [comparison, setComparison] = useState(null)
  const [loading, setLoading] = useState(false)
  const [error, setError] = useState('')

  const { cartLines, setCartLines, updateLine, addLine, removeLine } = useCartLines()

  // The catalogue is pre-filled with the first product on the very first load
  // only. Without this guard the basket would be reset every time a filter
  // changed, because the effect below re-runs on each tag change.
  const hasPrefilledBasket = useRef(false)

  // Filtering happens server-side: /api/products carries no dietary flags, so
  // the search endpoint from #14 is the only source that can apply the tags.
  function handleProductsLoaded(data) {
    setError('')
    setCartLines((currentLines) => syncBasketWithFilteredProducts(currentLines, data))

    if (!hasPrefilledBasket.current && data.length > 0) {
      hasPrefilledBasket.current = true
      setCartLines([{ productId: String(data[0].productId), quantity: 1 }])
    }
  }

  const { products, loading: catalogueLoading } = useProductCatalogue(
    dietaryTags, 
    handleProductsLoaded, 
    setError,
  )

  const selectedProducts = useMemo(() => {
    const ids = new Set(cartLines.map((line) => Number(line.productId)).filter(Boolean))
    return products.filter((product) => ids.has(product.productId))
  }, [cartLines, products])

  const basketQuantity = useMemo(
    () => cartLines.reduce((total, line) => total + Number(line.quantity || 0), 0),
    [cartLines],
  )

  function toggleDietaryTag(tag) {
    setDietaryTags((currentTags) =>
      currentTags.includes(tag)
        ? currentTags.filter((currentTag) => currentTag !== tag)
        : [...currentTags, tag],
    )
  }

  function clearDietaryTags() {
    setDietaryTags([])
  }

  async function handleSubmit(event) {
    event.preventDefault()
    setError('')
    setLoading(true)
    setComparison(null)

    try {
      const payload = {
        items: cartLines
          .filter((line) => line.productId !== '')
          .map((line) => ({
            productId: Number(line.productId),
            quantity: Number(line.quantity),
          })),
      }

      const data = await compareBasket(payload)

      setComparison(data)
    } catch (submitError) {
      setError(submitError.message)
    } finally {
      setLoading(false)
    }
  }

  return (
    <main className="shell">
      <Hero basketQuantity={basketQuantity} selectedProducts={selectedProducts.length} />

      <section className="panel grid filtered-grid">
        <div className="catalog-column">
          <DietaryFilterBar
            selectedTags={dietaryTags}
            onToggleTag={toggleDietaryTag}
            onClear={clearDietaryTags}
            matchCount={products.length}
          />

          <CatalogueList products={products} catalogueLoading={catalogueLoading} />
          
        </div>

        <form className="basket-form" onSubmit={handleSubmit}>
          <div className="section-heading">
            <h2>Basket</h2>
            <button type="button" className="ghost-button" onClick={addLine}>Add item</button>
          </div>

          <div className="basket-lines">
            {cartLines.map((line, index) => (
              <div key={`${index}-${line.productId}`} className="basket-line">
                <label>
                  Product
                  <select
                    value={line.productId}
                    onChange={(event) => updateLine(index, 'productId', event.target.value)}
                    disabled={products.length === 0}
                  >
                    <option value="">Select a product</option>
                    {products.map((product) => {
                      const isSelectedElsewhere = cartLines.some(
                        (otherLine, otherIndex) =>
                          otherIndex !== index && String(otherLine.productId) === String(product.productId)
                      )

                      return (
                        <option
                          key={product.productId}
                          value={product.productId}
                          disabled={isSelectedElsewhere}
                        >
                          {product.displayName}
                          {isSelectedElsewhere ? ' (Already added)' : ''}
                        </option>
                      )
                    })}
                  </select>
                </label>

                <label>
                  Quantity
                  <input
                    type="number"
                    min="1"
                    value={line.quantity}
                    onChange={(event) => updateLine(index, 'quantity', event.target.value)}
                  />
                </label>

                <button type="button" className="ghost-button subtle" onClick={() => removeLine(index)}>
                  Remove
                </button>
              </div>
            ))}
          </div>

          <button type="submit" className="primary-button" disabled={loading || products.length === 0}>
            {loading ? 'Comparing stores...' : 'Compare basket'}
          </button>

          {error ? <p className="error-banner">{error}</p> : null}
        </form>
      </section>

      {comparison ? (
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
              <article className={`store-card ${store.available ? 'available' : 'unavailable'}`} key={store.storeId}>
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
                        <li key={`${store.storeId}-${missingItem.productId}`}>
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
                      <li key={`${store.storeId}-${lineItem.productId}`}>
                        {lineItem.productName} x {lineItem.quantity} - ${lineItem.lineTotal.toFixed(2)}
                      </li>
                    ))}
                  </ul>
                </div>
              </article>
            ))}
          </div>
        </section>
      ) : null}
    </main>
  )
}

export default App