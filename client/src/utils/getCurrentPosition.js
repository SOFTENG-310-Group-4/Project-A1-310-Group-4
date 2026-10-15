const DEFAULT_OPTIONS = { timeout: 10000 }

export function getCurrentPosition(options = {}) {
    return new Promise((resolve, reject) => {
        if (!navigator.geolocation) {
            reject(new Error('Geolocation is not supported by this browser'))
            return
        }

        navigator.geolocation.getCurrentPosition(
            ({ coords }) => resolve({ latitude: coords.latitude, longitude: coords.longitude}),
            reject,
            { ...DEFAULT_OPTIONS, ...options },
        )
    })
}
