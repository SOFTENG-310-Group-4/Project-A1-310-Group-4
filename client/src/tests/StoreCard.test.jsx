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

  describe('member prices', () => {
    const WOOLWORTHS = {
      ...AVAILABLE_STORE,
      storeId: 3,
      storeName: 'Woolworths Three Kings',
      availableSubtotal: 11.4,
      loyaltyProgram: 'EVERYDAY_REWARDS',
      memberSavingApplied: 1.1,
      memberSavingAvailable: 0,
      lineItems: [
        { productId: 1, productName: 'Natural Yoghurt', quantity: 2, unitPrice: 4.2, lineTotal: 8.4,
          regularUnitPrice: 4.75, memberPriceApplied: true },
        { productId: 2, productName: 'White Bread', quantity: 1, unitPrice: 3, lineTotal: 3,
          regularUnitPrice: 3, memberPriceApplied: false },
      ],
    }

    it('shows member-priced lines with the regular price struck through', () => {
      render(<StoreCard store={WOOLWORTHS} />)

      expect(screen.getByText(/Natural Yoghurt x 2 - \$8\.40/)).toBeInTheDocument()
      expect(screen.getByLabelText('regular price')).toHaveTextContent('$9.50')
      expect(screen.getByText('Everyday Rewards price')).toBeInTheDocument()
      expect(screen.getByText('Includes $1.10 Everyday Rewards savings')).toBeInTheDocument()
    })

    it('only marks the lines that used a member price', () => {
      render(<StoreCard store={WOOLWORTHS} />)

      expect(screen.getAllByText('Everyday Rewards price')).toHaveLength(1)
      expect(screen.getAllByLabelText('regular price')).toHaveLength(1)
    })

    it('shows the possible saving when the card is not in use', () => {
      render(
        <StoreCard
          store={{
            ...WOOLWORTHS,
            availableSubtotal: 12.5,
            memberSavingApplied: 0,
            memberSavingAvailable: 1.1,
            lineItems: WOOLWORTHS.lineItems.map((line) => ({ ...line, memberPriceApplied: false })),
          }}
        />,
      )

      expect(screen.getByText('Save $1.10 with Everyday Rewards')).toBeInTheDocument()
      expect(screen.queryByText('Everyday Rewards price')).not.toBeInTheDocument()
    })

    it('shows nothing about member prices for stores without them', () => {
      render(<StoreCard store={AVAILABLE_STORE} />)

      expect(screen.queryByText(/Everyday Rewards/)).not.toBeInTheDocument()
      expect(screen.queryByLabelText('regular price')).not.toBeInTheDocument()
    })
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