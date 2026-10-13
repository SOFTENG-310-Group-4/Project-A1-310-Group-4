package nz.ac.auckland.grocerfy.scraper;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.io.InputStream;
import java.math.BigDecimal;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpHeaders;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.http.HttpResponse.BodyHandler;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.Flow;

import javax.net.ssl.SSLSession;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.data.util.Pair;

import nz.ac.auckland.grocerfy.dto.ProductInfo;
import nz.ac.auckland.grocerfy.model.Allergen;
import nz.ac.auckland.grocerfy.model.Dietary;

/**
 * Unit tests for WoolworthsScraper. The network is replaced by queued canned responses; the fixtures
 * in src/test/resources/woolworths are trimmed copies of real woolworths.co.nz responses (no cookies or tokens).
 */
public class WoolworthsScraperTest {

	private static final String GRAPHQL = "https://www.woolworths.co.nz/api/graphql?op-name=";
	private static final String CATEGORY = "9-75C37855"; // Meat & Poultry

	/**
	 * WoolworthsScraper with send() answering from a queue, a fake clock and a counted session reset.
	 */
	private static class StubbedScraper extends WoolworthsScraper {
		private final Deque<Optional<HttpResponse<String>>> queuedResponses = new ArrayDeque<>();
		private final List<HttpRequest> sentRequests = new ArrayList<>();
		private int sessionClears = 0;
		private Instant clock = Instant.parse("2026-10-13T06:00:00Z");

		void enqueue(int status, String body) {
			queuedResponses.add(Optional.of(new FakeResponse(status, body, Map.of())));
		}

		void enqueueWithCookie(int status, String body, String setCookie) {
			queuedResponses.add(Optional.of(new FakeResponse(status, body, Map.of("Set-Cookie", List.of(setCookie)))));
		}

		void enqueueNoResponse() {
			queuedResponses.add(Optional.empty());
		}

		@Override
		@SuppressWarnings("unchecked")
		protected <T> Optional<HttpResponse<T>> send(HttpRequest request, BodyHandler<T> handler) {
			sentRequests.add(request);
			Optional<HttpResponse<String>> next = queuedResponses.poll();
			if (next == null) {
				throw new AssertionError("Unexpected extra request to " + request.uri());
			}
			return (Optional<HttpResponse<T>>) (Optional<?>) next;
		}

		@Override
		protected void clearSession() {
			super.clearSession();
			sessionClears++;
		}

		@Override
		protected Instant now() {
			return clock;
		}
	}

	/** Minimal HttpResponse carrying a status code, a body and optional headers. */
	private static class FakeResponse implements HttpResponse<String> {
		private final int status;
		private final String body;
		private final Map<String, List<String>> headers;

		FakeResponse(int status, String body, Map<String, List<String>> headers) {
			this.status = status;
			this.body = body;
			this.headers = headers;
		}

		@Override public int statusCode() { return status; }
		@Override public String body() { return body; }
		@Override public HttpRequest request() { return null; }
		@Override public Optional<HttpResponse<String>> previousResponse() { return Optional.empty(); }
		@Override public HttpHeaders headers() { return HttpHeaders.of(headers, (name, value) -> true); }
		@Override public Optional<SSLSession> sslSession() { return Optional.empty(); }
		@Override public URI uri() { return null; }
		@Override public HttpClient.Version version() { return HttpClient.Version.HTTP_1_1; }
	}

	private StubbedScraper scraper;

	@BeforeEach
	void setUp() {
		scraper = new StubbedScraper();
		scraper.setupCookies();
	}

	// ------------------------------------------------------------------ helpers

	private static String resource(String path) {
		try (InputStream in = WoolworthsScraperTest.class.getResourceAsStream(path)) {
			return new String(in.readAllBytes(), StandardCharsets.UTF_8);
		} catch (IOException | NullPointerException exc) {
			throw new IllegalStateException("test resource " + path + " missing");
		}
	}

	private static String storeEcho(String pickupId) {
		return "{\"data\":{\"setCartShoppingMode\":{\"shoppingMode\":{\"mode\":\"Pickup\",\"pickupLocationId\":\""
			+ pickupId + "\",\"pickupLocation\":{\"name\":\"Test Woolworths\"}}}}}";
	}

