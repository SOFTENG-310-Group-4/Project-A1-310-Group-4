import { useState } from 'react'

export const EMPTY_CART_LINE = { productId: '', quantity: 1 }

/**
 * Collection of hooks for managing the basket line items (product + quantity).
 * Contains operations for adding, removing, and updating basket lines.
 */
export function useCartLines(initialLines = [{...EMPTY_CART_LINE }]) {
    const [cartLines, setCartLines] = useState(initialLines)

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