function LocationButton({ userLocation, loading, error, onUseLocation }) {
  return (
    <div className="location-control">
      <button type="button" className="primary-button" onClick={onUseLocation} disabled={loading}>
        {loading ? 'Finding you…' : userLocation ? 'Update my location' : 'Use my location'}
      </button>
      {userLocation ? <span> Distances are shown on each store.</span> : null}
      {error ? <p role="alert">{error}</p> : null}
    </div>
  )
}

export default LocationButton