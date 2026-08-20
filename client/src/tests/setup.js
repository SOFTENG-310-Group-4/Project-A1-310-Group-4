import '@testing-library/jest-dom/vitest'
import { cleanup } from '@testing-library/react'
import { beforeEach, afterEach } from 'vitest'

// React Testing Library does not unmount automatically between tests, so state
// from one test would otherwise leak into the next.
beforeEach(() => {
  localStorage.clear()
})

afterEach(() => {
  cleanup()
})