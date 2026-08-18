import { render, screen } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { describe, expect, it, vi } from 'vitest'

import BasketLine from '../components/BasketLine'

/**
 * Unit tests for a single basket row
 *
 * These cover the row in isolation, which options render, which are disabled
 * because another row already holds them, and what the change and remove
 * callbacks receive. How rows are added and removed as a set is covered in
 * useCartLines.test.jsx.
 */
const PRODUCTS = [
  { productId: 1, displayName: 'NZ Gala Apple' },
  { productId: 2, displayName: 'Anchor Trim Milk' },
]

function renderLine(overrides = {}) {
  const props = {
    line: { productId: '1', quantity: 1 },
    index: 0,
    products: PRODUCTS,
    cartLines: [{ productId: '1', quantity: 1 }],
    onChange: vi.fn(),
    onRemove: vi.fn(),
    ...overrides,
  }

  render(<BasketLine {...props} />)
  return props
}

describe('BasketLine', () => {
  it('renders an option for every product plus the empty placeholder', () => {
    renderLine()

    expect(screen.getAllByRole('option')).toHaveLength(PRODUCTS.length + 1)
    expect(screen.getByRole('option', { name: 'Select a product' })).toBeInTheDocument()
  })

  it('shows the product currently held by this line as selected', () => {
    renderLine({ line: { productId: '2', quantity: 1 } })

    expect(screen.getByLabelText(/product/i)).toHaveValue('2')
  })

  it('disables the dropdown when no products are available', () => {
    renderLine({ products: [] })

    expect(screen.getByLabelText(/product/i)).toBeDisabled()
  })

  it('disables a product that another basket line already holds', () => {
    renderLine({
      cartLines: [
        { productId: '1', quantity: 1 },
        { productId: '2', quantity: 1 },
      ],
    })

    expect(screen.getByRole('option', { name: /Anchor Trim Milk \(Already added\)/ })).toBeDisabled()
    expect(screen.getByRole('option', { name: 'NZ Gala Apple' })).toBeEnabled()
  })

  it('reports the new product id when the selection changes', async () => {
    const user = userEvent.setup()
    const { onChange } = renderLine()

    await user.selectOptions(screen.getByLabelText(/product/i), '2')

    expect(onChange).toHaveBeenCalledWith(0, 'productId', '2')
  })

  it('shows the current quantity', () => {
    renderLine({ line: { productId: '1', quantity: 4 } })

    expect(screen.getByLabelText(/quantity/i)).toHaveValue(4)
  })

  it('reports the new quantity when it is edited', async () => {
    const user = userEvent.setup()
    const { onChange } = renderLine({ index: 3 })

    await user.type(screen.getByLabelText(/quantity/i), '2')

    expect(onChange).toHaveBeenCalledWith(3, 'quantity', '12')
  })

  it('does not allow the quantity to be set below one', () => {
    renderLine()

    expect(screen.getByLabelText(/quantity/i)).toHaveAttribute('min', '1')
  })

  it('reports its own index when the remove button is used', async () => {
    const user = userEvent.setup()
    const { onRemove } = renderLine({ index: 2 })

    await user.click(screen.getByRole('button', { name: /remove/i }))

    expect(onRemove).toHaveBeenCalledWith(2)
  })
})