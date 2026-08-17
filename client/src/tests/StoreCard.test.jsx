import { render, screen } from '@testing-library/react'
import { describe, expect, it } from 'vitest'

import StoreCard from '../components/StoreCard'

/**
 * Unit tests for a single store comparison card
 *
 * These cover the availability status, price formatting, and the missing-items
 * block that only appears when a store cannot fulfil the whole basket.
 */
const AVAILABLE_STORE = {
  storeId: 1,
  storeName: "PAK'nSAVE Mount Albert",
  region: 'Auckland',
  address: '10 Example Road',
  available: true,
  availableSubtotal: 12.5,
  missingItems: [],
  lineItems: [
    { productId: 1, productName: 'NZ Gala Apple', quantity: 2, lineTotal: 5 },
    { productId: 2, productName: 'Anchor Trim Milk', quantity: 1, lineTotal: 7.5 },
  ],
}

const UNAVAILABLE_STORE = {
  ...AVAILABLE_STORE,
  storeId: 2,
  storeName: 'New World Victoria Park',
  available: false,
  availableSubtotal: 5,
  missingItems: [{ productId: 2, productName: 'Anchor Trim Milk', quantity: 1 }],
  lineItems: [{ productId: 1, productName: 'NZ Gala Apple', quantity: 2, lineTotal: 5 }],
}

describe('StoreCard', () => {
  it('shows the store name, region and address', () => {
    render(<StoreCard store={AVAILABLE_STORE} />)

    expect(screen.getByRole('heading', { name: "PAK'nSAVE Mount Albert" })).toBeInTheDocument()
    expect(screen.getByText('Auckland')).toBeInTheDocument()
    expect(screen.getByText('10 Example Road')).toBeInTheDocument()
  })

  it('marks a store that can fulfil the basket as available', () => {
    render(<StoreCard store={AVAILABLE_STORE} />)

    expect(screen.getByText('Basket available')).toBeInTheDocument()
    expect(screen.queryByText('Missing items')).not.toBeInTheDocument()
  })

  it('marks a store that cannot fulfil the basket as missing items', () => {
    render(<StoreCard store={UNAVAILABLE_STORE} />)

    expect(screen.getByText('Missing items', { selector: 'span' })).toBeInTheDocument()
    expect(screen.getByRole('heading', { name: 'Missing items' })).toBeInTheDocument()
  })

  it('formats the basket total to two decimal places', () => {
    render(<StoreCard store={AVAILABLE_STORE} />)

    expect(screen.getByText('$12.50')).toBeInTheDocument()
  })

  it('lists each missing item with its quantity', () => {
    render(<StoreCard store={UNAVAILABLE_STORE} />)

    expect(screen.getByText(/Anchor Trim Milk x 1/)).toBeInTheDocument()
  })

  it('does not render a missing items block when nothing is missing', () => {
    render(<StoreCard store={AVAILABLE_STORE} />)

    expect(screen.queryByRole('heading', { name: 'Missing items' })).not.toBeInTheDocument()
  })

  it('lists each available basket line with its quantity and line total', () => {
    render(<StoreCard store={AVAILABLE_STORE} />)

    expect(screen.getByText(/NZ Gala Apple x 2 - \$5\.00/)).toBeInTheDocument()
    expect(screen.getByText(/Anchor Trim Milk x 1 - \$7\.50/)).toBeInTheDocument()
  })

  it('shows a subtotal of zero when no basket lines are available', () => {
    render(
      <StoreCard
        store={{ ...UNAVAILABLE_STORE, availableSubtotal: 0, lineItems: [] }}
      />,
    )

    expect(screen.getByText('$0.00')).toBeInTheDocument()
  })
})