import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest';

import { getCurrentPosition } from '../utils/getCurrentPosition';

const geoMock = vi.fn();

beforeEach(() => {
  vi.stubGlobal('navigator', {
    ...navigator,
    geolocation: { getCurrentPosition: geoMock },
  });
});

afterEach(() => {
  geoMock.mockReset();
  vi.unstubAllGlobals();
});

describe('getCurrentPosition', () => {
  it('resolves with latitude and longitude on success', async () => {
    geoMock.mockImplementation((success) =>
      success({ coords: { latitude: -36.8485, longitude: 174.7633 } })
    );

    const result = await getCurrentPosition();

    expect(geoMock).toHaveBeenCalledTimes(1);
    expect(result).toEqual({ latitude: -36.8485, longitude: 174.7633 });
  });

  it('rejects when permission is denied', async () => {
    const denied = { code: 1, message: 'User denied Geolocation' };
    geoMock.mockImplementation((_success, error) => error(denied));

    await expect(getCurrentPosition()).rejects.toEqual(denied);
    expect(geoMock).toHaveBeenCalledTimes(1);
  });
});