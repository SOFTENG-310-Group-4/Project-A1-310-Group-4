function ProductCard({ product }) {
  return (
    <article className="product-card">
      <div className="product-info">
        <h3>{product.name}</h3>
        <p>{product.description}</p>
      </div>

      <div className="price-list">
        {product.prices.map((storePrice) => (
          <div
            className="store-price"
            key={storePrice.store.id}
          >
            <span className="store-name">
              {storePrice.store.name}
            </span>

            <span className="price">
              ${storePrice.price.toFixed(2)}
            </span>
          </div>
        ))}
      </div>
    </article>
  );
}

export default ProductCard;