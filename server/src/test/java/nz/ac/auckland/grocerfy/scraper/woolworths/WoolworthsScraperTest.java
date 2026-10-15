package nz.ac.auckland.grocerfy.scraper.woolworths;

import static nz.ac.auckland.grocerfy.scraper.woolworths.WoolworthsFakes.GRAPHQL;
import static nz.ac.auckland.grocerfy.scraper.woolworths.WoolworthsFakes.resource;
import static nz.ac.auckland.grocerfy.scraper.woolworths.WoolworthsFakes.searchPage;
import static nz.ac.auckland.grocerfy.scraper.woolworths.WoolworthsFakes.storeEcho;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.net.http.HttpRequest;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.data.util.Pair;

import nz.ac.auckland.grocerfy.dto.ProductInfo;
import nz.ac.auckland.grocerfy.model.Allergen;
import nz.ac.auckland.grocerfy.model.Dietary;
import nz.ac.auckland.grocerfy.model.LoyaltyProgram;
import nz.ac.auckland.grocerfy.scraper.woolworths.WoolworthsFakes.FakeTransport;

/**
 * Scenario tests for WoolworthsScraper as a whole: store changes, paging, dietary searches, session
 * refreshes and product details. The network is a FakeTransport answering from queued canned responses,
 * and the clock is a field the tests move forward. Parsing and request details are tested in the
 * WoolworthsProductParser / WoolworthsProductDetailParser / WoolworthsRequests tests.
 */
public class WoolworthsScraperTest {

    private static final String CATEGORY = "9-75C37855"; // Meat & Poultry

    private FakeTransport transport;
    private Instant clock;
    private WoolworthsScraper scraper;

    @BeforeEach
    void setUp() {
        transport = new FakeTransport();
        clock = Instant.parse("2026-10-13T06:00:00Z");
        scraper = new WoolworthsScraper(transport, () -> clock);
        scraper.setupCookies();
    }

    /** Queues the three dietary filter searches (vegan, vegetarian, organic), each a single page. */
    private void enqueueDietarySearches(String storeKey, String veganSku, String vegetarianSku, String organicSku) {
        transport.enqueue(200, veganSku == null ? searchPage(storeKey, 1) : searchPage(storeKey, 1, veganSku));
        transport.enqueue(200, vegetarianSku == null ? searchPage(storeKey, 1) : searchPage(storeKey, 1, vegetarianSku));
        transport.enqueue(200, organicSku == null ? searchPage(storeKey, 1) : searchPage(storeKey, 1, organicSku));
    }

    // ------------------------------------------------------------------ store selection

    @Test
    void changeStoreSendsPickupIdAndAcceptsMatchingEcho() {
        transport.enqueue(200, storeEcho("9529"));

        assertTrue(scraper.changeStore("9529"));

        assertEquals(GRAPHQL + "SetCartShoppingMode", transport.uriOf(0));
        assertTrue(transport.bodyOf(0).contains("\"pickupLocationId\":\"9529\""));
    }

    @Test
    void changeStoreFailsWhenAnotherStoreIsEchoed() {
        transport.enqueue(200, storeEcho("9146"));

        assertFalse(scraper.changeStore("9529"));
    }

    @Test
    void changeStoreFailsOnErrorStatusNoResponseOrUnreadableBody() {
        transport.enqueue(403, "Access Denied");
        assertFalse(scraper.changeStore("9529"));

        transport.enqueueNoResponse();
        assertFalse(scraper.changeStore("9529"));

        transport.enqueue(200, "<html>not json</html>");
        assertFalse(scraper.changeStore("9529"));
    }

    // ------------------------------------------------------------------ catalogue

    @Test
    void fetchProductsReadsUpToMaxPagesThenRunsDietarySearches() {
        scraper.setMaxPages(2);
        transport.enqueue(200, searchPage("9228", 3, "1", "2"));
        transport.enqueue(200, searchPage("9228", 3, "3"));
        enqueueDietarySearches("9228", null, null, null);

        List<ProductInfo> products = scraper.fetchProducts(CATEGORY);

        assertEquals(3, products.size());
        assertEquals(5, transport.sentRequests.size()); // 2 pages (of 3) + vegan, vegetarian, organic
        assertEquals(GRAPHQL + "ProductSearch", transport.uriOf(0));
        assertTrue(transport.bodyOf(0).contains("\"pageIndex\":0"));
        assertTrue(transport.bodyOf(1).contains("\"pageIndex\":1"));
        assertTrue(transport.bodyOf(2).contains("isVegan"));
        assertTrue(transport.bodyOf(3).contains("isVegetarian"));
        assertTrue(transport.bodyOf(4).contains("isOrganic"));
    }

    @Test
    void fetchProductsStopsAtLastPage() {
        scraper.setMaxPages(5);
        transport.enqueue(200, searchPage("9228", 1, "1"));
        enqueueDietarySearches("9228", null, null, null);

        assertEquals(1, scraper.fetchProducts(CATEGORY).size());
        assertEquals(4, transport.sentRequests.size());
    }

