import { ALLERGEN_TAGS, DIETARY_TAGS } from '../constants/dietaryTags'
import { MAX_STORE_DISTANCE_KM } from '../utils/distance'
import LocationButton from './LocationButton'

/**
 * Dietary tag filter (issue #32).
 *
 * Rendered as a wrapping row of toggle chips so the card stays short however
 * many tags exist. The tag list itself lives in dietaryTags.js.
 */

function ChipGroup({ legend, options, selected, onToggle }) {
  return (
    <fieldset className="filter-chips">
      <legend className="visually-hidden">{legend}</legend>
 
      {options.map((tag) => (
        <label key={tag.value} className="filter-chip">
          <input
            type="checkbox"
            checked={selected.includes(tag.value)}
            onChange={() => onToggle(tag.value)}
          />
          <span>{tag.label}</span>
        </label>
      ))}
    </fieldset>
  )
}

function DietaryFilterBar({
      selectedDietary=[],
      onToggleDietary,
      selectedAllergens=[],
      onToggleAllergen,
      onClear,
        matchCount,
        maxDistanceKm = MAX_STORE_DISTANCE_KM,
        onChangeMaxDistance,
        userLocation = null,
        locationLoading = false,
        locationError = '',
        onUseLocation }) {
  const hasDietary = selectedDietary.length > 0
  const hasAllergens = selectedAllergens.length > 0
      const hasDistanceLimit = maxDistanceKm < MAX_STORE_DISTANCE_KM
      const hasSelection = hasDietary || hasAllergens || hasDistanceLimit
  const matchText = `${matchCount} ${matchCount === 1 ? 'product matches' : 'products match'}`
 
  const criteria = []
  if (hasDietary) {
    criteria.push('every selected dietary tag')
  }
  if (hasAllergens) {
    criteria.push('your allergen exclusions')
  }

  return (
    <section className="filter-card">
      <div className="section-heading">
        <h2>Filters</h2>
        {hasSelection ? (
          <button type="button" className="ghost-button subtle" onClick={onClear}>
            Clear all
          </button>
        ) : null}
      </div>
 
      <h3>Dietary (incl.)</h3>
      <ChipGroup
        legend="Show only products with every selected dietary tag"
        options={DIETARY_TAGS}
        selected={selectedDietary}
        onToggle={onToggleDietary}
      />
 
      <h3>Allergens (excl.)</h3>
      <ChipGroup
        legend="Hide products containing any selected allergen"
        options={ALLERGEN_TAGS}
        selected={selectedAllergens}
        onToggle={onToggleAllergen}
      />

      <h3>Store distance</h3>
      <label className="distance-filter">
        <span>Within {maxDistanceKm} km</span>
        <input
          type="range"
          min="1"
          max={MAX_STORE_DISTANCE_KM}
          step="1"
          value={maxDistanceKm}
          disabled={!userLocation}
          aria-label="Maximum store distance"
          onChange={(event) => onChangeMaxDistance(Number(event.target.value))}
        />
      </label>
      <LocationButton
        userLocation={userLocation}
        loading={locationLoading}
        error={locationError}
        onUseLocation={onUseLocation}
      />
 
      <p className="filter-summary">
        {hasSelection
          ? `${matchText} ${criteria.join(' and ')}`
          : 'Showing the full catalogue'}
      </p>
    </section>
  )
}

export default DietaryFilterBar