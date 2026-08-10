package nz.ac.auckland.grocerfy;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;

/**
 * Integration test for the basket comparison controller.
 *
 * This confirms that the backend accepts a JSON POST request to /api/basket/compare
 * and returns the expected response structure for seeded stores and products.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class BasketComparisonControllerTest {

	private final HttpClient httpClient = HttpClient.newHttpClient();

	@Value("${local.server.port}")
	private int port;

	@Test
	void comparesBasketAcrossStores() throws IOException, InterruptedException {
		// Create a basket comparison request using seeded product IDs.
		HttpRequest request = HttpRequest.newBuilder(URI.create("http://localhost:" + port + "/api/basket/compare"))
				.header("Content-Type", "application/json")
				.POST(HttpRequest.BodyPublishers.ofString("""
					{
					  "items": [
					    { "productId": 1, "quantity": 2 },
					    { "productId": 5, "quantity": 1 }
					  ]
					}
					"""))
				.build();

		HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

		assertTrue(response.statusCode() == 200, "Expected 200 response but got " + response.statusCode());

		ObjectMapper mapper = new ObjectMapper();
		JsonNode body = mapper.readTree(response.body());

		assertTrue(body.get("requestedItems").isArray(), "Response should include requestedItems array");
		assertTrue(body.get("stores").isArray(), "Response should include stores array");
		assertTrue(body.get("cheapestAvailableStore").get("available").asBoolean(), "Expected cheapest available store to be available");
		assertTrue(response.body().contains("PAK'nSAVE Mt Albert"), "Expected a seeded store to appear in the response");
	}
}
