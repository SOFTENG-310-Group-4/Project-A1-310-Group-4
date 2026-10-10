package nz.ac.auckland.grocerfy.scraper;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.http.HttpResponse.BodyHandler;
import java.net.http.HttpResponse.BodyHandlers;
import java.nio.charset.StandardCharsets;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;

import nz.ac.auckland.grocerfy.dto.ProductInfo;
import nz.ac.auckland.grocerfy.model.Allergen;
import nz.ac.auckland.grocerfy.model.Dietary;

@DataJpaTest 
public class FoodstuffsScraperTest {

    private static final String STORE = "https://www.teststore.co.nz";
	private static final String API = "https://api-prod.teststore.co.nz/v1/edge";
 
	/**
	 * FoodstuffsScraper but send() hands back queued canned responses 
     * instead of real network responses. A private class to ensure not used
     * by anyone else (security).
	 */
	private static class StubbedScraper extends FoodstuffsScraper {
		private final Deque<Optional<HttpResponse<?>>> queuedResponses = new ArrayDeque<>();
		private final List<HttpRequest> sentRequests = new ArrayList<>();
 
		StubbedScraper() {
			super("teststore.co.nz", "ts");
		}
 
		void enqueue(Optional<HttpResponse<?>> response) {
			queuedResponses.add(response);
		}
 
		@Override
		@SuppressWarnings("unchecked")
		protected <T> Optional<HttpResponse<T>> send(HttpRequest request, BodyHandler<T> handler) {
			sentRequests.add(request);
			Optional<HttpResponse<?>> next = queuedResponses.poll();
			if (next == null) {
				throw new AssertionError("Unexpected extra request to " + request.uri());
			}
			return (Optional<HttpResponse<T>>) (Optional<?>) next;
		}
	}

    @SuppressWarnings("unchecked")
	private static Optional<HttpResponse<?>> response(int status, String body) {
		HttpResponse<String> response = mock(HttpResponse.class);
		when(response.statusCode()).thenReturn(status);
		when(response.body()).thenReturn(body);
		return Optional.<HttpResponse<?>>of(response);
	}
 
    /**
     * Returns an HttpResponse with a defined 200 code and a json with an access_token field.
     * @param token The auth token
     * @return An HttpResponse containing an auth token
     */
	private static Optional<HttpResponse<?>> tokenResponse(String token) {
		return response(200, "{\"access_token\":\"" + token + "\"}");
	}
 
    /**
     * Batched command for filler content page on the store.
     * @return HttpRequest to store category page
     */
	private static HttpRequest pageRequest() {
		return HttpRequest.newBuilder().uri(URI.create(STORE + "/shop/category/featured")).GET().build();
	}

    private StubbedScraper scraper;
 
	@BeforeEach
	void setUp() {
		scraper = new StubbedScraper();
	}
    
    @Test
	void setupCookiesVisitsHomepageThenFetchesToken() {
        // homepage - should return 200
		scraper.enqueue(response(200, null));

		scraper.enqueue(tokenResponse("test-cookie"));
		scraper.setupCookies();
 
		assertEquals(STORE, scraper.sentRequests.get(0).uri().toString());
		assertEquals("test-cookie", scraper.authToken);
	}
 
	@Test
	void refreshCookiesStoresAccessToken() {
		scraper.enqueue(tokenResponse("test-cookie"));
 
		scraper.refreshCookies();
 
		assertEquals("test-cookie", scraper.authToken);
		HttpRequest sent = scraper.sentRequests.get(0);
        // affirms request is sent as post and to the same link
		assertEquals("POST", sent.method());
		assertEquals(STORE + "/api/user/get-current-user", sent.uri().toString());
	}
 
	@Test
	void refreshCookiesThrowsWhenResponseHasNoToken() {
		scraper.enqueue(response(200, "{\"error\":\"blocked\"}"));
 
		assertThrows(IllegalStateException.class, scraper::refreshCookies);
	}
 
	@Test
	void sendRequestWithAuthRefreshesTokenAndRetriesOn401() {
		scraper.enqueue(response(401, "denied"));
		scraper.enqueue(tokenResponse("test-cookie"));
		scraper.enqueue(response(200, "allowed"));
 
		HttpResponse<String> result = scraper.sendRequestWithAuth(pageRequest(), BodyHandlers.ofString());
 
		assertEquals(200, result.statusCode());
		assertEquals(3, scraper.sentRequests.size());
		// request 2 is the token refresh, request 3 is the retry carrying the new token
		assertEquals(STORE + "/api/user/get-current-user", scraper.sentRequests.get(1).uri().toString());
		HttpRequest retry = scraper.sentRequests.get(2);
		assertEquals(pageRequest().uri(), retry.uri());
		assertEquals(Optional.of("Bearer test-cookie"), retry.headers().firstValue("Authorization"));
	}
 
