import { fireEvent, render, screen } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { describe, expect, it, vi } from 'vitest'

import DietaryFilterBar from '../components/DietaryFilterBar'
import { DIETARY_TAGS, ALLERGEN_TAGS } from '../constants/dietaryTags'

/**
 * Unit tests for the dietary filter sidebar (issue #32).
 *
 * These cover the component in isolation: which chips render, which appear
 * checked, and what the callbacks receive. Whether the filter actually removes
 * products is covered in App.test.jsx.
 */
function renderFilter(overrides = {}) {
  const props = {
    selectedDietary: [],
    onToggleDietary: vi.fn(),
    selectedAllergens: [],
    onToggleAllergen: vi.fn(),
    onClear: vi.fn(),
    matchCount: 10,
    maxDistanceKm: 50,
    onChangeMaxDistance: vi.fn(),
    onUseLocation: vi.fn(),
    ...overrides,
  }

  render(<DietaryFilterBar {...props} />)
  return props
}

describe('DietaryFilterBar', () => {
  it('renders a checkbox for every supported dietary and allergen tag', () => {
    renderFilter()

    expect(screen.getAllByRole('checkbox')).toHaveLength(DIETARY_TAGS.length + ALLERGEN_TAGS.length)
    expect(screen.getByRole('checkbox', { name: 'Vegan' })).toBeInTheDocument()
    expect(screen.getByRole('checkbox', { name: 'Vegetarian' })).toBeInTheDocument()
    expect(screen.getByRole('checkbox', { name: 'Non-GMO' })).toBeInTheDocument()
    expect(screen.getByRole('checkbox', { name: 'Organic' })).toBeInTheDocument()

    expect(screen.getByRole('checkbox', { name: 'Gluten' })).toBeInTheDocument()
    expect(screen.getByRole('checkbox', { name: 'Wheat' })).toBeInTheDocument()
    expect(screen.getByRole('checkbox', { name: 'Dairy' })).toBeInTheDocument()
    expect(screen.getByRole('checkbox', { name: 'Egg' })).toBeInTheDocument()
    expect(screen.getByRole('checkbox', { name: 'Fish' })).toBeInTheDocument()
    expect(screen.getByRole('checkbox', { name: 'Shellfish' })).toBeInTheDocument()
    expect(screen.getByRole('checkbox', { name: 'Soy' })).toBeInTheDocument()
    expect(screen.getByRole('checkbox', { name: 'Tree Nuts' })).toBeInTheDocument()
    expect(screen.getByRole('checkbox', { name: 'Sesame' })).toBeInTheDocument()
    expect(screen.getByRole('checkbox', { name: 'Peanut' })).toBeInTheDocument()
  })

  it('checks only the tags that are currently selected', () => {
    renderFilter({ selectedDietary: ['VEGAN'], selectedAllergens: ['GLUTEN'] })

    expect(screen.getByRole('checkbox', { name: 'Vegan' })).toBeChecked()
    expect(screen.getByRole('checkbox', { name: 'Vegetarian' })).not.toBeChecked()
    expect(screen.getByRole('checkbox', { name: 'Non-GMO' })).not.toBeChecked()
    expect(screen.getByRole('checkbox', { name: 'Organic' })).not.toBeChecked()

    expect(screen.getByRole('checkbox', { name: 'Gluten' })).toBeChecked()
    expect(screen.getByRole('checkbox', { name: 'Wheat' })).not.toBeChecked()
    expect(screen.getByRole('checkbox', { name: 'Dairy' })).not.toBeChecked()
    expect(screen.getByRole('checkbox', { name: 'Egg' })).not.toBeChecked()
    expect(screen.getByRole('checkbox', { name: 'Fish' })).not.toBeChecked()
    expect(screen.getByRole('checkbox', { name: 'Shellfish' })).not.toBeChecked()
    expect(screen.getByRole('checkbox', { name: 'Soy' })).not.toBeChecked()
    expect(screen.getByRole('checkbox', { name: 'Tree Nuts' })).not.toBeChecked()
    expect(screen.getByRole('checkbox', { name: 'Sesame' })).not.toBeChecked()
    expect(screen.getByRole('checkbox', { name: 'Peanut' })).not.toBeChecked()
  })

  it('reports the dietary value, not the label, when a chip is clicked', async () => {
    const user = userEvent.setup()
    const { onToggleDietary } = renderFilter()

    await user.click(screen.getByRole('checkbox', { name: 'Vegetarian' }))

    // The API rejects unknown tag names with a 400, so the exact value matters.
    expect(onToggleDietary).toHaveBeenCalledExactlyOnceWith('VEGETARIAN')
  })

  it('reports the same tag when an already selected chip is clicked again', async () => {
    const user = userEvent.setup()
    const { onToggleDietary } = renderFilter({ selectedDietary: ['VEGAN'] })

    await user.click(screen.getByRole('checkbox', { name: 'Vegan' }))

    expect(onToggleDietary).toHaveBeenCalledExactlyOnceWith('VEGAN')
  })

  it('hides the clear button until at least one tag is selected', () => {
    renderFilter()

    expect(screen.queryByRole('button', { name: /clear all/i })).not.toBeInTheDocument()
  })

  it('shows the clear button when tags are selected and calls back on click', async () => {
    const user = userEvent.setup()
    const { onClear } = renderFilter({ selectedDietary: ['VEGAN'] })

    await user.click(screen.getByRole('button', { name: /clear all/i }))

    expect(onClear).toHaveBeenCalledOnce()
  })

  it('summarises the whole catalogue when nothing is selected', () => {
    renderFilter({ matchCount: 10 })

    expect(screen.getByText('Showing the full catalogue')).toBeInTheDocument()
  })

  it('summarises how many products match the dietary tags', () => {
    renderFilter({ selectedDietary: ['VEGAN'], matchCount: 4 })

    expect(screen.getByText('4 products match every selected dietary tag')).toBeInTheDocument()
  })

  it('summarises how many products match the allergen exclusions', () => {
    renderFilter({ selectedAllergens: ['GLUTEN'], matchCount: 4 })
 
    expect(screen.getByText('4 products match your allergen exclusions')).toBeInTheDocument()
  })
 
  it('summarises dietary tags and allergen exclusions together', () => {
    renderFilter({ selectedDietary: ['VEGAN'], selectedAllergens: ['GLUTEN'], matchCount: 4 })
 
    expect(
      screen.getByText('4 products match every selected dietary tag and your allergen exclusions'),
    ).toBeInTheDocument()
  })

  it('uses singular wording for a single match', () => {
    renderFilter({ selectedDietary: ['VEGAN', 'NONGMO'], matchCount: 1 })

    expect(screen.getByText('1 product matches every selected dietary tag')).toBeInTheDocument()
  })

  it('disables the distance slider until a location is available', () => {
    renderFilter()

    expect(screen.getByRole('slider', { name: 'Maximum store distance' })).toBeDisabled()
    expect(screen.getByRole('button', { name: 'Use my location' })).toBeInTheDocument()
  })

  it('reports the selected distance when the slider changes', async () => {
    const { onChangeMaxDistance } = renderFilter({
      userLocation: { latitude: -36.85, longitude: 174.76 },
    })
    const slider = screen.getByRole('slider', { name: 'Maximum store distance' })

    fireEvent.change(slider, { target: { value: '15' } })

    expect(onChangeMaxDistance).toHaveBeenCalledExactlyOnceWith(15)
  })
})