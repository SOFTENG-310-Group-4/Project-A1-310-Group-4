function buttonLabel(loading, userLocation) {
  if (loading) return 'Finding you…'
  return userLocation ? 'Update my location' : 'Use my location'
}

function LocationButton({ userLocation, loading, error, onUseLocation }) {
  return (
    <div className="location-control">
      <button type="button" className="ghost-button" onClick={onUseLocation} disabled={loading}>
        {buttonLabel(loading, userLocation)}
      </button>
      {userLocation ? <span> Distances are shown on each store.</span> : null}
      {error ? <p role="alert">{error}</p> : null}
    </div>
  )
}

export default LocationButton