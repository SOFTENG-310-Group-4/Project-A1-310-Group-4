
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
  const [loyaltyPrograms, setLoyaltyPrograms] = useState([])
  const [allergenTags, setAllergenTags] = useState([])
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
    setCartLines((currentLines) => {
      const syncBasketLines = syncBasketWithFilteredProducts(currentLines, data)

      if (
        !hasPrefilledBasket.current &&
        data.length > 0 &&
        syncBasketLines.length === 1 &&
        syncBasketLines[0].productId === ''
      ) {
        hasPrefilledBasket.current = true
        return [{ productId: String(data[0].productId), quantity: 1 }]
      }

      hasPrefilledBasket.current = true
      return syncBasketLines
    })
  }

  const { products, loading: catalogueLoading } = useProductCatalogue(
    dietaryTags,
    allergenTags,
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

  function clearAllTags() {
    setDietaryTags([])
    setAllergenTags([])
  }

  function toggleAllergenTag(tag) {
    setAllergenTags((currentTags) =>
      currentTags.includes(tag)
        ? currentTags.filter((currentTag) => currentTag !== tag)
        : [...currentTags, tag],
    )
  }

  /**
   * Compares the current basket, applying member prices for the given loyalty programmes.
   * loyaltyPrograms is only sent when a card is selected, so the request is unchanged otherwise.
   */
  async function runComparison(programs) {
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
      if (programs.length > 0) {
        payload.loyaltyPrograms = programs
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

  function handleSubmit(event) {
    event.preventDefault()
    return runComparison(loyaltyPrograms)
  }

  // Flipping a card while results are showing re-runs the comparison, so the
  // totals and cheapest store always match the toggles.
  function toggleLoyaltyProgram(program) {
    const nextPrograms = loyaltyPrograms.includes(program)
      ? loyaltyPrograms.filter((current) => current !== program)
      : [...loyaltyPrograms, program]

    setLoyaltyPrograms(nextPrograms)
    if (comparison && !loading) {
      void runComparison(nextPrograms)
    }
  }

  return (
    <main className="shell">
      <Hero basketQuantity={basketQuantity} selectedProducts={selectedProducts.length} />

      <section className="panel grid filtered-grid">
        <div className="catalog-column">
          <DietaryFilterBar
            selectedDietary={dietaryTags}
            onToggleDietary={toggleDietaryTag}
            selectedAllergens={allergenTags}
            onToggleAllergen={toggleAllergenTag}
            onClear={clearAllTags}
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
          loyaltyPrograms={loyaltyPrograms}
          onToggleLoyaltyProgram={toggleLoyaltyProgram}
        />
      </section>
      
      {comparison ? <ResultsPanel comparison={comparison} /> : null}
    </main>
  )
}

export default App