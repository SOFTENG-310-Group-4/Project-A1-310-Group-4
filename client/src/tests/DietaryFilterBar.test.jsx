import { render, screen } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { describe, expect, it, vi } from 'vitest'

import DietaryFilterBar from '../components/DietaryFilterBar'
import { DIETARY_TAGS } from '../constants/dietaryTags'

/**
 * Unit tests for the dietary filter sidebar (issue #32).
 *
 * These cover the component in isolation: which chips render, which appear
 * checked, and what the callbacks receive. Whether the filter actually removes
 * products is covered in App.test.jsx.
 */
function renderFilter(overrides = {}) {
  const props = {
    selectedTags: [],
    onToggleTag: vi.fn(),
    onClear: vi.fn(),
    matchCount: 10,
    ...overrides,
  }

  render(<DietaryFilterBar {...props} />)
  return props
}

describe('DietaryFilterBar', () => {
  it('renders a checkbox for every supported dietary tag', () => {
    renderFilter()

    expect(screen.getAllByRole('checkbox')).toHaveLength(DIETARY_TAGS.length)
    expect(screen.getByRole('checkbox', { name: 'Gluten free' })).toBeInTheDocument()
    expect(screen.getByRole('checkbox', { name: 'Lactose free' })).toBeInTheDocument()
    expect(screen.getByRole('checkbox', { name: 'Vegetarian' })).toBeInTheDocument()
    expect(screen.getByRole('checkbox', { name: 'Vegan' })).toBeInTheDocument()
  })

  it('checks only the tags that are currently selected', () => {
    renderFilter({ selectedTags: ['vegan', 'gluten_free'] })

    expect(screen.getByRole('checkbox', { name: 'Vegan' })).toBeChecked()
    expect(screen.getByRole('checkbox', { name: 'Gluten free' })).toBeChecked()
    expect(screen.getByRole('checkbox', { name: 'Vegetarian' })).not.toBeChecked()
    expect(screen.getByRole('checkbox', { name: 'Lactose free' })).not.toBeChecked()
  })

  it('reports the tag value, not the label, when a chip is clicked', async () => {
    const user = userEvent.setup()
    const { onToggleTag } = renderFilter()

    await user.click(screen.getByRole('checkbox', { name: 'Gluten free' }))

    // The API rejects unknown tag names with a 400, so the exact value matters.
    expect(onToggleTag).toHaveBeenCalledExactlyOnceWith('gluten_free')
  })

  it('reports the same tag when an already selected chip is clicked again', async () => {
    const user = userEvent.setup()
    const { onToggleTag } = renderFilter({ selectedTags: ['vegan'] })

    await user.click(screen.getByRole('checkbox', { name: 'Vegan' }))

    expect(onToggleTag).toHaveBeenCalledExactlyOnceWith('vegan')
  })

  it('hides the clear button until at least one tag is selected', () => {
    renderFilter()

    expect(screen.queryByRole('button', { name: /clear all/i })).not.toBeInTheDocument()
  })

  it('shows the clear button when tags are selected and calls back on click', async () => {
    const user = userEvent.setup()
    const { onClear } = renderFilter({ selectedTags: ['vegan'] })

    await user.click(screen.getByRole('button', { name: /clear all/i }))

    expect(onClear).toHaveBeenCalledOnce()
  })

  it('summarises the whole catalogue when nothing is selected', () => {
    renderFilter({ matchCount: 10 })

    expect(screen.getByText('Showing the full catalogue')).toBeInTheDocument()
  })

  it('summarises how many products match the selected tags', () => {
    renderFilter({ selectedTags: ['vegan'], matchCount: 4 })

    expect(screen.getByText('4 products match every selected tag')).toBeInTheDocument()
  })

  it('uses singular wording for a single match', () => {
    renderFilter({ selectedTags: ['vegan', 'gluten_free'], matchCount: 1 })

    expect(screen.getByText('1 product matches every selected tag')).toBeInTheDocument()
  })
})