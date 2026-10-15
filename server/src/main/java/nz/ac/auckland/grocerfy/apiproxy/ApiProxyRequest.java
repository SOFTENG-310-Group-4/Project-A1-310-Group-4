package nz.ac.auckland.grocerfy.apiproxy;

/**
 * Request sent to the API proxy.
 *
 * @param userPrompt user prompt; must not be blank
 */
public record ApiProxyRequest(String userPrompt) {

  public ApiProxyRequest {
    if (userPrompt == null || userPrompt.isBlank()) {
      throw new IllegalArgumentException("userPrompt must not be blank.");
    }
  }
}
