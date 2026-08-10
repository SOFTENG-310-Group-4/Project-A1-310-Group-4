/**
 * Simple placeholder UI for Grocerfy.
 *
 * This app currently focuses on backend basket comparison logic only.
 * The frontend is intentionally minimal and does not provide interactive
 * basket entry or comparison controls yet.
 */
function App() {
  return (
    <main className="shell">
      <section className="hero">
        <div>
          <p className="eyebrow">Grocerfy</p>
          <h1>Backend-only comparison ready</h1>
          <p className="lede">
            The core server-side basket comparison endpoint is operational.
          </p>
        </div>

        <div className="summary-card">
          <span className="summary-label">Backend status</span>
          <strong>Ready</strong>
          <span className="summary-copy">POST /api/basket/compare is the intended entry point.</span>
        </div>
      </section>
    </main>
  )
}

export default App
