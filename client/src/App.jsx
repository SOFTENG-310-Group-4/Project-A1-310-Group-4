import { useMemo, useRef, useState } from 'react'
import DietaryFilterBar from './components/DietaryFilterBar'
import { compareBasket } from './api/basketApi'
import { syncBasketWithFilteredProducts } from './utils/basketSync'
import { useProductCatalogue } from './hooks/useProductCatalogue'
import { useCartLines } from './hooks/useCartLines'

import Hero from './components/Hero'
import CatalogueList from './components/CatalogueList'
import BasketForm from './components/BasketForm'
import ResultsPanel from './components/ResultsPanel'

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
      const validLines = cartLines.filter((line) => line.productId !== '')

      const payload = {
        items: validLines.map((line) => ({
          productId: Number(line.productId),
          quantity: Number(line.quantity),
        })),
      }

      const data = await compareBasket(payload)

      setCartLines(validLines)
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

        <BasketForm
          cartLines={cartLines}
          products={products}
          loading={loading}
          error={error}
          onAddLine={addLine}
          onRemoveLine={removeLine}
          onUpdateLine={updateLine}
          onSubmit={handleSubmit}
          setError={setError}
        />
      </section>
      
      {comparison ? <ResultsPanel comparison={comparison} /> : null}
    </main>
  )
}

export default App