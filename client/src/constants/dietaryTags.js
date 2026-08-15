/**
 * Dietary tags supported by GET /api/product 
 *
 * The API rejects unknown tag names with a 400, so these values live in one
 * place rather than being repeated at each call. Kept outside the
 * component file so React Fast Refresh keeps working there.
 */
export const DIETARY_TAGS = [
  { value: 'gluten_free', label: 'Gluten free' },
  { value: 'lactose_free', label: 'Lactose free' },
  { value: 'vegetarian', label: 'Vegetarian' },
  { value: 'vegan', label: 'Vegan' },
]