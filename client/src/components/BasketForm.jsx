import BasketLine from './BasketLine'

/**
 * Form for the basket, containing list of BasketLine rows, an
 * "Add item" button, and a "Compare basket" button.
 */
function BasketForm({ cartLines, products, loading, error, onAddLine, onRemoveLine, onUpdateLine, onSubmit, setError }) {

    function handleAddLine() {
    const hasEmptyLine = cartLines.some(
        (line) => line.productId === ''
    )

    const selectedProductCount = cartLines.filter(
        (line) => line.productId !== ''
    ).length

    if (hasEmptyLine) {
        setError('Please select a product before adding another item.')
        return
    }

    if (selectedProductCount >= products.length) {
        setError('All available products have already been added.')
        return
    }

    setError('')
    onAddLine()
}

    return (
        <form className="basket-form" onSubmit={onSubmit}>
            <div className="section-heading">
                <h2>Basket</h2>
                <button type="button"className="ghost-button"onClick={handleAddLine}>Add item</button>
            </div>

            <div className="basket-lines">
                {cartLines.map((line, index) => (
                    <BasketLine
                        key={`${index}-${line.productId}`}
                        line={line}
                        index={index}
                        products={products}
                        cartLines={cartLines}
                        onChange={onUpdateLine}
                        onRemove={onRemoveLine}
                    />
                ))}
            </div>

            <button type="submit" className="primary-button" disabled={loading || products.length === 0}>
                {loading ? 'Comparing stores...' : 'Compare basket'}
            </button>

            {error ? <p className="error-banner">{error}</p> : null}
        </form>
    )
}

export default BasketForm