	/** A ProductSearch response containing one ProductSummary per sku, all from the given store. */
	private static String searchPage(String storeKey, int totalPages, String... skus) {
		StringBuilder results = new StringBuilder();
		for (String sku : skus) {
			if (results.length() > 0) {
				results.append(',');
			}
			results.append("{\"__typename\":\"ProductSummary\",\"sku\":\"").append(sku)
				.append("\",\"productName\":\"Product ").append(sku).append(" 500g\",\"slug\":\"product-").append(sku)
				.append("\",\"storeKey\":\"").append(storeKey)
				.append("\",\"variants\":[{\"unitOfMeasure\":\"EACH\",\"variantPrice\":{\"sellingPrice\":4.5,\"sellingUnit\":\"EA\"}}]}");
		}
		return "{\"data\":{\"My\":{\"products\":{\"totalPages\":" + totalPages
			+ ",\"currentPage\":0,\"results\":[" + results + "]}}}}";
	}

	/** Reads the body a request would send. */
	private static String bodyOf(HttpRequest request) {
		if (request.bodyPublisher().isEmpty()) {
			return "";
		}
		StringBuilder body = new StringBuilder();
		request.bodyPublisher().get().subscribe(new Flow.Subscriber<ByteBuffer>() {
			@Override public void onSubscribe(Flow.Subscription subscription) { subscription.request(Long.MAX_VALUE); }
			@Override public void onNext(ByteBuffer item) { body.append(StandardCharsets.UTF_8.decode(item)); }
			@Override public void onError(Throwable throwable) { throw new AssertionError(throwable); }
			@Override public void onComplete() { }
		});
		return body.toString();
	}

	private String uriOf(int requestIndex) {
		return scraper.sentRequests.get(requestIndex).uri().toString();
	}

	/** Queues the three dietary filter searches (vegan, vegetarian, organic), each a single page. */
	private void enqueueDietarySearches(String storeKey, String veganSku, String vegetarianSku, String organicSku) {
		scraper.enqueue(200, veganSku == null ? searchPage(storeKey, 1) : searchPage(storeKey, 1, veganSku));
		scraper.enqueue(200, vegetarianSku == null ? searchPage(storeKey, 1) : searchPage(storeKey, 1, vegetarianSku));
		scraper.enqueue(200, organicSku == null ? searchPage(storeKey, 1) : searchPage(storeKey, 1, organicSku));
	}

	// ------------------------------------------------------------------ store selection

	@Test
	void changeStoreSendsPickupIdAndAcceptsMatchingEcho() {
		scraper.enqueue(200, storeEcho("9529"));

		assertTrue(scraper.changeStore("9529"));

		HttpRequest sent = scraper.sentRequests.get(0);
		assertEquals("POST", sent.method());
		assertEquals(GRAPHQL + "SetCartShoppingMode", sent.uri().toString());
		assertEquals(Optional.of("SetCartShoppingMode"), sent.headers().firstValue("wnzx-operation-name"));
		String body = bodyOf(sent);
		assertTrue(body.contains("\"pickupLocationId\":\"9529\""));
		assertTrue(body.contains("\"shoppingMode\":\"Pickup\""));
	}

	@Test
	void changeStoreFailsWhenAnotherStoreIsEchoed() {
		scraper.enqueue(200, storeEcho("9146"));

		assertFalse(scraper.changeStore("9529"));
	}

	@Test
	void changeStoreFailsOnErrorStatusOrNoResponse() {
		scraper.enqueue(403, "Access Denied");
		assertFalse(scraper.changeStore("9529"));

		scraper.enqueueNoResponse();
		assertFalse(scraper.changeStore("9529"));
	}

	// ------------------------------------------------------------------ product extraction

	@Test
	void extractProductsReadsRealSearchResponseAndSkipsAdsAndBanners() {
		List<ProductInfo> products = scraper.extractProducts(resource("/woolworths/search_page.json"));

		// the fixture holds 1 sponsored ad, 1 recipe banner and 5 real products
		assertEquals(5, products.size());

		ProductInfo broccoli = products.get(0);
		assertEquals("Woolworths Fresh Broccoli Head", broccoli.productName());
		assertEquals(new BigDecimal("2"), broccoli.price());
		assertEquals("ea", broccoli.productSize());
		assertEquals("281082/woolworths-fresh-broccoli-head", broccoli.productId());

		// sold by weight: first variant is per kg, the "Min Order 250g" is not the size
		ProductInfo bananas = products.get(1);
		assertEquals(new BigDecimal("3.99"), bananas.price());
		assertEquals("kg", bananas.productSize());

		ProductInfo mince = products.get(2);
		assertEquals("Woolworths Essentials Beef Mince Value 400g", mince.productName());
		assertEquals("400g", mince.productSize());
	}

