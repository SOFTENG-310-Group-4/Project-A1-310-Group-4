package nz.ac.auckland.grocerfy.scraper.woolworths;

import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.http.HttpResponse.BodyHandler;
import java.time.Instant;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.function.Supplier;

import org.jsoup.nodes.Document;
import org.springframework.data.util.Pair;

import com.fasterxml.jackson.databind.JsonNode;

import nz.ac.auckland.grocerfy.dto.ProductInfo;
import nz.ac.auckland.grocerfy.model.Allergen;
import nz.ac.auckland.grocerfy.model.Dietary;
import nz.ac.auckland.grocerfy.model.LoyaltyProgram;
import nz.ac.auckland.grocerfy.scraper.SupermarketScraper;

/**
 * Scraper for Woolworths NZ (woolworths.co.nz). Coordinates the pieces in this package:
 * <ul>
 *   <li>{@link WoolworthsSession}: guest session, store selection, refresh and retry</li>
 *   <li>{@link WoolworthsRequests}: the GraphQL and product page requests</li>
 *   <li>{@link WoolworthsProductParser}: search results to ProductInfo (sizes, member prices)</li>
 *   <li>{@link WoolworthsProductDetailParser}: ingredients and allergen text from product pages</li>
 *   <li>{@link WoolworthsDietaryTags}: Vegan / Vegetarian / Organic tags from filtered searches</li>
 * </ul>
 * Unlike the Foodstuffs sites, Woolworths has no server-rendered catalogue pages: products come from a
 * GraphQL API, so this scraper overrides fetchProducts() and treats each "link" in scrape_targets.json
 * as a Woolworths category key (e.g. "9-BDB6545B" for Fruit &amp; Veg), paging through up to maxPages pages.
 * Department keys (from the category filter in a ProductSearch response): Fruit &amp; Veg 9-BDB6545B,
 * Meat &amp; Poultry 9-75C37855, Fish &amp; Seafood 9_0C90660, Fridge &amp; Deli 9-1B62CA0E, Bakery 9-18EB43BA,
 * Frozen 9-5C2568E0, Pantry 9-4286804C, Drinks 9-1C861292, Beer &amp; Wine 9_32FCDBF, Health &amp; Body 9-9CE98992,
 * Baby &amp; Child 9-F51D37CD, Pet 9-50BDA4A9, Household 9-3C550DA5.
 * <p>
 * Stores: the "region_cookie" of a branch is its pickup location id (e.g. Three Kings 9529).
 */
public final class WoolworthsScraper extends SupermarketScraper {

    private final HttpTransport transport;
    final WoolworthsSession session;
    private final WoolworthsDietaryTags dietaryTags;

    public WoolworthsScraper() {
        this(HttpTransport.overNetwork(), Instant::now);
    }

    /**
     * @param transport sends the requests (a fake in tests)
     * @param clock the current time, used for the guest session's expiry
     */
    WoolworthsScraper(HttpTransport transport, Supplier<Instant> clock) {
        this.transport = transport;
        this.session = new WoolworthsSession(transport, clock);
        this.dietaryTags = new WoolworthsDietaryTags(this::searchPage, this::randomRequestDelay);
    }

    // ------------------------------------------------------------------ session and store

    /**
     * Starts a fresh guest session; called at the start of every scrape run.
     */
    @Override
    public void setupCookies() {
        session.reset();
        // re-check dietary tags, since the range changes over time
        dietaryTags.clear();
    }

    @Override
    public void refreshCookies() {
        session.refresh();
    }

    @Override
    public <T> HttpResponse<T> sendRequestWithAuth(HttpRequest request, BodyHandler<T> handler) {
        return session.sendWithAuth(request, handler);
    }

    /**
     * @param pickupLocationId the store's pickup location id (the "region_cookie" in scrape_targets.json)
     * @return true if Woolworths confirmed the store change
     */
    @Override
    public boolean changeStore(String pickupLocationId) {
        return session.changeStore(pickupLocationId);
    }

    // ------------------------------------------------------------------ catalogue

