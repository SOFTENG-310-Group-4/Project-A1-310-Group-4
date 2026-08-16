/**
 * Representation of a single line in the basket:
 * Has product dropdown, quantity input, and remove button.
 * 
 * Products already selected in a different basket line are
 * disabled in the dropdown selection.
 */
function BasketLine({ line, index, products, cartLines, onChange, onRemove }) {
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
                    {products.map((product) => {
                        const isSelectedElsewhere = cartLines.some(
                            (otherLine, otherIndex) =>
                                otherIndex !== index && String(otherLine.productId) === String(product.productId),
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