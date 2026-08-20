import { useEffect, useState } from 'react'

export const EMPTY_CART_LINE = { productId: '', quantity: 1 }

/**
 * Collection of hooks for managing the basket line items (product + quantity).
 * Contains operations for adding, removing, and updating basket lines.
 */
export function useCartLines(initialLines = [{...EMPTY_CART_LINE }]) {
    const KEEP_BASKET_STATE_KEY = 'grocerfy-basket'
    // Let users have persisting basket state across different sessions, as long
    // as they are on the same host
    const [cartLines, setCartLines] = useState(() => getSavedCartLines(initialLines))

    useEffect(() => {
        const safeLines = isValidCartLines(cartLines) ? cartLines : []
        localStorage.setItem(KEEP_BASKET_STATE_KEY, JSON.stringify(safeLines))
    }, [cartLines])

    function getSavedCartLines(initialLines) {
        const savedLines = localStorage.getItem(KEEP_BASKET_STATE_KEY)

        if (!savedLines) {
            return initialLines
        }

        try {
            const parsed = JSON.parse(savedLines)
            return isValidCartLines(parsed) ? parsed : initialLines
        } catch {
            return initialLines
        }
    }

    function updateLine(index, field, value) {
        setCartLines((currentLines) =>
            currentLines.map((line, lineIndex) =>
                lineIndex === index ? { ...line, [field]: value } : line,
            ),
        )
    }

    function addLine() {
        setCartLines((currentLines) => [...currentLines, {...EMPTY_CART_LINE}])
    }

    function removeLine(index) {
        setCartLines((currentLines) => currentLines.length === 1 ? currentLines : currentLines.filter((_, lineIndex) => lineIndex !== index))
    }

    return { cartLines, setCartLines, updateLine, addLine, removeLine }
}

/**
 * Validate that data read from or written to localStorage is actually a basket and not anything malicious.
 * Addresses security flag picked up by Sonar.
 */
function isValidCartLines(parsedValue) {
    return (
        Array.isArray(parsedValue) &&
        parsedValue.every(
             (line) =>
                line !== null &&
                typeof line === 'object' &&
                typeof line.productId === 'string' &&
                (typeof line.quantity === 'number' || typeof line.quantity === 'string'),
        )
    )
}