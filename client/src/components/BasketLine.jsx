/**
 * Representation of a single line in the basket:
 * Has product dropdown, quantity input, and remove button.
 */
function BasketLine({ line, index, products, onChange, onRemove }) {
    return (
        <div className="basket-line">
            <label>
                Product{' '}
                <select
                    value={line.productId}
                    onChange={(event) => onChange(index, 'productId', event.target.value)}
                    disabled={products.length === 0}
                >
                    <option value="">Select a product</option>
                    {products.map((product) => (
                        <option key={product.productId} value={product.productId}>
                            {product.displayName}
                        </option>
                    ))}
                </select>
            </label>

            <label>
                Quantity{' '}
                <input
                    type="number"
                    min="1"
                    value={line.quantity}
                    onChange={(event) => onChange(index, 'quantity', event.target.value)}/>
            </label>

            <button type="button" className="ghost-button subtle" onClick={() => onRemove(index)}>
                Remove
            </button>
        </div>
    )
}

export default BasketLine