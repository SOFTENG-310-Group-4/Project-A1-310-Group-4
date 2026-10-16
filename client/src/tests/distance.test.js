import { describe, expect, it } from 'vitest'
import { distanceKm, formatDistance } from '../utils/distance'

const albany = { latitude: -36.7301, longitude: 174.7065 }
const royalOak = { latitude: -36.91, longitude: 174.7746 }

describe('distanceKm', () => {
  it('is zero for the same point', () => {
    expect(distanceKm(albany, albany)).toBe(0)
  })

  it('gives roughly 21 km between Albany and Royal Oak', () => {
    const km = distanceKm(albany, royalOak)
    expect(km).toBeGreaterThan(20)
    expect(km).toBeLessThan(22)
  })

  it('is the same in both directions', () => {
    expect(distanceKm(albany, royalOak)).toBeCloseTo(distanceKm(royalOak, albany), 10)
  })
})

describe('formatDistance', () => {
  it('uses metres under 1 km and km otherwise', () => {
    expect(formatDistance(0.43)).toBe('430 m')
    expect(formatDistance(3.24)).toBe('3.2 km')
  })
})