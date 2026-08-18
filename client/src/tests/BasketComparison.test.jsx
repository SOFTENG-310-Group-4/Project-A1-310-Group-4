import { render, screen, waitFor, within } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'

import App from '../App'

/**
 * Integration tests for the basket comparison
 *
 * The catalogue response is fixed, what varies per test is the response
 * from POST /api/basket/compare, so both the success and failure paths of
 * handleSubmit are exercised.
 */
const CATALOGUE = [
  { productId: 1, productName: 'Apple', displayName: 'NZ Gala Apple' },
  { productId: 2, productName: 'Trim Milk', displayName: 'Anchor Trim Milk' },
]

const CHEAPEST = {
  storeId: 2,
  storeName: 'Woolworths Grey Lynn',
  region: 'Auckland',
  address: '2 Example Street',
  available: true,
  availableSubtotal: 9.99,
  missingItems: [],
  lineItems: [{ productId: 1, productName: 'NZ Gala Apple', quantity: 1, lineTotal: 9.99 }],
}

const COMPARISON = {
  stores: [CHEAPEST],
  cheapestAvailableStore: CHEAPEST,
}

let postedBodies = []
/** Set per test to control what the compare endpoint returns. */
let compareResponse

beforeEach(() => {
  postedBodies = []
  compareResponse = { ok: true, json: async () => COMPARISON }

  vi.stubGlobal('fetch', vi.fn(async (url, options) => {
    if (String(url).startsWith('/api/product')) {
      return { ok: true, json: async () => CATALOGUE }
    }

    postedBodies.push(JSON.parse(options.body))
    return compareResponse
  }))
})

afterEach(() => {
  vi.unstubAllGlobals()
})

async function renderAndLoad() {
  const user = userEvent.setup()
  render(<App />)
  await waitFor(() =>
    expect(within(screen.getByRole('complementary')).getByText('Apple')).toBeInTheDocument(),
  )
  return user
}

function basket() {
  return screen.getByRole('button', { name: /compare basket/i })
}

describe('basket comparison', () => {
  it('posts the selected products and quantities to the compare endpoint', async () => {
    const user = await renderAndLoad()

    await user.click(basket())

    await waitFor(() => expect(postedBodies).toHaveLength(1))
    expect(postedBodies[0]).toEqual({ items: [{ productId: 1, quantity: 1 }] })
  })

  it('sends the updated quantity when a line is changed', async () => {
    const user = await renderAndLoad()

    const quantity = screen.getByLabelText(/quantity/i)
    await user.clear(quantity)
    await user.type(quantity, '3')
    await user.click(basket())

    await waitFor(() => expect(postedBodies).toHaveLength(1))
    expect(postedBodies[0].items[0].quantity).toBe(3)
  })

  it('omits basket lines that have no product selected', async () => {
    const user = await renderAndLoad()

    await user.click(screen.getByRole('button', { name: /add item/i }))
    await user.click(basket())

    await waitFor(() => expect(postedBodies).toHaveLength(1))
    expect(postedBodies[0].items).toHaveLength(1)
  })

  it('shows the results panel once the comparison returns', async () => {
    const user = await renderAndLoad()

    await user.click(basket())

    expect(await screen.findByText('Cheapest available option')).toBeInTheDocument()
    expect(screen.getByRole('heading', { name: 'Woolworths Grey Lynn' })).toBeInTheDocument()
  })

  it('does not show a results panel before a comparison is requested', async () => {
    await renderAndLoad()

    expect(screen.queryByText('Cheapest available option')).not.toBeInTheDocument()
  })

  it('shows the error returned by the server when the comparison fails', async () => {
    compareResponse = { ok: false, json: async () => ({ error: 'Basket must not be empty.' }) }
    const user = await renderAndLoad()

    await user.click(basket())

    expect(await screen.findByText('Basket must not be empty.')).toBeInTheDocument()
  })

  it('falls back to a generic message when the server sends no error text', async () => {
    compareResponse = { ok: false, json: async () => ({}) }
    const user = await renderAndLoad()

    await user.click(basket())

    expect(await screen.findByText('Unable to compare basket.')).toBeInTheDocument()
  })

  it('clears a previous result when a failing comparison is retried', async () => {
    const user = await renderAndLoad()

    await user.click(basket())
    expect(await screen.findByText('Cheapest available option')).toBeInTheDocument()

    compareResponse = { ok: false, json: async () => ({ error: 'Server unavailable.' }) }
    await user.click(basket())

    expect(await screen.findByText('Server unavailable.')).toBeInTheDocument()
    expect(screen.queryByText('Cheapest available option')).not.toBeInTheDocument()
  })

  it('re-enables the compare button after the request settles', async () => {
    const user = await renderAndLoad()

    await user.click(basket())

    await waitFor(() => expect(basket()).toBeEnabled())
  })
})