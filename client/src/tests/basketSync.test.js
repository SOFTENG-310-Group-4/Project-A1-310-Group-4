import { describe, expect, it } from 'vitest'

import { syncBasketWithFilteredProducts } from '../utils/basketSync'

/**
 * Unit tests for basket sync
 *
 * When the dietary filter changes, any basket line holding a product that is no
 * longer visible is removed, so the user cannot submit a basket containing an
 * item the filter has excluded. Lines with no product selected yet are kept, so
 * the user still has a row to work with.
 */
const VISIBLE = [{ productId: 1 }, { productId: 2 }]

describe('syncBasketWithFilteredProducts', () => {
  it('keeps lines whose product is still visible', () => {
    const lines = [{ productId: '1', quantity: 2 }]

    expect(syncBasketWithFilteredProducts(lines, VISIBLE)).toEqual(lines)
  })

  it('removes a line whose product is no longer visible', () => {
    const lines = [{ productId: '3', quantity: 2 }]

    expect(syncBasketWithFilteredProducts(lines, VISIBLE)).toEqual([])
  })

  it('leaves an empty placeholder line untouched', () => {
    const lines = [{ productId: '', quantity: 1 }]

    expect(syncBasketWithFilteredProducts(lines, VISIBLE)).toEqual(lines)
  })

  it('removes only the lines that are no longer visible', () => {
    const lines = [
      { productId: '1', quantity: 1 },
      { productId: '3', quantity: 2 },
      { productId: '2', quantity: 3 },
    ]

    expect(syncBasketWithFilteredProducts(lines, VISIBLE)).toEqual([
      { productId: '1', quantity: 1 },
      { productId: '2', quantity: 3 },
    ])
  })

  it('preserves the quantity on lines that survive', () => {
    const lines = [
      { productId: '3', quantity: 9 },
      { productId: '2', quantity: 5 },
    ]

    expect(syncBasketWithFilteredProducts(lines, VISIBLE)[0].quantity).toBe(5)
  })

  it('removes every line when no products are visible', () => {
    const lines = [
      { productId: '1', quantity: 1 },
      { productId: '2', quantity: 2 },
    ]

    expect(syncBasketWithFilteredProducts(lines, [])).toEqual([])
  })

  it('keeps placeholder lines even when no products are visible', () => {
    const lines = [
      { productId: '1', quantity: 1 },
      { productId: '', quantity: 2 },
    ]

    expect(syncBasketWithFilteredProducts(lines, [])).toEqual([
      { productId: '', quantity: 2 },
    ])
  })

  it('returns an empty array for an empty basket', () => {
    expect(syncBasketWithFilteredProducts([], VISIBLE)).toEqual([])
  })

  it('matches ids held as strings against numeric product ids', () => {
    const lines = [{ productId: '2', quantity: 1 }]

    expect(syncBasketWithFilteredProducts(lines, VISIBLE)).toEqual(lines)
  })

  it('does not mutate the lines it was given', () => {
    const lines = [
      { productId: '3', quantity: 1 },
      { productId: '1', quantity: 2 },
    ]

    syncBasketWithFilteredProducts(lines, VISIBLE)

    expect(lines).toHaveLength(2)
    expect(lines[0].productId).toBe('3')
  })
})