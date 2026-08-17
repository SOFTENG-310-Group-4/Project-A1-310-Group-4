import { render, screen } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { describe, expect, it, vi } from 'vitest'

import BasketForm from '../components/BasketForm'

/**
 * Unit tests for the basket form
 *
 * These cover the form shell in isolation, how many rows it renders, when the
 * submit button is disabled, and the error banner. The behaviour of an
 * individual row is covered in BasketLine.test.jsx.
 */
const PRODUCTS = [
  { productId: 1, displayName: 'NZ Gala Apple' },
  { productId: 2, displayName: 'Anchor Trim Milk' },
]

function renderForm(overrides = {}) {
  const props = {
    cartLines: [{ productId: '1', quantity: 1 }],
    products: PRODUCTS,
    loading: false,
    error: '',
    onAddLine: vi.fn(),
    onRemoveLine: vi.fn(),
    onUpdateLine: vi.fn(),
    onSubmit: vi.fn((event) => event.preventDefault()),
    ...overrides,
  }

  render(<BasketForm {...props} />)
  return props
}

describe('BasketForm', () => {
  it('renders a basket row for every cart line', () => {
    renderForm({
      cartLines: [
        { productId: '1', quantity: 1 },
        { productId: '2', quantity: 3 },
      ],
    })

    expect(screen.getAllByLabelText(/product/i)).toHaveLength(2)
  })

  it('calls the add handler when the add item button is used', async () => {
    const user = userEvent.setup()
    const { onAddLine } = renderForm()

    await user.click(screen.getByRole('button', { name: /add item/i }))

    expect(onAddLine).toHaveBeenCalledTimes(1)
  })

  it('submits the basket when the compare button is used', async () => {
    const user = userEvent.setup()
    const { onSubmit } = renderForm()

    await user.click(screen.getByRole('button', { name: /compare basket/i }))

    expect(onSubmit).toHaveBeenCalledTimes(1)
  })

  it('disables the compare button and shows progress while loading', () => {
    renderForm({ loading: true })

    expect(screen.getByRole('button', { name: /comparing stores/i })).toBeDisabled()
  })

  it('disables the compare button when no products are available', () => {
    renderForm({ products: [] })

    expect(screen.getByRole('button', { name: /compare basket/i })).toBeDisabled()
  })

  it('shows an error banner when an error is present', () => {
    renderForm({ error: 'Could not reach the server.' })

    expect(screen.getByText('Could not reach the server.')).toBeInTheDocument()
  })

  it('does not show an error banner when there is no error', () => {
    renderForm()

    expect(screen.queryByText(/could not reach/i)).not.toBeInTheDocument()
  })

  it('renders no basket rows when the cart is empty', () => {
    renderForm({ cartLines: [] })

    expect(screen.queryAllByLabelText(/product/i)).toHaveLength(0)
    expect(screen.getByRole('button', { name: /compare basket/i })).toBeEnabled()
  })
})