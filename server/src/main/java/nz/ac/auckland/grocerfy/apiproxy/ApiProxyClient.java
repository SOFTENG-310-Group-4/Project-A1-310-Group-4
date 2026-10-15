package nz.ac.auckland.grocerfy.apiproxy;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.http.HttpResponse.BodyHandlers;
import java.time.Duration;
import org.springframework.stereotype.Component;

/** Small client for calling the configured chat-completions style API proxy. */
@Component
public class ApiProxyClient {

  private final ObjectMapper objectMapper = new ObjectMapper();
  private final HttpClient httpClient =
      HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(10)).build();
  private final ApiProxyConfig config;

  public ApiProxyClient(ApiProxyConfig config) {
    this.config = config;
  }

  /**
   * Sends a user prompt to the configured API proxy.
   *
   * @param request request containing the user prompt; must not be null
   * @return response containing the raw model content
   * @throws ApiProxyException if the network call fails, the API returns an error, or the response
   *     cannot be parsed
   */
  public ApiProxyResponse send(ApiProxyRequest request) {

    ObjectNode requestBody = objectMapper.createObjectNode();
    requestBody.put("model", config.getModel());

    ArrayNode messages = requestBody.putArray("messages");

    ObjectNode userMessage = messages.addObject();
    userMessage.put("role", "user");
    userMessage.put("content", request.userPrompt());

    HttpRequest httpRequest =
        HttpRequest.newBuilder()
            .uri(URI.create(config.getUrl()))
            .timeout(Duration.ofSeconds(45))
            .header("Content-Type", "application/json")
            .header("Authorization", "Bearer " + config.getApiKey())
            .POST(HttpRequest.BodyPublishers.ofString(requestBody.toString()))
            .build();

    HttpResponse<String> httpResponse;
    try {
      httpResponse = httpClient.send(httpRequest, BodyHandlers.ofString());
    } catch (IOException e) {
      throw new ApiProxyException("Network error calling API proxy: " + e.getMessage(), e);
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
      throw new ApiProxyException("Request interrupted: " + e.getMessage(), e);
    }

    if (httpResponse.statusCode() >= 300) {
      throw new ApiProxyException(
          "API proxy returned " + httpResponse.statusCode() + ": " + httpResponse.body());
    }

    try {
      JsonNode root = objectMapper.readTree(httpResponse.body());
      String content = root.path("choices").path(0).path("message").path("content").asText();
      return new ApiProxyResponse(content);
    } catch (Exception exception) {
      throw new ApiProxyException(
          "Failed to parse API proxy response: " + exception.getMessage(), exception);
    }
  }
}
