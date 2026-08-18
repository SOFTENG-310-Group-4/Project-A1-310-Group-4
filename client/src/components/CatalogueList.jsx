/**
 * List for displaying the filtered product catalogue.
 * In addition to loading/empty states.
 */
function CatalogueList({ products, catalogueLoading }) {
    return (
        <aside className="catalog-card">
            <div className="section-heading">
              <h2>Catalogue</h2>
              <span>{products.length} products</span>
            </div>

            {catalogueLoading ? <p className="filter-summary">Loading catalogue...</p> : null}

            {!catalogueLoading && products.length === 0 ? (
              <p className="filter-summary">No products match every selected dietary tag.</p>
            ) : null}

            <ul className="catalog-list">
              {products.map((product) => (
                <li key={product.productId}>
                  <strong>{product.displayName}</strong>
                  <span>{product.productName}</span>
                </li>
              ))}
            </ul>
        </aside>
    )
}

export default CatalogueList