	@Test
	void extractProductsReturnsEmptyListForUnreadableBody() {
		assertTrue(scraper.extractProducts("<html>Access Denied</html>").isEmpty());
	}

	@Test
	void extractSizeHandlesWoolworthsNameFormats() {
		assertEquals("750g", WoolworthsScraper.extractSize("Gopala Yoghurt Tub Natural Full Cream 750g", "EACH"));
		assertEquals("1.2kg", WoolworthsScraper.extractSize("Weet-Bix Cereal 1.2kg", "EACH"));
		assertEquals("12 x 250ml", WoolworthsScraper.extractSize("Up&Go Protein Liquid Breakfast Choc Hit Fridge Pack 12 x 250mL", "EACH"));
		assertEquals("12 x 250ml", WoolworthsScraper.extractSize("Up&Go Protein Breakfast Drink Vanilla 12x250mL Cartons", "EACH"));
		assertEquals("2 x 95g", WoolworthsScraper.extractSize("Aunt Betty's Steamed Pudding Gooey Caramel 2 x 95g Pottles", "EACH"));
		assertEquals("204g", WoolworthsScraper.extractSize("Oreo Creme Filled Biscuits Original Minis 10 Pack - 204g", "EACH"));
		assertEquals("1kg", WoolworthsScraper.extractSize("Woolworths NZ Beef Mince Grass Fed 18% Fat 1kg tray", "EACH"));
		assertEquals("2l", WoolworthsScraper.extractSize("Anchor Blue Top Milk 2L", "EACH"));
		assertEquals("12pk", WoolworthsScraper.extractSize("Hellers Sausages Precooked 12 Pack", "EACH"));
		assertEquals("kg", WoolworthsScraper.extractSize("Woolworths Lamb Leg Frozen Bone In Min Order 3kg", "KG"));
		assertEquals("ea", WoolworthsScraper.extractSize("Woolworths Fresh Cucumbers Telegraph", "EACH"));
	}

	// ------------------------------------------------------------------ paging and dietary tags

	@Test
	void fetchProductsReadsUpToMaxPagesThenRunsDietarySearches() {
		scraper.setMaxPages(2);
		scraper.enqueue(200, searchPage("9228", 3, "1", "2"));
		scraper.enqueue(200, searchPage("9228", 3, "3"));
		enqueueDietarySearches("9228", null, null, null);

		List<ProductInfo> products = scraper.fetchProducts(CATEGORY);

		assertEquals(3, products.size());
		assertEquals(5, scraper.sentRequests.size()); // 2 pages (of 3) + vegan, vegetarian, organic
		assertEquals(GRAPHQL + "ProductSearch", uriOf(0));
		String firstPage = bodyOf(scraper.sentRequests.get(0));
		assertTrue(firstPage.contains("\"value\":\"" + CATEGORY + "\""));
		assertTrue(firstPage.contains("\"pageIndex\":0"));
		assertTrue(firstPage.contains("\"facetFilters\":[]"));
		assertTrue(bodyOf(scraper.sentRequests.get(1)).contains("\"pageIndex\":1"));
		assertTrue(bodyOf(scraper.sentRequests.get(2)).contains("\"facetFilters\":[[{\"key\":\"isVegan\",\"value\":\"true\"}]]"));
		assertTrue(bodyOf(scraper.sentRequests.get(3)).contains("isVegetarian"));
		assertTrue(bodyOf(scraper.sentRequests.get(4)).contains("isOrganic"));
	}

	@Test
	void fetchProductsStopsAtLastPage() {
		scraper.setMaxPages(5);
		scraper.enqueue(200, searchPage("9228", 1, "1"));
		enqueueDietarySearches("9228", null, null, null);

		assertEquals(1, scraper.fetchProducts(CATEGORY).size());
		assertEquals(4, scraper.sentRequests.size());
	}

