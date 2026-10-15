import { distanceKm, formatDistance } from '../utils/distance'

/**
 * Shows how far a store is from the user, e.g. "3.2 km away".
 * Renders nothing until the user has shared their location, or if the store has no coordinates.
 */
function StoreDistance({ userLocation, store }) {
  if (!userLocation || store.latitude == null || store.longitude == null) return null
  const km = distanceKm(userLocation, { latitude: store.latitude, longitude: store.longitude })
  return <span className="store-distance">{formatDistance(km)} away</span>
}

export default StoreDistance