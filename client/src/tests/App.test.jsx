import { render, screen, waitFor, within } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'

import App from '../App'

/**
 * Integration tests for the dietary filter
 *
 * fetch is stubbed rather than hitting the real backend, so these run without a
 * server. The stub applies the same strict AND semantics as GET /api/product:
 * a product is returned only if it carries every requested tag.
 */
const CATALOGUE = [
  { productId: 1, productName: 'Apple', displayName: 'NZ Gala Apple', glutenFree: true, lactoseFree: true, vegetarian: true, vegan: true },
  { productId: 2, productName: 'Trim Milk', displayName: 'Anchor Trim Milk', glutenFree: true, lactoseFree: false, vegetarian: true, vegan: false },
  { productId: 3, productName: 'White Bread', displayName: 'Tip Top White Bread', glutenFree: false, lactoseFree: true, vegetarian: true, vegan: true },
  { productId: 4, productName: 'Chicken Breast', displayName: 'Tegel Chicken Breast', glutenFree: true, lactoseFree: true, vegetarian: false, vegan: false },
]

const TAG_FIELDS = {
  gluten_free: 'glutenFree',
  lactose_free: 'lactoseFree',
  vegetarian: 'vegetarian',
  vegan: 'vegan',
}

/** Records every URL the component requested, so tests can assert on them. */
let requestedUrls = []

function filterCatalogue(url) {
  const tags = new URL(url, 'http://localhost').searchParams.getAll('dietary')
  return CATALOGUE.filter((product) => tags.every((tag) => product[TAG_FIELDS[tag]]))
}

beforeEach(() => {
  requestedUrls = []

  vi.stubGlobal('fetch', vi.fn(async (url) => {
    requestedUrls.push(url)

    if (String(url).startsWith('/api/product')) {
      return { ok: true, json: async () => filterCatalogue(url) }
    }

    return { ok: true, json: async () => ({ stores: [], cheapestAvailableStore: null }) }
  }))
})

afterEach(() => {
  vi.unstubAllGlobals()
})

/** The catalogue list in the left column, as opposed to the basket dropdown. */
function catalogue() {
  return screen.getByRole('complementary')
}

async function renderAndLoad() {
  const user = userEvent.setup()
  render(<App />)
  await waitFor(() => expect(within(catalogue()).getByText('Apple')).toBeInTheDocument())
  return user
}

describe('dietary filtering', () => {
  it('requests the unfiltered catalogue on first load', async () => {
    await renderAndLoad()

    expect(requestedUrls[0]).toBe('/api/product')
    expect(within(catalogue()).getAllByRole('listitem')).toHaveLength(4)
  })

  it('sends the selected tag as a dietary query parameter', async () => {
    const user = await renderAndLoad()

    await user.click(screen.getByRole('checkbox', { name: 'Vegan' }))

    await waitFor(() => expect(requestedUrls).toContain('/api/product?dietary=vegan'))
  })

  it('removes products that do not carry the selected tag', async () => {
    const user = await renderAndLoad()

    await user.click(screen.getByRole('checkbox', { name: 'Vegan' }))

    await waitFor(() => {
      expect(within(catalogue()).queryByText('Trim Milk')).not.toBeInTheDocument()
    })
    expect(within(catalogue()).getByText('Apple')).toBeInTheDocument()
    expect(within(catalogue()).getByText('White Bread')).toBeInTheDocument()
  })

  it('requires every selected tag rather than any of them', async () => {
    const user = await renderAndLoad()

    await user.click(screen.getByRole('checkbox', { name: 'Vegan' }))
    await user.click(screen.getByRole('checkbox', { name: 'Gluten free' }))

    // White Bread is vegan but not gluten free, so an OR would wrongly keep it.
    await waitFor(() => {
      expect(within(catalogue()).queryByText('White Bread')).not.toBeInTheDocument()
    })
    expect(within(catalogue()).getByText('Apple')).toBeInTheDocument()
    expect(within(catalogue()).queryByText('Trim Milk')).not.toBeInTheDocument()
  })

  it('restores the full catalogue when a tag is unticked', async () => {
    const user = await renderAndLoad()

    await user.click(screen.getByRole('checkbox', { name: 'Vegan' }))
    await waitFor(() => expect(within(catalogue()).queryByText('Trim Milk')).not.toBeInTheDocument())

    await user.click(screen.getByRole('checkbox', { name: 'Vegan' }))

    await waitFor(() => expect(within(catalogue()).getByText('Trim Milk')).toBeInTheDocument())
  })

  it('clears every tag when the clear button is used', async () => {
    const user = await renderAndLoad()

    await user.click(screen.getByRole('checkbox', { name: 'Vegan' }))
    await user.click(screen.getByRole('checkbox', { name: 'Gluten free' }))
    await user.click(screen.getByRole('button', { name: /clear all/i }))

    await waitFor(() => {
      expect(screen.getByRole('checkbox', { name: 'Vegan' })).not.toBeChecked()
    })
    expect(screen.getByRole('checkbox', { name: 'Gluten free' })).not.toBeChecked()
    expect(within(catalogue()).getAllByRole('listitem')).toHaveLength(4)
  })

  it('tells the user when no product matches the selected tags', async () => {
    const user = userEvent.setup()
    vi.stubGlobal('fetch', vi.fn(async () => ({ ok: true, json: async () => [] })))
    render(<App />)

    await waitFor(() => {
      expect(screen.getByText('No products match every selected dietary tag.')).toBeInTheDocument()
    })
    void user
  })
})

describe('basket interaction with the filter', () => {
  it('offers only filtered products in the basket dropdown', async () => {
    const user = await renderAndLoad()

    await user.click(screen.getByRole('checkbox', { name: 'Vegan' }))

    await waitFor(() => {
      const options = within(screen.getByRole('combobox')).getAllByRole('option')
      const labels = options.map((option) => option.textContent)
      expect(labels).not.toContain('Anchor Trim Milk')
      expect(labels).toContain('NZ Gala Apple')
    })
  })

  it('removes a basket line whose product falls outside the filter', async () => {
    const user = await renderAndLoad()

    const select = screen.getByRole('combobox')
    await user.selectOptions(select, '2')
    expect(select).toHaveValue('2')

    await user.click(screen.getByRole('checkbox', { name: 'Vegan' }))

    // Trim Milk is not vegan, so the line must be removed entirely, 
    // rather than being left behind as an empty "Select a product" row.
    await waitFor(() => expect(screen.queryByRole('combobox')).not.toBeInTheDocument())
  })

  it('keeps a basket line whose product still matches the filter', async () => {
    const user = await renderAndLoad()

    const select = screen.getByRole('combobox')
    await user.selectOptions(select, '1')

    await user.click(screen.getByRole('checkbox', { name: 'Vegan' }))

    await waitFor(() => expect(requestedUrls).toContain('/api/product?dietary=vegan'))
    expect(screen.getByRole('combobox')).toHaveValue('1')
  })

  it('does not reset the basket selection when a filter changes', async () => {
    const user = await renderAndLoad()

    await user.selectOptions(screen.getByRole('combobox'), '3')
    await user.click(screen.getByRole('checkbox', { name: 'Vegan' }))

    // White Bread is vegan, so the pre fill guard must not overwrite the choice.
    await waitFor(() => expect(requestedUrls).toContain('/api/product?dietary=vegan'))
    expect(screen.getByRole('combobox')).toHaveValue('3')
  })
})