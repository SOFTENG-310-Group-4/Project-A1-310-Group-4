import { useEffect, useMemo, useState } from 'react'

const emptyCartLine = { productId: '', quantity: 1 }

function App() {
  const [products, setProducts] = useState([])
  const [cartLines, setCartLines] = useState([{ ...emptyCartLine }])
  const [comparison, setComparison] = useState(null)
  const [loading, setLoading] = useState(false)
  const [error, setError] = useState('')

  useEffect(() => {
    let active = true

    async function loadProducts() {
      try {
        const response = await fetch('/api/products')
        if (!response.ok) {
          throw new Error('Unable to load products.')
        }

        const data = await response.json()
        if (active) {
          setProducts(data)
          setCartLines(data.length > 0 ? [{ productId: String(data[0].productId), quantity: 1 }] : [{ ...emptyCartLine }])
        }
      } catch (loadError) {
        if (active) {
          setError(loadError.message)
        }
      }
    }

    loadProducts()

    return () => {
      active = false
    }
  }, [])

  const selectedProducts = useMemo(() => {
    const ids = new Set(cartLines.map((line) => Number(line.productId)).filter(Boolean))
    return products.filter((product) => ids.has(product.productId))
  }, [cartLines, products])

  const basketQuantity = useMemo(
    () => cartLines.reduce((total, line) => total + Number(line.quantity || 0), 0),
    [cartLines],
  )

  function updateLine(index, field, value) {
    setCartLines((currentLines) =>
      currentLines.map((line, lineIndex) =>
        lineIndex === index
          ? {
              ...line,
              [field]: value,
            }
          : line,
      ),
    )
  }

  function addLine() {
    setCartLines((currentLines) => [...currentLines, { ...emptyCartLine }])
  }

  function removeLine(index) {
    setCartLines((currentLines) => currentLines.length === 1 ? currentLines : currentLines.filter((_, lineIndex) => lineIndex !== index))
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

      const response = await fetch('/api/basket/compare', {
        method: 'POST',
        headers: {
          'Content-Type': 'application/json',
        },
        body: JSON.stringify(payload),
      })

      const data = await response.json()
      if (!response.ok) {
        throw new Error(data.error || 'Unable to compare basket.')
      }

      setComparison(data)
    } catch (submitError) {
      setError(submitError.message)
    } finally {
      setLoading(false)
    }
  }

  return (
    <main className="shell">
      <section className="hero">
        <div>
          <p className="eyebrow">Grocerfy</p>
          <h1>Find the cheapest supermarket for your basket.</h1>
          <p className="lede">
            Add items from the seeded catalogue, send the cart to the backend, and compare each store’s availability and total.
          </p>
        </div>

        <div className="summary-card">
          <span className="summary-label">Cart items</span>
          <strong>{basketQuantity}</strong>
          <span className="summary-copy">
            {selectedProducts.length === 1 ? '1 product selected' : `${selectedProducts.length} products selected`}
          </span>
        </div>
      </section>

      <section className="panel grid">
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
                  <select value={line.productId} onChange={(event) => updateLine(index, 'productId', event.target.value)}>
                    <option value="">Select a product</option>
                    {products.map((product) => (
                      <option key={product.productId} value={product.productId}>
                        {product.displayName}
                      </option>
                    ))}
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

        <aside className="catalog-card">
          <div className="section-heading">
            <h2>Catalogue</h2>
            <span>{products.length} products</span>
          </div>
          <ul className="catalog-list">
            {products.map((product) => (
              <li key={product.productId}>
                <strong>{product.displayName}</strong>
                <span>{product.productName}</span>
              </li>
            ))}
          </ul>
        </aside>
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
