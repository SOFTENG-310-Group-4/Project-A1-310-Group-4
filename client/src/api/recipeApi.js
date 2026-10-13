/**
 * Requests a recipe suggestion from the backend based on the supplied basket.
 *
 * The endpoint returns plain text (not JSON), so the response is read as text.
 * Non-OK responses throw an Error carrying the server's message.
 *
 * @param {Object} payload - Basket payload sent to /api/recipes/suggest.
 * @returns {Promise<string>} Recipe text from the server.
 */
export async function suggestRecipes(payload) {
  const response = await fetch("/api/recipes/suggest", {
    method: "POST",
    headers: { "Content-Type": "application/json" },
    body: JSON.stringify(payload),
  });

  const text = await response.text();

  if (!response.ok) {
    throw new Error(text || "Unable to suggest recipes.");
  }

  return text;
}
