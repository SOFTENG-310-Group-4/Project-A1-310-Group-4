/**
 * Dietary tags supported by GET /api/product 
 *
 * The API rejects unknown tag names with a 400, so these values live in one
 * place rather than being repeated at each call. Kept outside the
 * component file so React Fast Refresh keeps working there.
 */

/**
 * Filter options supported by GET /api/product.
 *
 * Values are the backend enum constant names (the Dietary and Allergen enums in
 * nz.ac.auckland.grocerfy.model), which is what the API binds from the query string and what it
 * returns on each product, so they must match those enums exactly. The API rejects unknown
 * names with a 400. Kept outside the component file so React Fast Refresh keeps working there.
 *
 * Dietary tags are "must have": a product has to carry every selected one.
 * Allergens are "must avoid": products containing any selected allergen are hidden.
 */
export const DIETARY_TAGS = [
  { value: 'VEGETARIAN', label: 'Vegetarian' },
  { value: 'VEGAN', label: 'Vegan' },
  { value: 'ORGANIC', label: 'Organic' },
  { value: 'NONGMO', label: 'Non-GMO' },
]
 
export const ALLERGEN_TAGS = [
  { value: 'GLUTEN', label: 'Gluten' },
  { value: 'WHEAT', label: 'Wheat' },
  { value: 'DAIRY', label: 'Dairy' },
  { value: 'EGG', label: 'Egg' },
  { value: 'FISH', label: 'Fish' },
  { value: 'SHELLFISH', label: 'Shellfish' },
  { value: 'SOY', label: 'Soy' },
  { value: 'TREENUT', label: 'Tree Nuts' },
  { value: 'SESAME', label: 'Sesame' },
  { value: 'PEANUT', label: 'Peanut' }
]
 