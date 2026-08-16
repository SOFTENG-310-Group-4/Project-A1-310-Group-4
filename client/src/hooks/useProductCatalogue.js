import { useEffect, useState } from 'react'
import { fetchProducts } from '../api/productsApi'

/**
 * Whenever the dietary tags change, fetch the product catalogue from the backend
 * since the filtering is done on the server side.
 */
export function useProductCatalogue(dietaryTags, onSuccess, onError) {
    const [products, setProducts] = useState([])
    const [loading, setLoading] = useState(true)

    useEffect(() => {
        let active = true

        async function loadProducts() {
            setLoading(true)
            try {
                const data = await fetchProducts(dietaryTags)
                if (!active) {
                    return
                }
                setProducts(data)
                onSuccess(data)
            } catch (loadError) {
                if (active) {
                    onError(loadError.message)
                }
            } finally {
                if (active) {
                    setLoading(false)
                }
            }
        }

        loadProducts()
        return () => {
            active = false
        }
    }, [dietaryTags])

    return { products, loading }
    }

