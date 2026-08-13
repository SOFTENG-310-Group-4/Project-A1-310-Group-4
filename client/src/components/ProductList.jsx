import ProductCard from './ProductCard';

const mockProducts = [
  {
    id: 1,
    name: 'Milk',
    description: '2L Standard Milk',
    prices: [
      {
        store: {
          id: 1,
          name: "PAK'nSAVE",
        },
        price: 4.50,
      },
      {
        store: {
          id: 2,
          name: 'Countdown',
        },
        price: 5.20,
      },
    ],
  },
  {
    id: 2,
    name: 'Bread',
    description: 'White Sandwich Bread',
    prices: [
      {
        store: {
          id: 1,
          name: "PAK'nSAVE",
        },
        price: 3.50,
      },
      {
        store: {
          id: 3,
          name: 'New World',
        },
        price: 4.20,
      },
    ],
  },
];

function ProductList() {
  return (
    <main className="product-section">
      <div className="section-heading">

        <span className="product-count">
          {mockProducts.length} products
        </span>
      </div>

      <div className="product-list">
        {mockProducts.map((product) => (
          <ProductCard
            key={product.id}
            product={product}
          />
        ))}
      </div>
    </main>
  );
}

export default ProductList;
