import { render, screen, waitFor, within } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'

import RecipeSuggestions from '../components/RecipeSuggestions'
import { suggestRecipes } from '../api/recipeApi'

/**
 * Unit tests for the recipe suggestion panel
 *
 * These cover the panel in isolation: the button state, what gets sent to the
 * API, and how the text, loading and error states render. The recipe API
 * client itself is covered in RecipeApi.test.js.
 */
vi.mock('../api/recipeApi', () => ({
  suggestRecipes: vi.fn(),
}))

const CART_LINES = [{ productId: '1', quantity: 2 }]
const DIETARY_TAGS = ['vegan']

function renderPanel(overrides = {}) {
  const props = {
    cartLines: CART_LINES,
    dietaryTags: DIETARY_TAGS,
    ...overrides,
  }

  render(<RecipeSuggestions {...props} />)
  return props
}

function suggestButton() {
  return screen.getByRole('button', { name: /suggest recipe/i })
}

beforeEach(() => {
  suggestRecipes.mockReset()
})

afterEach(() => {
  vi.restoreAllMocks()
})

describe('RecipeSuggestions', () => {
  it('shows the button and no recipe text on first render', () => {
    renderPanel()

    expect(suggestButton()).toBeInTheDocument()
    expect(screen.queryByText(/pasta/i)).not.toBeInTheDocument()
  })

  it('disables the button when the cart has no lines', () => {
    renderPanel({ cartLines: [] })

    expect(suggestButton()).toBeDisabled()
  })

  it('sends only the lines with a selected product, with numeric fields', async () => {
    suggestRecipes.mockResolvedValue('Garlic pasta.')
    const user = userEvent.setup()
    renderPanel({
      cartLines: [
        { productId: '1', quantity: '2' },
        { productId: '', quantity: '3' },
      ],
    })

    await user.click(suggestButton())

    await waitFor(() => expect(suggestRecipes).toHaveBeenCalledTimes(1))
    expect(suggestRecipes).toHaveBeenCalledWith({
      items: [{ productId: 1, quantity: 2 }],
      dietaryTags: DIETARY_TAGS,
    })
  })

  it('passes the current dietary tags through unchanged', async () => {
    suggestRecipes.mockResolvedValue('Done.')
    const user = userEvent.setup()
    renderPanel({ dietaryTags: ['vegan', 'gluten_free'] })

    await user.click(suggestButton())

    await waitFor(() => expect(suggestRecipes).toHaveBeenCalledTimes(1))
    expect(suggestRecipes.mock.calls[0][0].dietaryTags).toEqual(['vegan', 'gluten_free'])
  })

  it('shows the recipe text returned by the API', async () => {
    suggestRecipes.mockResolvedValue('Tomato pasta with garlic.')
    const user = userEvent.setup()
    renderPanel()

    await user.click(suggestButton())

    expect(await screen.findByText('Tomato pasta with garlic.')).toBeInTheDocument()
  })

  it('shows progress and disables the button while the request is in flight', async () => {
    let resolveRequest
    suggestRecipes.mockReturnValue(
      new Promise((resolve) => {
        resolveRequest = resolve
      }),
    )
    const user = userEvent.setup()
    renderPanel()

    await user.click(suggestButton())

    const thinking = screen.getByRole('button', { name: /thinking/i })
    expect(thinking).toBeDisabled()

    resolveRequest('Done.')
    await waitFor(() => expect(suggestButton()).toBeEnabled())
  })

  it('shows the error message when the request fails', async () => {
    suggestRecipes.mockRejectedValue(new Error('Server unavailable.'))
    const user = userEvent.setup()
    renderPanel()

    await user.click(suggestButton())

    expect(await screen.findByText('Server unavailable.')).toBeInTheDocument()
  })

  it('clears a previous error and result when the button is used again', async () => {
    suggestRecipes
      .mockRejectedValueOnce(new Error('First failure'))
      .mockResolvedValueOnce('Second attempt works.')
    const user = userEvent.setup()
    renderPanel()

    await user.click(suggestButton())
    expect(await screen.findByText('First failure')).toBeInTheDocument()

    await user.click(suggestButton())
    expect(await screen.findByText('Second attempt works.')).toBeInTheDocument()
    expect(screen.queryByText('First failure')).not.toBeInTheDocument()
  })

  it('renders the recipe as preformatted text so line breaks are preserved', async () => {
    suggestRecipes.mockResolvedValue('Line one.\nLine two.')
    const user = userEvent.setup()
    renderPanel()

    await user.click(suggestButton())

    const block = await screen.findByText(/Line one\./)
    expect(block.tagName).toBe('PRE')
    expect(block).toHaveTextContent('Line one.')
    expect(block).toHaveTextContent('Line two.')
  })

  it('does not call the API when the cart is empty', async () => {
    const user = userEvent.setup()
    renderPanel({ cartLines: [] })

    await user.click(suggestButton())

    expect(suggestRecipes).not.toHaveBeenCalled()
  })

  it('sends no items when the only line has no product selected', async () => {
    suggestRecipes.mockResolvedValue('Done.')
    const user = userEvent.setup()
    renderPanel({ cartLines: [{ productId: '', quantity: 1 }] })

    await user.click(suggestButton())

    await waitFor(() => expect(suggestRecipes).toHaveBeenCalledTimes(1))
    expect(suggestRecipes.mock.calls[0][0].items).toEqual([])
  })

  it('still renders the heading when the panel is idle', () => {
    renderPanel()

    expect(screen.getByRole('heading', { name: /recipe idea/i })).toBeInTheDocument()
  })

  it('does not show a loading state before the button is clicked', () => {
    renderPanel()

    expect(within(screen.getByRole('button', { name: /suggest recipe/i })).queryByText(/thinking/i)).not.toBeInTheDocument()
  })
})