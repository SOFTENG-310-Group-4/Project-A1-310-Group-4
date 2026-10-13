/**
 * RecipeSuggestions panel — renders a button that sends the current cart lines
 * and dietary tags to the recipe API, then shows the returned recipe text (or an
 * error). Local state tracks the text, loading flag, and error message; empty
 * product IDs are filtered out before the request is sent.
 */
import { useState } from "react";
import { suggestRecipes } from "../api/recipeApi";

function RecipeSuggestions({ cartLines, dietaryTags }) {
  const [text, setText] = useState("");
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState("");

  async function handleSuggest() {
    setError("");
    setText("");
    setLoading(true);

    try {
      const validLines = cartLines.filter((line) => line.productId !== "");

      const result = await suggestRecipes({
        items: validLines.map((line) => ({
          productId: Number(line.productId),
          quantity: Number(line.quantity),
        })),
        dietaryTags,
      });

      setText(result);
    } catch (err) {
      setError(err.message);
    } finally {
      setLoading(false);
    }
  }

  return (
    <section className="panel">
      <div className="section-heading">
        <h2>Recipe idea</h2>
        <button
          className="primary-button"
          onClick={handleSuggest}
          disabled={loading || cartLines.length === 0}
        >
          {loading ? "Thinking…" : "Suggest recipe"}
        </button>
      </div>

      {error ? <p className="error-banner">{error}</p> : null}

      {text ? (
        <pre
          style={{
            marginTop: 16,
            whiteSpace: "pre-wrap",
            fontFamily: "inherit",
            background: "rgba(255,255,255,0.6)",
            padding: 16,
            borderRadius: 16,
          }}
        >
          {text}
        </pre>
      ) : null}
    </section>
  );
}

export default RecipeSuggestions;