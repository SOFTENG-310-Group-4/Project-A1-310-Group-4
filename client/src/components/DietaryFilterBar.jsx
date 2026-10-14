import { ALLERGEN_TAGS, DIETARY_TAGS } from '../constants/dietaryTags'

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
      matchCount }) {
  const hasDietary = selectedDietary.length > 0
  const hasAllergens = selectedAllergens.length > 0
  const hasSelection = hasDietary || hasAllergens
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
 
      <p className="filter-summary">
        {hasSelection
          ? `${matchText} ${criteria.join(' and ')}`
          : 'Showing the full catalogue'}
      </p>
    </section>
  )
}

export default DietaryFilterBar