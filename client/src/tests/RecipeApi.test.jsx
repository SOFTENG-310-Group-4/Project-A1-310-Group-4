import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'

import { suggestRecipes } from '../api/recipeApi'

/**
 * Unit tests for the recipe suggestion API client
 *
 * The endpoint returns plain text rather than JSON, so the success path reads
 * the body as text, and a non-OK response surfaces the server's message via a
 * thrown Error. Network failures are left to propagate untouched.
 */
let fetchMock

beforeEach(() => {
  fetchMock = vi.fn()
  vi.stubGlobal('fetch', fetchMock)
})

afterEach(() => {
  vi.unstubAllGlobals()
})

describe('suggestRecipes', () => {
  it('POSTs the payload as JSON to the suggest endpoint', async () => {
    fetchMock.mockResolvedValue({ ok: true, text: async () => 'Garlic pasta.' })

    const payload = {
      items: [{ productId: 1, quantity: 2 }],
      dietaryTags: ['vegan'],
    }

    await suggestRecipes(payload)

    expect(fetchMock).toHaveBeenCalledTimes(1)
    expect(fetchMock).toHaveBeenCalledWith('/api/recipes/suggest', {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify(payload),
    })
  })

  it('returns the response body as plain text', async () => {
    fetchMock.mockResolvedValue({
      ok: true,
      text: async () => 'Tomato pasta with garlic and basil.',
    })

    const result = await suggestRecipes({ items: [] })

    expect(result).toBe('Tomato pasta with garlic and basil.')
  })

  it('throws with the server message when the response is not ok', async () => {
    fetchMock.mockResolvedValue({ ok: false, text: async () => 'Basket must not be empty.' })

    await expect(suggestRecipes({ items: [] })).rejects.toThrow('Basket must not be empty.')
  })

  it('falls back to a generic message when the error body is empty', async () => {
    fetchMock.mockResolvedValue({ ok: false, text: async () => '' })

    await expect(suggestRecipes({ items: [] })).rejects.toThrow(
      'Unable to suggest recipes.',
    )
  })

  it('propagates network errors raised by fetch', async () => {
    fetchMock.mockRejectedValue(new Error('Network down'))

    await expect(suggestRecipes({ items: [] })).rejects.toThrow('Network down')
  })
})