    @Test
    void fetchProductsStopsAtAnUnreadablePage() {
        transport.enqueue(200, "{\"errors\":[{\"message\":\"bad category\"}]}");

        assertTrue(scraper.fetchProducts(CATEGORY).isEmpty());
        assertEquals(1, transport.sentRequests.size()); // no products, so no dietary searches either
    }

    @Test
    void extractProductsReadsAResponseBodyAndRejectsHtmlDocuments() {
        assertEquals(5, scraper.extractProducts(resource("/woolworths/search_page.json")).size());
        assertThrows(UnsupportedOperationException.class,
            () -> scraper.extractProducts(org.jsoup.Jsoup.parse("<html></html>")));
    }

    @Test
    void memberPricesBelongToEverydayRewards() {
        assertEquals(LoyaltyProgram.EVERYDAY_REWARDS, scraper.getLoyaltyProgram());
    }

    // ------------------------------------------------------------------ dietary tags

    @Test
    void dietaryTagsComeFromFilteredSearches() {
        transport.enqueue(200, searchPage("9228", 1, "100", "200"));
        // 100 is vegan and vegetarian, 200 is organic; a vegan result outside our products is ignored
        transport.enqueue(200, searchPage("9228", 1, "100", "999"));
        transport.enqueue(200, searchPage("9228", 1, "100"));
        transport.enqueue(200, searchPage("9228", 1, "200"));
        scraper.fetchProducts(CATEGORY);

        transport.enqueue(200, "{}"); // product page without ingredient data
        Pair<Set<Allergen>, Set<Dietary>> first = scraper.getProductInfo("100/product-100", "9529");
        transport.enqueue(200, "{}");
        Pair<Set<Allergen>, Set<Dietary>> second = scraper.getProductInfo("200/product-200", "9529");

        assertEquals(Set.of(Dietary.VEGAN, Dietary.VEGETARIAN), first.getSecond());
        assertEquals(Set.of(Dietary.ORGANIC), second.getSecond());
    }

    @Test
    void dietarySearchesRunOncePerCategoryAcrossStores() {
        transport.enqueue(200, searchPage("9228", 1, "1"));
        enqueueDietarySearches("9228", null, null, null);
        scraper.fetchProducts(CATEGORY);

        transport.enqueue(200, storeEcho("9161"));
        scraper.changeStore("9161");
        transport.enqueue(200, searchPage("9545", 1, "1"));
        scraper.fetchProducts(CATEGORY);

        assertEquals(6, transport.sentRequests.size()); // 1 + 3 searches, store change, 1 page (no searches)
    }

    @Test
    void setupCookiesStartsANewRunThatChecksDietaryTagsAgain() {
        transport.enqueue(200, searchPage("9228", 1, "1"));
        enqueueDietarySearches("9228", "1", null, null);
        scraper.fetchProducts(CATEGORY);

        scraper.setupCookies();
        transport.enqueue(200, searchPage("9228", 1, "1"));
        enqueueDietarySearches("9228", null, null, null);
        scraper.fetchProducts(CATEGORY);

        assertEquals(8, transport.sentRequests.size()); // the searches ran again in the new run
        transport.enqueue(200, "{}");
        assertTrue(scraper.getProductInfo("1/product-1", "9529").getSecond().isEmpty());
    }

    // ------------------------------------------------------------------ session handling

    @Test
    void guestTokenFromResponseIsSentBackAndDroppedOnRefresh() {
        // as seen live: the first API call without a token gets one in Set-Cookie
        transport.enqueueWithCookie(200, storeEcho("9529"),
            "__guest__token=token-one; Path=/; Secure; HttpOnly; SameSite=lax");
        scraper.changeStore("9529");
        transport.enqueue(200, searchPage("9270", 1, "1"));
        enqueueDietarySearches("9270", null, null, null);
        scraper.fetchProducts(CATEGORY);

        assertTrue(transport.sentRequests.get(0).headers().firstValue("Cookie").isEmpty());
        assertEquals(Optional.of("__guest__token=token-one"), transport.sentRequests.get(1).headers().firstValue("Cookie"));

        // refreshing forgets the token, so the store change starts a new session and gets a new one
        transport.enqueueWithCookie(200, storeEcho("9529"), "__guest__token=token-two; Path=/");
        scraper.refreshCookies();
        assertTrue(transport.sentRequests.get(5).headers().firstValue("Cookie").isEmpty());
        assertEquals("token-two", scraper.session.guestToken());
    }

    @Test
    void storeKeyMismatchRefreshesSessionReselectsStoreAndRetries() {
        transport.enqueue(200, storeEcho("9529"));
        scraper.changeStore("9529");

        scraper.setMaxPages(2);
        transport.enqueue(200, searchPage("9270", 3, "1"));  // pins Three Kings' storeKey
        transport.enqueue(200, searchPage("9171", 3, "2"));  // default store: session was lost
        transport.enqueue(200, storeEcho("9529"));           // store re-selected on the new session
        transport.enqueue(200, searchPage("9270", 3, "2"));  // retried page
        enqueueDietarySearches("9270", null, null, null);

        List<ProductInfo> products = scraper.fetchProducts(CATEGORY);

        assertEquals(2, products.size());
        assertEquals(GRAPHQL + "SetCartShoppingMode", transport.uriOf(3));
        assertTrue(transport.bodyOf(4).contains("\"pageIndex\":1"));
        assertEquals("9270", scraper.session.pinnedStoreKey());
    }

