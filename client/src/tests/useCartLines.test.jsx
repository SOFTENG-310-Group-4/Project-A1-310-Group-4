import { act, renderHook } from '@testing-library/react'
import { describe, expect, it } from 'vitest'

import { EMPTY_CART_LINE, useCartLines } from '../hooks/useCartLines'

/**
 * Unit tests for the basket line state hook 
 *
 * These cover adding, updating and removing lines directly, including the guard
 * that stops the last remaining line from being removed.
 */
describe('useCartLines', () => {
  it('starts with one empty line by default', () => {
    const { result } = renderHook(() => useCartLines())

    expect(result.current.cartLines).toEqual([EMPTY_CART_LINE])
  })

  it('accepts an explicit set of initial lines', () => {
    const initial = [{ productId: '4', quantity: 2 }]
    const { result } = renderHook(() => useCartLines(initial))

    expect(result.current.cartLines).toEqual(initial)
  })

  it('appends a new empty line', () => {
    const { result } = renderHook(() => useCartLines())

    act(() => result.current.addLine())

    expect(result.current.cartLines).toHaveLength(2)
    expect(result.current.cartLines[1]).toEqual(EMPTY_CART_LINE)
  })

  it('gives each added line its own object rather than a shared reference', () => {
    const { result } = renderHook(() => useCartLines())

    act(() => result.current.addLine())
    act(() => result.current.updateLine(1, 'quantity', 9))

    expect(result.current.cartLines[0].quantity).toBe(1)
    expect(result.current.cartLines[1].quantity).toBe(9)
  })

  it('updates one field on the line at the given index', () => {
    const { result } = renderHook(() => useCartLines())

    act(() => result.current.updateLine(0, 'productId', '3'))

    expect(result.current.cartLines[0]).toEqual({ productId: '3', quantity: 1 })
  })

  it('leaves other lines untouched when one is updated', () => {
    const { result } = renderHook(() =>
      useCartLines([
        { productId: '1', quantity: 1 },
        { productId: '2', quantity: 2 },
      ]),
    )

    act(() => result.current.updateLine(1, 'quantity', 5))

    expect(result.current.cartLines[0]).toEqual({ productId: '1', quantity: 1 })
    expect(result.current.cartLines[1]).toEqual({ productId: '2', quantity: 5 })
  })

  it('removes the line at the given index', () => {
    const { result } = renderHook(() =>
      useCartLines([
        { productId: '1', quantity: 1 },
        { productId: '2', quantity: 2 },
        { productId: '3', quantity: 3 },
      ]),
    )

    act(() => result.current.removeLine(1))

    expect(result.current.cartLines).toEqual([
      { productId: '1', quantity: 1 },
      { productId: '3', quantity: 3 },
    ])
  })

  it('keeps the last line rather than emptying the basket', () => {
    const { result } = renderHook(() => useCartLines([{ productId: '1', quantity: 1 }]))

    act(() => result.current.removeLine(0))

    expect(result.current.cartLines).toEqual([{ productId: '1', quantity: 1 }])
  })

  it('allows the basket to be replaced wholesale', () => {
    const { result } = renderHook(() => useCartLines())

    act(() => result.current.setCartLines([{ productId: '7', quantity: 4 }]))

    expect(result.current.cartLines).toEqual([{ productId: '7', quantity: 4 }])
  })

    it('saves basket changes to localStorage', () => {
    const { result } = renderHook(() => useCartLines())

    act(() => {
      result.current.updateLine(0, 'productId', '3')
    })

    expect(JSON.parse(localStorage.getItem('grocerfy-basket'))).toEqual([
      { productId: '3', quantity: 1 },
    ])
  })

  it('restores the basket from localStorage', () => {
    const savedLines = [
      { productId: '3', quantity: 2 },
      { productId: '5', quantity: 1 },
    ]

    localStorage.setItem('grocerfy-basket', JSON.stringify(savedLines))

    const { result } = renderHook(() => useCartLines())

    expect(result.current.cartLines).toEqual(savedLines)
  })

  it('restores the basket after the hook is recreated', () => {
    const firstRender = renderHook(() => useCartLines())

    act(() => {
      firstRender.result.current.updateLine(0, 'productId', '4')
      firstRender.result.current.updateLine(0, 'quantity', 3)
    })

    firstRender.unmount()

    const secondRender = renderHook(() => useCartLines())

    expect(secondRender.result.current.cartLines).toEqual([
      { productId: '4', quantity: 3 },
    ])
  })

  it('uses initial lines when saved basket data is malformed', () => {
    const initialLines = [{ productId: '4', quantity: 2 }]

    localStorage.setItem('grocerfy-basket', 'invalid json')

    const { result } = renderHook(() => useCartLines(initialLines))

    expect(result.current.cartLines).toEqual(initialLines)
  })
})