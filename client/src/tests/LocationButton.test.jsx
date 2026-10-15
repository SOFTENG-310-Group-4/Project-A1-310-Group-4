  import { render, screen } from '@testing-library/react'
  import userEvent from '@testing-library/user-event'
  import { describe, expect, it, vi } from 'vitest'
  import LocationButton from '../components/LocationButton'

  describe('LocationButton', () => {
    it('calls onUseLocation when clicked', async () => {
      const onUseLocation = vi.fn()
      render(<LocationButton userLocation={null} loading={false} error="" onUseLocation={onUseLocation} />)
      await userEvent.click(screen.getByRole('button', { name: 'Use my location' }))
      expect(onUseLocation).toHaveBeenCalledOnce()
    })

    it('is disabled while loading', () => {
      render(<LocationButton userLocation={null} loading error="" onUseLocation={() => {}} />)
      expect(screen.getByRole('button')).toBeDisabled()
    })

    it('shows an error message', () => {
      render(<LocationButton userLocation={null} loading={false} error="Permission denied" onUseLocation={() => {}} />)
      expect(screen.getByRole('alert')).toHaveTextContent('Permission denied')
    })
  })