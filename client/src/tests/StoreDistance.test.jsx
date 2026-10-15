import { render, screen } from '@testing-library/react'
import { describe, expect, it } from 'vitest'

import StoreDistance from '../components/StoreDistance'

const user = { latitude: -36.7301, longitude: 174.7065 } // Albany
const royalOak = { latitude: -36.91, longitude: 174.7746 }

describe('StoreDistance', () => {
  it('shows the distance when the user location and store coordinates are known', () => {
    render(<StoreDistance userLocation={user} store={royalOak} />)

    expect(screen.getByText(/2\d\.\d km away/)).toBeInTheDocument()
  })

  it('shows nothing before the user shares their location', () => {
    const { container } = render(<StoreDistance userLocation={null} store={royalOak} />)

    expect(container).toBeEmptyDOMElement()
  })

  it('shows nothing for a store without coordinates', () => {
    const { container } = render(
      <StoreDistance userLocation={user} store={{ latitude: null, longitude: null }} />,
    )

    expect(container).toBeEmptyDOMElement()
  })
})