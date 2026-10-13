package nz.ac.auckland.grocerfy.apiproxy;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

/** Loads prompt templates from the classpath. */
public class ApiProxyPromptLoader {

  private ApiProxyPromptLoader() {}

  /**
   * Reads a UTF-8 prompt template from the classpath.
   *
   * @param resourcePath classpath resource path, e.g. {@code /prompts/recipe-prompt.txt}
   * @return prompt template contents
   * @throws ApiProxyException if the resource is missing or cannot be read
   */
  public static String load(String resourcePath) {
    if (resourcePath == null || resourcePath.isBlank()) {
      throw new IllegalArgumentException("resourcePath must not be blank");
    }

    try (InputStream in = ApiProxyPromptLoader.class.getResourceAsStream(resourcePath)) {
      if (in == null) {
        throw new ApiProxyException(
            "Prompt template not found on classpath: "
                + resourcePath
                + " (expected under src/main/resources)");
      }
      return new String(in.readAllBytes(), StandardCharsets.UTF_8);
    } catch (IOException exception) {
      throw new ApiProxyException("Failed to read prompt template: " + resourcePath, exception);
    }
  }
}
