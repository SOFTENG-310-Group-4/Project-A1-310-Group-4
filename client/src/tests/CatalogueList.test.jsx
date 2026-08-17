import { render, screen } from '@testing-library/react'
import { describe, expect, it } from 'vitest'

import CatalogueList from '../components/CatalogueList'

/**
 * Unit tests for the catalogue list
 *
 * The product count, the loading and empty states, and what each row outputs. 
 * How the catalogue is fetched and filtered is covered in App.test.jsx.
 */
const PRODUCTS = [
  { productId: 1, displayName: 'NZ Gala Apple', productName: 'apple-gala' },
  { productId: 2, displayName: 'Anchor Trim Milk', productName: 'milk-trim' },
]

describe('CatalogueList', () => {
  it('renders a row for every product', () => {
    render(<CatalogueList products={PRODUCTS} catalogueLoading={false} />)

    expect(screen.getAllByRole('listitem')).toHaveLength(PRODUCTS.length)
    expect(screen.getByText('NZ Gala Apple')).toBeInTheDocument()
    expect(screen.getByText('Anchor Trim Milk')).toBeInTheDocument()
  })

  it('shows both the display name and the underlying product name', () => {
    render(<CatalogueList products={[PRODUCTS[0]]} catalogueLoading={false} />)

    expect(screen.getByText('NZ Gala Apple')).toBeInTheDocument()
    expect(screen.getByText('apple-gala')).toBeInTheDocument()
  })

  it('reports how many products are shown', () => {
    render(<CatalogueList products={PRODUCTS} catalogueLoading={false} />)

    expect(screen.getByText('2 products')).toBeInTheDocument()
  })

  it('shows the loading message while the catalogue is being fetched', () => {
    render(<CatalogueList products={[]} catalogueLoading={true} />)

    expect(screen.getByText(/loading catalogue/i)).toBeInTheDocument()
  })

  it('does not show the empty state while still loading', () => {
    render(<CatalogueList products={[]} catalogueLoading={true} />)

    expect(screen.queryByText(/no products match/i)).not.toBeInTheDocument()
  })

  it('shows the empty state when loading has finished with no matches', () => {
    render(<CatalogueList products={[]} catalogueLoading={false} />)

    expect(screen.getByText(/no products match every selected dietary tag/i)).toBeInTheDocument()
    expect(screen.queryAllByRole('listitem')).toHaveLength(0)
  })

  it('does not show the empty state when products are present', () => {
    render(<CatalogueList products={PRODUCTS} catalogueLoading={false} />)

    expect(screen.queryByText(/no products match/i)).not.toBeInTheDocument()
  })
})