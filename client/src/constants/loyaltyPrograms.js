/**
 * Loyalty programmes with member prices, matching the backend's LoyaltyProgram enum
 * (sent to POST /api/basket/compare as `loyaltyPrograms`).
 *
 * Only Woolworths' Everyday Rewards has public per-product member prices; Club+
 * (New World / PAK'nSAVE) gives points and personalised deals instead, so it isn't listed.
 * Kept outside the component files so React Fast Refresh keeps working there.
 */
export const LOYALTY_PROGRAMS = [
  { value: 'EVERYDAY_REWARDS', label: 'Everyday Rewards', supermarket: 'Woolworths' },
]

/** Display name for a programme value, e.g. 'EVERYDAY_REWARDS' -> 'Everyday Rewards'. */
export function loyaltyProgramLabel(value) {
  const program = LOYALTY_PROGRAMS.find((candidate) => candidate.value === value)
  return program ? program.label : value
}