    @Test
    void pageStillFromWrongStoreAfterRefreshIsSkipped() {
        transport.enqueue(200, storeEcho("9529"));
        scraper.changeStore("9529");

        scraper.setMaxPages(2);
        transport.enqueue(200, searchPage("9270", 3, "1"));
        transport.enqueue(200, searchPage("9171", 3, "2"));
        transport.enqueue(200, storeEcho("9529"));
        transport.enqueue(200, searchPage("9171", 3, "2"));
        enqueueDietarySearches("9270", null, null, null);

        List<ProductInfo> products = scraper.fetchProducts(CATEGORY);

        assertEquals(1, products.size()); // only the page from the right store
    }

    @Test
    void sessionIsRefreshedBeforeGuestTokenExpires() {
        transport.enqueue(200, storeEcho("9529"));
        scraper.changeStore("9529");
        clock = clock.plus(Duration.ofMinutes(51));

        transport.enqueue(200, storeEcho("9529")); // refresh re-selects the store first
        transport.enqueue(200, searchPage("9270", 1, "1"));
        enqueueDietarySearches("9270", null, null, null);
        scraper.fetchProducts(CATEGORY);

        assertEquals(GRAPHQL + "SetCartShoppingMode", transport.uriOf(1));
        assertEquals(GRAPHQL + "ProductSearch", transport.uriOf(2));
    }

    @Test
    void sendRequestWithAuthRefreshesAndRetriesOnError() {
        transport.enqueue(200, storeEcho("9529"));
        scraper.changeStore("9529");

        transport.enqueue(401, "expired");
        transport.enqueue(200, storeEcho("9529"));
        transport.enqueue(200, searchPage("9270", 1, "1"));
        enqueueDietarySearches("9270", null, null, null);

        assertEquals(1, scraper.fetchProducts(CATEGORY).size());
        assertEquals(GRAPHQL + "SetCartShoppingMode", transport.uriOf(2)); // refresh between the 401 and the retry
    }

    @Test
    void sendRequestWithAuthThrowsIfStillRejectedAfterRefreshOrNoResponse() {
        transport.enqueue(200, storeEcho("9529"));
        scraper.changeStore("9529");

        transport.enqueue(403, "denied");
        transport.enqueue(200, storeEcho("9529"));
        transport.enqueue(403, "denied again");
        assertThrows(IllegalStateException.class, () -> scraper.fetchProducts(CATEGORY));

        HttpRequest request = WoolworthsRequests.productSearch(CATEGORY, 0, null);
        transport.enqueueNoResponse();
        assertThrows(IllegalStateException.class,
            () -> scraper.sendRequestWithAuth(request, java.net.http.HttpResponse.BodyHandlers.ofString()));
    }

    @Test
    void refreshThrowsIfStoreCannotBeReselected() {
        transport.enqueue(200, storeEcho("9529"));
        scraper.changeStore("9529");

        transport.enqueue(200, storeEcho("9171"));
        assertThrows(IllegalStateException.class, scraper::refreshCookies);
    }

    // ------------------------------------------------------------------ product details

    @Test
    void getProductInfoReadsIngredientsFromRealProductPage() {
        transport.enqueue(200, resource("/woolworths/product_detail_rsc.txt"));

        Pair<Set<Allergen>, Set<Dietary>> info = scraper.getProductInfo("885704/weet-bix-cereal", "9529");

        assertEquals("https://www.woolworths.co.nz/shop/product-details/885704/weet-bix-cereal?_rsc=1", transport.uriOf(0));
        // "Wholegrain Wheat (97%), Sugar, Salt, Barley Malt Extract, ..."
        assertEquals(Set.of(Allergen.WHEAT), info.getFirst());
        assertTrue(info.getSecond().isEmpty());
    }

    @Test
    void getProductInfoUsesAllergenStatement() {
        transport.enqueue(200, "5:[\"$\",\"div\",null,{\"product\":{\"ingredients\":\"Water, Sugar\","
            + "\"allergenContained\":\"Contains Milk and Soy\"}}]");

        Pair<Set<Allergen>, Set<Dietary>> info = scraper.getProductInfo("1/test", "9529");

        assertEquals(Set.of(Allergen.DAIRY, Allergen.SOY), info.getFirst());
    }

    @Test
    void getProductInfoWithUnavailablePageReturnsNoAllergens() {
        transport.enqueue(404, "not found");
        assertTrue(scraper.getProductInfo("1/test", "9529").getFirst().isEmpty());

        transport.enqueueNoResponse();
        assertTrue(scraper.getProductInfo("1/test", "9529").getFirst().isEmpty());
    }
}
