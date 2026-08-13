import { useEffect, useMemo, useState } from 'react'

const catalog = [
  { id: 'apple', name: 'Apple', price: 1.5 },
  { id: 'banana', name: 'Banana', price: 1.2 },
  { id: 'bread', name: 'Bread', price: 3.5 },
  { id: 'milk', name: 'Milk', price: 2.8 },
  { id: 'eggs', name: 'Eggs', price: 4.2 },
  { id: 'rice', name: 'Rice', price: 5.4 },
]

const STORAGE_KEY = 'grocerfy-cart'

function App() {
  const [cart, setCart] = useState(() => {
    if (typeof window === 'undefined') return []

    try {
      const savedCart = window.localStorage.getItem(STORAGE_KEY)
      return savedCart ? JSON.parse(savedCart) : []
    } catch {
      return []
    }
  })

  useEffect(() => {
    window.localStorage.setItem(STORAGE_KEY, JSON.stringify(cart))
  }, [cart])

  const addToCart = (product) => {
    setCart((currentCart) => {
      const existingItem = currentCart.find((item) => item.id === product.id)

      if (existingItem) {
        return currentCart.map((item) =>
          item.id === product.id
            ? { ...item, quantity: item.quantity + 1 }
            : item,
        )
      }

      return [...currentCart, { ...product, quantity: 1 }]
    })
  }

  const updateQuantity = (productId, delta) => {
    setCart((currentCart) =>
      currentCart
        .map((item) =>
          item.id === productId
            ? { ...item, quantity: item.quantity + delta }
            : item,
        )
        .filter((item) => item.quantity > 0),
    )
  }

  const subtotal = useMemo(
    () => cart.reduce((total, item) => total + item.price * item.quantity, 0),
    [cart],
  )

  return (
    <main className="shell">
      <section className="hero">
        <div>
          <p className="eyebrow">Grocerfy</p>
          <h1>Build your cart</h1>
          <p className="lede">
            Pick groceries, adjust quantities, and keep the cart saved in your browser.
          </p>
        </div>

        <div className="summary-card">
          <span className="summary-label">Cart total</span>
          <strong>${subtotal.toFixed(2)}</strong>
          <span className="summary-copy">
            {cart.reduce((count, item) => count + item.quantity, 0)} item(s) selected
          </span>
        </div>
      </section>

      <section className="panel cart-layout">
        <div className="catalog-panel">
          <div className="section-heading">
            <h2>Groceries</h2>
            <span>{catalog.length} products</span>
          </div>

          <ul className="catalog-list">
            {catalog.map((product) => {
              const selectedItem = cart.find((item) => item.id === product.id)
              const quantity = selectedItem ? selectedItem.quantity : 0

              return (
                <li key={product.id} className="catalog-item">
                  <div className="product-meta">
                    <strong>{product.name}</strong>
                    <span>${product.price.toFixed(2)}</span>
                  </div>

                  <div className="product-actions">
                    <button
                      type="button"
                      className="ghost-button subtle"
                      onClick={() => updateQuantity(product.id, -1)}
                      disabled={quantity === 0}
                    >
                      -
                    </button>
                    <span className="quantity-value">{quantity}</span>
                    <button
                      type="button"
                      className="primary-button small"
                      onClick={() => addToCart(product)}
                    >
                      Add
                    </button>
                  </div>
                </li>
              )
            })}
          </ul>
        </div>

        <aside className="cart-panel">
          <div className="section-heading">
            <h2>Shopping cart</h2>
            <span>{cart.length} type(s)</span>
          </div>

          {cart.length === 0 ? (
            <p className="empty-state">Your cart is empty.</p>
          ) : (
            <ul className="cart-list">
              {cart.map((item) => (
                <li key={item.id} className="cart-item">
                  <div className="cart-item-main">
                    <strong>{item.name}</strong>
                    <span>${(item.price * item.quantity).toFixed(2)}</span>
                  </div>

                  <div className="quantity-control">
                    <button
                      type="button"
                      className="ghost-button subtle"
                      onClick={() => updateQuantity(item.id, -1)}
                    >
                      -
                    </button>
                    <span>{item.quantity}</span>
                    <button
                      type="button"
                      className="ghost-button subtle"
                      onClick={() => updateQuantity(item.id, 1)}
                    >
                      +
                    </button>
                  </div>
                </li>
              ))}
            </ul>
          )}

          <div className="cart-footer">
            <span>Subtotal</span>
            <strong>${subtotal.toFixed(2)}</strong>
          </div>
        </aside>
      </section>
    </main>
  )
}

export default App
