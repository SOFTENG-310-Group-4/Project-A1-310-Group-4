import { LOYALTY_PROGRAMS } from '../constants/loyaltyPrograms'

/**
 * Loyalty card toggles (issue #78).
 *
 * One chip per programme, styled like the dietary filter chips. When a card is
 * on, the comparison uses that programme's member prices.
 */
function LoyaltyToggle({ selectedPrograms, onToggleProgram }) {
  return (
    <fieldset className="filter-chips loyalty-toggle">
      <legend className="loyalty-legend">I have a loyalty card</legend>

      {LOYALTY_PROGRAMS.map((program) => (
        <label key={program.value} className="filter-chip">
          <input
            type="checkbox"
            checked={selectedPrograms.includes(program.value)}
            onChange={() => onToggleProgram(program.value)}
          />
          <span>{program.supermarket} {program.label}</span>
        </label>
      ))}
    </fieldset>
  )
}

export default LoyaltyToggle