	@Test
	void dietaryTagsComeFromFilteredSearches() {
		scraper.enqueue(200, searchPage("9228", 1, "100", "200"));
		// 100 is vegan and vegetarian, 200 is organic; a vegan result outside our products is ignored
		scraper.enqueue(200, searchPage("9228", 1, "100", "999"));
		scraper.enqueue(200, searchPage("9228", 1, "100"));
		scraper.enqueue(200, searchPage("9228", 1, "200"));
		scraper.fetchProducts(CATEGORY);

		scraper.enqueue(200, "{}"); // product page without ingredient data
		Pair<Set<Allergen>, Set<Dietary>> first = scraper.getProductInfo("100/product-100", "9529");
		scraper.enqueue(200, "{}");
		Pair<Set<Allergen>, Set<Dietary>> second = scraper.getProductInfo("200/product-200", "9529");

		assertEquals(Set.of(Dietary.VEGAN, Dietary.VEGETARIAN), first.getSecond());
		assertEquals(Set.of(Dietary.ORGANIC), second.getSecond());
	}

	@Test
	void dietarySearchesRunOncePerCategoryAcrossStores() {
		scraper.enqueue(200, searchPage("9228", 1, "1"));
		enqueueDietarySearches("9228", null, null, null);
		scraper.fetchProducts(CATEGORY);

		scraper.enqueue(200, storeEcho("9161"));
		scraper.changeStore("9161");
		scraper.enqueue(200, searchPage("9545", 1, "1"));
		scraper.fetchProducts(CATEGORY);

		assertEquals(6, scraper.sentRequests.size()); // 1 + 3 searches, store change, 1 page (no searches)
	}

	// ------------------------------------------------------------------ session handling

	@Test
	void guestTokenFromResponseIsSentBackAndDroppedOnRefresh() {
		// as seen live: the first API call without a token gets one in Set-Cookie
		scraper.enqueueWithCookie(200, storeEcho("9529"),
			"__guest__token=token-one; Path=/; Secure; HttpOnly; SameSite=lax");
		scraper.changeStore("9529");
		scraper.enqueue(200, searchPage("9270", 1, "1"));
		enqueueDietarySearches("9270", null, null, null);
		scraper.fetchProducts(CATEGORY);

		assertTrue(scraper.sentRequests.get(0).headers().firstValue("Cookie").isEmpty());
		assertEquals(Optional.of("__guest__token=token-one"), scraper.sentRequests.get(1).headers().firstValue("Cookie"));

		// refreshing forgets the token, so the store change starts a new session and gets a new one
		scraper.enqueueWithCookie(200, storeEcho("9529"), "__guest__token=token-two; Path=/");
		scraper.refreshCookies();
		assertTrue(scraper.sentRequests.get(5).headers().firstValue("Cookie").isEmpty());
		assertEquals("token-two", scraper.guestToken);
	}

	@Test
	void storeKeyMismatchRefreshesSessionReselectsStoreAndRetries() {
		scraper.enqueue(200, storeEcho("9529"));
		scraper.changeStore("9529");

		scraper.setMaxPages(2);
		scraper.enqueue(200, searchPage("9270", 3, "1"));  // pins Three Kings' storeKey
		scraper.enqueue(200, searchPage("9171", 3, "2"));  // default store: session was lost
		scraper.enqueue(200, storeEcho("9529"));           // store re-selected on the new session
		scraper.enqueue(200, searchPage("9270", 3, "2"));  // retried page
		enqueueDietarySearches("9270", null, null, null);

		List<ProductInfo> products = scraper.fetchProducts(CATEGORY);

		assertEquals(2, products.size());
		assertEquals(2, scraper.sessionClears); // setUp() + the refresh
		assertEquals(GRAPHQL + "SetCartShoppingMode", uriOf(3));
		assertTrue(bodyOf(scraper.sentRequests.get(4)).contains("\"pageIndex\":1"));
	}

	@Test
	void pageStillFromWrongStoreAfterRefreshIsSkipped() {
		scraper.enqueue(200, storeEcho("9529"));
		scraper.changeStore("9529");

		scraper.setMaxPages(2);
		scraper.enqueue(200, searchPage("9270", 3, "1"));
		scraper.enqueue(200, searchPage("9171", 3, "2"));
		scraper.enqueue(200, storeEcho("9529"));
		scraper.enqueue(200, searchPage("9171", 3, "2"));
		enqueueDietarySearches("9270", null, null, null);

		List<ProductInfo> products = scraper.fetchProducts(CATEGORY);

		assertEquals(1, products.size()); // only the page from the right store
	}

