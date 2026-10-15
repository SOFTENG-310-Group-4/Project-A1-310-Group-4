import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest';

import { getCurrentPosition } from '../utils/getCurrentPosition';

const geoMock = vi.fn()

beforeEach(() => {
	vi.stubGlobal('navigator', {
		...navigator,
		geolocation: { getCurrentPosition: geoMock },
	})
})

afterEach(() => {
	geoMock.mockReset()
	vi.unstubAllGlobals()
})

describe('getCurrentPosition', () => {
	it('resolves with latitude and longitude and applies the default timeout', async () => {
		geoMock.mockImplementation((success) =>
			success({ coords: { latitude: -36.8485, longitude: 174.7633 } }),
		)

		const result = await getCurrentPosition()

		expect(result).toEqual({ latitude: -36.8485, longitude: 174.7633 })
		expect(geoMock).toHaveBeenCalledWith(
			expect.any(Function),
			expect.any(Function),
			{ timeout: 10000 },
		)
	})

	it('rejects when permission is denied', async () => {
		const denied = { code: 1, message: 'User denied Geolocation' }
		geoMock.mockImplementation((_success, error) => error(denied))

		await expect(getCurrentPosition()).rejects.toEqual(denied)
	})

	it('rejects when the lookup times out', async () => {
		const timedOut = { code: 3, message: 'Timeout expired' }
		geoMock.mockImplementation((_success, error) => error(timedOut))

		await expect(getCurrentPosition()).rejects.toEqual(timedOut)
	})

	it('lets callers override the default options', async () => {
		geoMock.mockImplementation((success) =>
			success({ coords: { latitude: 1, longitude: 2 } }),
		)

		await getCurrentPosition({ timeout: 5000, enableHighAccuracy: true })

		expect(geoMock).toHaveBeenCalledWith(
			expect.any(Function),
			expect.any(Function),
			{ timeout: 5000, enableHighAccuracy: true },
		)
	})

	it('rejects when geolocation is not supported', async () => {
		vi.stubGlobal('navigator', {})

		await expect(getCurrentPosition()).rejects.toThrow(/not supported/i)
	})
})