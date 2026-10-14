package nz.ac.auckland.grocerfy.apiproxy;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.io.IOException;
import java.io.InputStream;
import org.junit.jupiter.api.Test;

/**
 * Unit tests for the prompt template loader.
 *
 * <p>The loader reads from the classpath, so the tests exercise both the happy path (a template
 * that exists) and the two failure modes callers will hit: a missing file and an invalid argument.
 */
class ApiProxyPromptLoaderTest {

  @Test
  void loadsTheRecipePromptTemplate() {
    String template = ApiProxyPromptLoader.load("/prompts/recipe-prompt.txt");

    assertThat(template).isNotBlank().contains("{{ingredients}}").contains("{{dietary}}");
  }

  @Test
  void failsClearlyWhenTheTemplateIsMissing() {
    assertThatThrownBy(() -> ApiProxyPromptLoader.load("/prompts/does-not-exist.txt"))
        .isInstanceOf(ApiProxyException.class)
        .hasMessageContaining("not found on classpath");
  }

  @Test
  void rejectsANullPath() {
    assertThatThrownBy(() -> ApiProxyPromptLoader.load(null))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("must not be blank");
  }

  @Test
  void rejectsABlankPath() {
    assertThatThrownBy(() -> ApiProxyPromptLoader.load("  "))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("must not be blank");
  }

  @Test
  void wrapsIoExceptionWhenTheStreamCannotBeRead() {
    InputStream broken =
        new InputStream() {
          @Override
          public int read() throws IOException {
            throw new IOException("disk failure");
          }
        };

    assertThatThrownBy(() -> ApiProxyPromptLoader.readStream(broken, "/prompts/whatever.txt"))
        .isInstanceOf(ApiProxyException.class)
        .hasMessageContaining("Failed to read prompt template")
        .hasRootCauseInstanceOf(IOException.class)
        .hasRootCauseMessage("disk failure");
  }
}
