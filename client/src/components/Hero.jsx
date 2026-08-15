/**
 * Main page header component: title, description, and cart summary.
 */
function Hero({ basketQuantity, selectedProducts }) {
    return (
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
    )
}

export default Hero