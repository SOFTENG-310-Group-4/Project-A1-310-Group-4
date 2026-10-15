import { render, screen } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { describe, expect, it, vi } from 'vitest'

import LoyaltyToggle from '../components/LoyaltyToggle'

/**
 * Unit tests for the loyalty card toggles (issue #78).
 */
describe('LoyaltyToggle', () => {
  it('offers an Everyday Rewards card toggle', () => {
    render(<LoyaltyToggle selectedPrograms={[]} onToggleProgram={() => {}} />)

    expect(screen.getByRole('group', { name: /i have a loyalty card/i })).toBeInTheDocument()
    expect(screen.getByRole('checkbox', { name: 'Woolworths Everyday Rewards' })).not.toBeChecked()
  })

  it('shows a selected card as checked', () => {
    render(<LoyaltyToggle selectedPrograms={['EVERYDAY_REWARDS']} onToggleProgram={() => {}} />)

    expect(screen.getByRole('checkbox', { name: 'Woolworths Everyday Rewards' })).toBeChecked()
  })

  it('reports the programme value when toggled', async () => {
    const onToggleProgram = vi.fn()
    render(<LoyaltyToggle selectedPrograms={[]} onToggleProgram={onToggleProgram} />)

    await userEvent.setup().click(screen.getByRole('checkbox', { name: 'Woolworths Everyday Rewards' }))

    expect(onToggleProgram).toHaveBeenCalledWith('EVERYDAY_REWARDS')
  })
})
