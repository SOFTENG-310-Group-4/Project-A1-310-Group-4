/**
 * When fetching products, filtering happens on the backend (server side) 
 * rather than the frontend (client side) since /api/product is the source 
 * of truth that applies the dietary tag filtering.
 */
export async function fetchProducts(dietaryTags) {
    const params = new URLSearchParams()
    dietaryTags.forEach((tag) => { params.append('dietary', tag) })
    const suffix = params.toString() ? `?${params}` : ''

    const response = await fetch(`/api/product${suffix}`)
    if (!response.ok) {
        throw new Error('Unable to load products.')
    }
    return response.json()
}