    /**
     * Fetches up to maxPages pages of one Woolworths category, then looks up dietary tags for those products.
     * @param categoryKey a category key from scrape_targets.json, e.g. "9-BDB6545B"
     * @return the products in the category, in the site's relevance order
     */
    @Override
    public List<ProductInfo> fetchProducts(String categoryKey) {
        List<ProductInfo> products = new ArrayList<>();
        for (int page = 0; page < maxPages; page++) {
            if (page > 0) {
                randomRequestDelay();
            }
            JsonNode productsNode = searchPage(categoryKey, page, null);
            if (productsNode == null) {
                break; // error already reported
            }
            products.addAll(WoolworthsProductParser.parseProducts(productsNode));
            if (WoolworthsProductParser.isLastPage(productsNode, page)) {
                break;
            }
        }
        dietaryTags.findFor(categoryKey, products);
        return products;
    }

    /**
     * Requests one page of a category, optionally narrowed by a search filter such as "isVegan".
     * Checks every product came from the selected store, refreshing the session and retrying once if not.
     * @return the "products" node of the response, or null if the page could not be read
     */
    JsonNode searchPage(String categoryKey, int pageIndex, String facetKey) {
        session.refreshIfExpiring();
        JsonNode productsNode = requestSearchPage(categoryKey, pageIndex, facetKey);
        if (productsNode == null || session.matchesPinnedStore(WoolworthsProductParser.storeKeys(productsNode))) {
            return productsNode;
        }

        System.err.println("Woolworths returned another store's products (session lost?), refreshing and retrying...");
        session.refresh();
        productsNode = requestSearchPage(categoryKey, pageIndex, facetKey);
        if (productsNode != null && !session.matchesPinnedStore(WoolworthsProductParser.storeKeys(productsNode))) {
            System.err.println("Woolworths still returns the wrong store for category " + categoryKey
                + " page " + pageIndex + ", skipping page.");
            return null;
        }
        return productsNode;
    }

    private JsonNode requestSearchPage(String categoryKey, int pageIndex, String facetKey) {
        HttpResponse<String> response = session.sendWithAuth(
            WoolworthsRequests.productSearch(categoryKey, pageIndex, facetKey), HttpResponse.BodyHandlers.ofString());
        return WoolworthsProductParser.readProductsNode(response.body(), categoryKey);
    }

    /**
     * Woolworths catalogue pages are JSON from fetchProducts(), never HTML documents.
     * @throws UnsupportedOperationException always; use extractProducts(String)
     */
    @Override
    public List<ProductInfo> extractProducts(Document pageData) {
        throw new UnsupportedOperationException("Woolworths products are read from JSON; use extractProducts(String).");
    }

    /**
     * Extracts products from a raw ProductSearch response body.
     * @param responseBody the JSON returned by the ProductSearch GraphQL operation
     * @return the products in it (sponsored ads and recipe banners are skipped)
     */
    public List<ProductInfo> extractProducts(String responseBody) {
        return WoolworthsProductParser.parseProducts(responseBody);
    }

    /** Member prices on Woolworths are Everyday Rewards prices. */
    @Override
    public LoyaltyProgram getLoyaltyProgram() {
        return LoyaltyProgram.EVERYDAY_REWARDS;
    }

    // ------------------------------------------------------------------ product details

    /**
     * Reads a product's ingredients and allergen statement from its product page, and combines the
     * allergens found there with the dietary tags from the filtered searches.
     * @param productId "{sku}/{slug}", as produced by extractProducts()
     * @param storeCode unused: product details are the same for every store
     * @return the product's allergens and dietary tags
     */
    @Override
    public Pair<Set<Allergen>, Set<Dietary>> getProductInfo(String productId, String storeCode) {
        Set<Dietary> productDietary = dietaryTags.tagsFor(productId);

        // product pages don't need the guest session
        Optional<HttpResponse<String>> responseOptional = transport.send(
            WoolworthsRequests.productDetails(productId), HttpResponse.BodyHandlers.ofString());
        if (responseOptional.isEmpty() || responseOptional.get().statusCode() >= 400) {
            System.err.println("Product details for Woolworths product " + productId + " unavailable, allergens unknown.");
            return Pair.of(EnumSet.noneOf(Allergen.class), productDietary);
        }

        String allergenText = WoolworthsProductDetailParser.allergenText(responseOptional.get().body(), productId);
        return Pair.of(matchAllergens(allergenText), productDietary);
    }
}
