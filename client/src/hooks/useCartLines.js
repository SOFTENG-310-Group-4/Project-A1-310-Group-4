import { useEffect, useState } from 'react'

export const EMPTY_CART_LINE = { productId: '', quantity: 1 }
const KEEP_BASKET_STATE_KEY = 'grocerfy-basket'

/**
 * Filters out any malicious basket lines and rebuilds the remaining ones from
 * scratch, since values taken from localStorage cannot be blindly trusted.
 * 
 * Used both when reading data from and writing data to, as these are the
 * exchange points between localStorage.
 */
function sanitizeCartLines(lines, fallbackLines = [{ ...EMPTY_CART_LINE }]) {
    if (!Array.isArray(lines)) {
        return fallbackLines
    }

    return lines
        .filter((line) => {
            if (!line || typeof line !== 'object' || Array.isArray(line)) {
                return false
            }

            const validProductId =
                line.productId === '' ||
                (typeof line.productId === 'string' &&
                    /^\d+$/.test(line.productId) &&
                    Number(line.productId) > 0) ||
                (Number.isInteger(line.productId) && line.productId > 0)
            const quantity = Number(line.quantity)

            return validProductId && Number.isInteger(quantity) && quantity >= 1
        })
        .map((line) => ({
            productId: line.productId === '' ? '' : String(line.productId),
            quantity: Number(line.quantity),
        }))
}

function getSavedCartLines(initialLines) {
    const savedLines = localStorage.getItem(KEEP_BASKET_STATE_KEY)

    if (!savedLines) {
        return sanitizeCartLines(initialLines)
    }

    try {
        return sanitizeCartLines(JSON.parse(savedLines), initialLines)
    } catch {
        return sanitizeCartLines(initialLines)
    }
}

/**
 * Collection of hooks for managing the basket line items (product + quantity).
 * Contains operations for adding, removing, and updating basket lines.
 */
export function useCartLines(initialLines = [{ ...EMPTY_CART_LINE }]) {
    // Let users have persisting basket state across different sessions, as long
    // as they are on the same host
    const [cartLines, setCartLines] = useState(() => getSavedCartLines(initialLines))

    useEffect(() => {
        const safeLines = sanitizeCartLines(cartLines)
        localStorage.setItem(KEEP_BASKET_STATE_KEY, JSON.stringify(safeLines))
    }, [cartLines])

    function updateLine(index, field, value) {
        setCartLines((currentLines) =>
            currentLines.map((line, lineIndex) =>
                lineIndex === index ? { ...line, [field]: value } : line,
            ),
        )
    }

    function addLine() {
        setCartLines((currentLines) => [...currentLines, { ...EMPTY_CART_LINE }])
    }

    function removeLine(index) {
        setCartLines((currentLines) => currentLines.length === 1 ? currentLines : currentLines.filter((_, lineIndex) => lineIndex !== index))
    }

    return { cartLines, setCartLines, updateLine, addLine, removeLine }
}
