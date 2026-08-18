import { render, screen } from '@testing-library/react'
import { describe, expect, it } from 'vitest'

import ResultsPanel from '../components/ResultsPanel'

/**
 * Unit tests for the results panel
 *
 * The important case here is the fallback, when no single store can fulfil the
 * whole basket, the panel must say so rather than presenting a partial total as
 * if it were the cheapest option.
 */
function makeStore(overrides = {}) {
  return {
    storeId: 1,
    storeName: "PAK'nSAVE Mount Albert",
    region: 'Auckland',
    address: '10 Example Road',
    available: true,
    availableSubtotal: 12.5,
    missingItems: [],
    lineItems: [{ productId: 1, productName: 'NZ Gala Apple', quantity: 2, lineTotal: 12.5 }],
    ...overrides,
  }
}

const CHEAPEST = makeStore({ storeId: 2, storeName: 'Woolworths Grey Lynn', availableSubtotal: 9.99 })

describe('ResultsPanel', () => {
  it('reports how many stores were compared', () => {
    render(
      <ResultsPanel
        comparison={{ stores: [makeStore(), CHEAPEST], cheapestAvailableStore: CHEAPEST }}
      />,
    )

    expect(screen.getByText('2 stores compared')).toBeInTheDocument()
  })

  it('renders a card for every store in the comparison', () => {
    render(
      <ResultsPanel
        comparison={{ stores: [makeStore(), CHEAPEST], cheapestAvailableStore: CHEAPEST }}
      />,
    )

    expect(screen.getByRole('heading', { name: "PAK'nSAVE Mount Albert" })).toBeInTheDocument()
    expect(screen.getByRole('heading', { name: 'Woolworths Grey Lynn' })).toBeInTheDocument()
  })

  it('highlights the cheapest store that can fulfil the basket', () => {
    render(
      <ResultsPanel
        comparison={{ stores: [makeStore(), CHEAPEST], cheapestAvailableStore: CHEAPEST }}
      />,
    )

    expect(screen.getByText('Cheapest available option')).toBeInTheDocument()
    expect(screen.getByText('$9.99', { selector: '.price' })).toBeInTheDocument()
  })

  it('says no store can fulfil the basket when there is no cheapest option', () => {
    render(
      <ResultsPanel
        comparison={{
          stores: [makeStore({ available: false, missingItems: [{ productId: 3, productName: 'Bread', quantity: 1 }] })],
          cheapestAvailableStore: null,
        }}
      />,
    )

    expect(screen.getByText(/no store can fulfil the full basket/i)).toBeInTheDocument()
  })

  it('still lists the store cards when no store can fulfil the basket', () => {
    render(
      <ResultsPanel
        comparison={{
          stores: [makeStore({ available: false, missingItems: [{ productId: 3, productName: 'Bread', quantity: 1 }] })],
          cheapestAvailableStore: null,
        }}
      />,
    )

    expect(screen.getByRole('heading', { name: "PAK'nSAVE Mount Albert" })).toBeInTheDocument()
    expect(screen.getByText(/Bread x 1/)).toBeInTheDocument()
  })

  it('handles a comparison with no stores at all', () => {
    render(<ResultsPanel comparison={{ stores: [], cheapestAvailableStore: null }} />)

    expect(screen.getByText('0 stores compared')).toBeInTheDocument()
    expect(screen.getByText(/no store can fulfil the full basket/i)).toBeInTheDocument()
  })
})