	@Test
	void sessionIsRefreshedBeforeGuestTokenExpires() {
		scraper.enqueue(200, storeEcho("9529"));
		scraper.changeStore("9529");
		scraper.clock = scraper.clock.plus(Duration.ofMinutes(51));

		scraper.enqueue(200, storeEcho("9529")); // refresh re-selects the store first
		scraper.enqueue(200, searchPage("9270", 1, "1"));
		enqueueDietarySearches("9270", null, null, null);
		scraper.fetchProducts(CATEGORY);

		assertEquals(2, scraper.sessionClears);
		assertEquals(GRAPHQL + "SetCartShoppingMode", uriOf(1));
		assertEquals(GRAPHQL + "ProductSearch", uriOf(2));
	}

	@Test
	void sendRequestWithAuthRefreshesAndRetriesOnError() {
		scraper.enqueue(200, storeEcho("9529"));
		scraper.changeStore("9529");

		scraper.enqueue(401, "expired");
		scraper.enqueue(200, storeEcho("9529"));
		scraper.enqueue(200, searchPage("9270", 1, "1"));
		enqueueDietarySearches("9270", null, null, null);

		assertEquals(1, scraper.fetchProducts(CATEGORY).size());
		assertEquals(2, scraper.sessionClears);
	}

	@Test
	void sendRequestWithAuthThrowsIfStillRejectedAfterRefresh() {
		scraper.enqueue(200, storeEcho("9529"));
		scraper.changeStore("9529");

		scraper.enqueue(403, "denied");
		scraper.enqueue(200, storeEcho("9529"));
		scraper.enqueue(403, "denied again");

		assertThrows(IllegalStateException.class, () -> scraper.fetchProducts(CATEGORY));
	}

	@Test
	void refreshThrowsIfStoreCannotBeReselected() {
		scraper.enqueue(200, storeEcho("9529"));
		scraper.changeStore("9529");

		scraper.enqueue(200, storeEcho("9171"));
		assertThrows(IllegalStateException.class, scraper::refreshCookies);
	}

	// ------------------------------------------------------------------ product details

	@Test
	void getProductInfoReadsIngredientsFromRealProductPage() {
		scraper.enqueue(200, resource("/woolworths/product_detail_rsc.txt"));

		Pair<Set<Allergen>, Set<Dietary>> info = scraper.getProductInfo("885704/weet-bix-cereal", "9529");

		HttpRequest sent = scraper.sentRequests.get(0);
		assertEquals("https://www.woolworths.co.nz/shop/product-details/885704/weet-bix-cereal?_rsc=1", sent.uri().toString());
		assertEquals(Optional.of("1"), sent.headers().firstValue("RSC"));
		// "Wholegrain Wheat (97%), Sugar, Salt, Barley Malt Extract, ..."
		assertEquals(Set.of(Allergen.WHEAT), info.getFirst());
		assertTrue(info.getSecond().isEmpty());
	}

	@Test
	void getProductInfoUsesAllergenStatement() {
		scraper.enqueue(200, "5:[\"$\",\"div\",null,{\"product\":{\"ingredients\":\"Water, Sugar\","
			+ "\"allergenContained\":\"Contains Milk and Soy\"}}]");

		Pair<Set<Allergen>, Set<Dietary>> info = scraper.getProductInfo("1/test", "9529");

		assertEquals(Set.of(Allergen.DAIRY, Allergen.SOY), info.getFirst());
	}

	@Test
	void getProductInfoWithUnavailablePageReturnsNoAllergens() {
		scraper.enqueue(404, "not found");

		Pair<Set<Allergen>, Set<Dietary>> info = scraper.getProductInfo("1/test", "9529");

		assertTrue(info.getFirst().isEmpty());
	}

	@Test
	void readStringAfterKeyHandlesEscapesNullsAndMissingKeys() {
		String text = "x:{\"ingredients\":\"Milk \\\"A2\\\" (98%), Salt\",\"allergenContained\":null}";

		assertEquals("Milk \"A2\" (98%), Salt", scraper.readStringAfterKey(text, "ingredients"));
		assertNull(scraper.readStringAfterKey(text, "allergenContained"));
		assertNull(scraper.readStringAfterKey(text, "directionsOfUse"));
	}

	@Test
	void extractProductsFromDocumentIsUnsupported() {
		assertThrows(UnsupportedOperationException.class, () -> scraper.extractProducts(org.jsoup.Jsoup.parse("<html></html>")));
	}
}