	@Test
	void sendRequestWithAuthThrowsIfStillRejectedAfterRefresh() {
		scraper.enqueue(response(401, "denied"));
		scraper.enqueue(tokenResponse("fresh"));
		scraper.enqueue(response(401, "denied again"));
 
		IllegalStateException thrown = assertThrows(IllegalStateException.class,
				() -> scraper.sendRequestWithAuth(pageRequest(), BodyHandlers.ofString()));
 
		assertTrue(thrown.getMessage().contains("does not return after refreshing"));
	}
 
	@Test
	void changeStorePostsToRegionEndpointWithBearerToken() {
		scraper.authToken = "test-cookie";
		scraper.enqueue(response(200, null));
 
		assertTrue(scraper.changeStore("store-uuid"));
 
		HttpRequest sent = scraper.sentRequests.get(0);
		assertEquals("POST", sent.method());
		assertEquals(API + "/cart/store/store-uuid", sent.uri().toString());
		assertEquals(Optional.of("Bearer test-cookie"), sent.headers().firstValue("Authorization"));
	}
 
	@Test
	void changeStoreReturnsFalseWhenNoResponse() {
		scraper.authToken = "test-cookie";
		scraper.enqueue(Optional.empty());
 
		assertFalse(scraper.changeStore("store-uuid"));
	}
 
	@Test
	void changeStoreReturnsFalseWhenServerRejectsRequest() {
		scraper.authToken = "test-cookie";
		scraper.enqueue(response(403, null));
 
		assertFalse(scraper.changeStore("store-uuid"));
	}
 
	@Test
	void getProductInfoRequestsStoreSpecificEndpointAndReadsCategories() {
		scraper.authToken = "test-cookie";
		scraper.enqueue(response(200, "{\"categories\": [\"Fruit\"]}"));
 
		var result = scraper.getProductInfo("5252611-EA-000", "store-uuid");
 
		assertEquals(API + "/store/store-uuid/product/5252611-EA-000", scraper.sentRequests.get(0).uri().toString());
		assertTrue(result.getSecond().containsAll(Set.of(Dietary.VEGAN, Dietary.VEGETARIAN)));
	}
 
	@Test
	void eggsCategoryFlagsEggAllergen() {
		scraper.authToken = "test-cookie";
		scraper.enqueue(response(200, "{\"categories\": [\"Eggs\"]}"));
 
		var result = scraper.getProductInfo("5000001-EA-000", "store-uuid");
 
		assertThat(result.getFirst()).containsExactly(Allergen.EGG);
	}

    @Test
	void allergenIngredientFlagsAllergenAdd() {
		scraper.authToken = "test-cookie";
		scraper.enqueue(response(200, "{\"ingredientStatement\": \"milk, apple concentrate, emulsifier(500), flour\"}"));
 
		var result = scraper.getProductInfo("5000001-EA-000", "store-uuid");
 
        assertThat(result.getFirst()).containsExactly(Allergen.DAIRY);
	}

    @Test
	void allergenStatementFlagsAllergenAdd() {
		scraper.authToken = "test-cookie";
		scraper.enqueue(response(200, "{\"allergenStatement\": \"May contain milk, gluten, soy. Traces of shellfish.\"}"));
 
		var result = scraper.getProductInfo("5000001-EA-000", "store-uuid");
 
        assertThat(result.getFirst()).containsExactlyInAnyOrder(
            Allergen.DAIRY,
            Allergen.SHELLFISH,
            Allergen.GLUTEN,
            Allergen.SOY
        );
	}

    @Test
	void facetInfoAddingDietary() {
		scraper.authToken = "test-cookie";
		scraper.enqueue(response(200, 
            """
			{
				"facets": [
					{
						"itemCode": "00020007",
						"itemDescription": "Dairy Free"
					},
					{
						"itemCode": "00020012",
						"itemDescription": "Non-GMO"
					}
				]
			}
            """));
 
		var result = scraper.getProductInfo("5000001-EA-000", "store-uuid");
 
        assertThat(result.getSecond()).containsExactly(Dietary.NONGMO);
	}

	@Test 
	void extractProductDataGetsAllProducts() {
		Document doc;
		try (InputStream in = FoodstuffsScraperTest.class.getResourceAsStream("/test_dom.txt")) {
			doc = Jsoup.parse(new String(in.readAllBytes(), StandardCharsets.UTF_8));
		} catch (IOException exc) {
			throw new IllegalStateException("test file test_dom.txt triggered IO error");
		}

		List<ProductInfo> products = scraper.extractProducts(doc);
		assertEquals(50, products.size());
		// first product should be butter
		assertEquals("Pams Pure Butter", products.get(0).productName());
	}
}
