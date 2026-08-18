import { render, screen } from '@testing-library/react'
import { describe, expect, it } from 'vitest'

import Hero from '../components/Hero'

/**
 * Unit tests for the page header
 *
 * The header carries the cart summary, so the counts and the singular/plural
 * wording are the parts worth asserting on.
 */
describe('Hero', () => {
  it('shows the total number of items in the cart', () => {
    render(<Hero basketQuantity={7} selectedProducts={3} />)

    expect(screen.getByText('7')).toBeInTheDocument()
  })

  it('uses singular wording for a single selected product', () => {
    render(<Hero basketQuantity={2} selectedProducts={1} />)

    expect(screen.getByText('1 product selected')).toBeInTheDocument()
  })

  it('uses plural wording for several selected products', () => {
    render(<Hero basketQuantity={5} selectedProducts={3} />)

    expect(screen.getByText('3 products selected')).toBeInTheDocument()
  })

  it('uses plural wording for an empty cart', () => {
    render(<Hero basketQuantity={0} selectedProducts={0} />)

    expect(screen.getByText('0 products selected')).toBeInTheDocument()
  })
})