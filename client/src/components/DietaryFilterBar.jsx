import { DIETARY_TAGS } from '../constants/dietaryTags'

/**
 * Dietary tag filter (issue #32).
 *
 * Rendered as a wrapping row of toggle chips so the card stays short however
 * many tags exist. The tag list itself lives in dietaryTags.js.
 */

function DietaryFilterBar({ selectedTags, onToggleTag, onClear, matchCount }) {
  const hasSelection = selectedTags.length > 0
  const matchText = `${matchCount} ${matchCount === 1 ? 'product matches' : 'products match'}`

  return (
    <section className="filter-card">
      <div className="section-heading">
        <h2>Dietary</h2>
        {hasSelection ? (
          <button type="button" className="ghost-button subtle" onClick={onClear}>
            Clear all
          </button>
        ) : null}
      </div>

      <fieldset className="filter-chips">
        <legend className="visually-hidden">Filter products by dietary requirement</legend>

        {DIETARY_TAGS.map((tag) => (
          <label key={tag.value} className="filter-chip">
            <input
              type="checkbox"
              checked={selectedTags.includes(tag.value)}
              onChange={() => onToggleTag(tag.value)}
            />
            <span>{tag.label}</span>
          </label>
        ))}
      </fieldset>

      <p className="filter-summary">
        {hasSelection
          ? `${matchText} every selected tag`
          : 'Showing the full catalogue'}
      </p>
    </section>
  )
}

export default DietaryFilterBar