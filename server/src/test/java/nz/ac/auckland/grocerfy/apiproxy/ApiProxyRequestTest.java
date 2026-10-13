package nz.ac.auckland.grocerfy.apiproxy;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;

/**
 * Unit tests for the API proxy request record.
 *
 * <p>The record is a thin wrapper around a prompt string, so the only behaviour worth covering is
 * the compact constructor's validation.
 */
class ApiProxyRequestTest {

  @Test
  void storesTheUserPrompt() {
    ApiProxyRequest request = new ApiProxyRequest("Suggest a recipe.");

    assertThat(request.userPrompt()).isEqualTo("Suggest a recipe.");
  }

  @Test
  void rejectsANullPrompt() {
    assertThatThrownBy(() -> new ApiProxyRequest(null))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessage("userPrompt must not be blank.");
  }

  @Test
  void rejectsABlankPrompt() {
    assertThatThrownBy(() -> new ApiProxyRequest("   "))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessage("userPrompt must not be blank.");
  }

  @Test
  void rejectsAnEmptyPrompt() {
    assertThatThrownBy(() -> new ApiProxyRequest(""))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessage("userPrompt must not be blank.");
  }
}
