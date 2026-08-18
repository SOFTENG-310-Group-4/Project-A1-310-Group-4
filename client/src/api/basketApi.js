/**
 * Compares the current basket with the server-side to get the
 * per-store prices and availability of basket items.
 */
export async function compareBasket(payload) {
      const response = await fetch('/api/basket/compare', {
        method: 'POST',
        headers: {
          'Content-Type': 'application/json',
        },
        body: JSON.stringify(payload),
      })

      const data = await response.json()
      if (!response.ok) {
        throw new Error(data.error || 'Unable to compare basket.')
      }
      return data;
}