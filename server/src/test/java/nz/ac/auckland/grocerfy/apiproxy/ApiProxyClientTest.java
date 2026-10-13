package nz.ac.auckland.grocerfy.apiproxy;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.io.IOException;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import okhttp3.mockwebserver.RecordedRequest;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Unit tests for the API proxy client.
 *
 * <p>A real MockWebServer stands in for the upstream API, so the client is exercised end-to-end:
 * request serialisation, header construction, status handling, and response parsing. Errors from
 * the server and from the network are both asserted to surface as ApiProxyException with the
 * relevant detail.
 */
class ApiProxyClientTest {

  private MockWebServer server;
  private ApiProxyClient client;

  @BeforeEach
  void setUp() throws IOException {
    server = new MockWebServer();
    server.start();

    ApiProxyConfig config = new ApiProxyConfig();
    config.setUrl(server.url("/v1/chat/completions").toString());
    config.setApiKey("test-key");
    config.setModel("test-model");

    client = new ApiProxyClient(config);
  }

  @AfterEach
  void tearDown() throws IOException {
    server.shutdown();
  }

  @Test
  void postsTheRequestAndParsesTheModelContent() throws InterruptedException {
    server.enqueue(
        new MockResponse()
            .setResponseCode(200)
            .setHeader("Content-Type", "application/json")
            .setBody(
                """
                {
                  "choices": [
                    { "message": { "role": "assistant", "content": "Tomato pasta." } }
                  ]
                }
                """));

    ApiProxyRequest request = new ApiProxyRequest("Suggest a recipe.");

    ApiProxyResponse response = client.send(request);

    assertThat(response.content()).isEqualTo("Tomato pasta.");

    RecordedRequest recorded = server.takeRequest();
    assertThat(recorded.getMethod()).isEqualTo("POST");
    assertThat(recorded.getPath()).isEqualTo("/v1/chat/completions");
    assertThat(recorded.getHeader("Content-Type")).isEqualTo("application/json");
    assertThat(recorded.getHeader("Authorization")).isEqualTo("Bearer test-key");
    assertThat(recorded.getBody().readUtf8())
        .contains("\"model\":\"test-model\"")
        .contains("\"role\":\"user\"")
        .contains("\"content\":\"Suggest a recipe.\"");
  }

  @Test
  void throwsWithTheStatusAndBodyWhenTheServerErrors() {
    server.enqueue(
        new MockResponse()
            .setResponseCode(401)
            .setHeader("Content-Type", "application/json")
            .setBody("{\"error\":{\"message\":\"Invalid API key\"}}"));

    ApiProxyRequest request = new ApiProxyRequest("Suggest a recipe.");

    assertThatThrownBy(() -> client.send(request))
        .isInstanceOf(ApiProxyException.class)
        .hasMessageContaining("401")
        .hasMessageContaining("Invalid API key");
  }

  @Test
  void throwsWhenTheResponseBodyIsNotJson() {
    server.enqueue(new MockResponse().setResponseCode(200).setBody("not json"));

    ApiProxyRequest request = new ApiProxyRequest("Suggest a recipe.");

    assertThatThrownBy(() -> client.send(request))
        .isInstanceOf(ApiProxyException.class)
        .hasMessageContaining("Failed to parse API proxy response");
  }

  @Test
  void returnsAnEmptyContentWhenTheResponseIsMissingTheContentField() {
    server.enqueue(
        new MockResponse()
            .setResponseCode(200)
            .setHeader("Content-Type", "application/json")
            .setBody("{\"choices\":[]}"));

    ApiProxyRequest request = new ApiProxyRequest("Suggest a recipe.");

    ApiProxyResponse response = client.send(request);

    assertThat(response.content()).isEmpty();
  }

  @Test
  void throwsANetworkErrorWhenTheServerIsUnreachable() throws IOException {
    server.shutdown();

    ApiProxyRequest request = new ApiProxyRequest("Suggest a recipe.");

    assertThatThrownBy(() -> client.send(request))
        .isInstanceOf(ApiProxyException.class)
        .hasMessageContaining("Network error calling API proxy");
  }
}
