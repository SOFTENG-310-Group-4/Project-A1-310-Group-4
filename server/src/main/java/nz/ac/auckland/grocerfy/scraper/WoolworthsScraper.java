package nz.ac.auckland.grocerfy.scraper;

import java.io.IOException;
import java.math.BigDecimal;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.http.HttpResponse.BodyHandler;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.jsoup.nodes.Document;
import org.springframework.data.util.Pair;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.JsonToken;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import nz.ac.auckland.grocerfy.dto.ProductInfo;
import nz.ac.auckland.grocerfy.model.Allergen;
import nz.ac.auckland.grocerfy.model.Dietary;
import nz.ac.auckland.grocerfy.model.LoyaltyProgram;
import nz.ac.auckland.grocerfy.util.HttpUtils;

/**
 * Scraper for Woolworths NZ (woolworths.co.nz).
 * <p>
 * Unlike the Foodstuffs sites, Woolworths has no server-rendered catalogue pages: products come from a
 * GraphQL API, so this scraper overrides fetchProducts() and treats each "link" in scrape_targets.json
 * as a Woolworths category key (e.g. "9-BDB6545B" for Fruit &amp; Veg), paging through up to maxPages pages.
 * Department keys (from the category filter in a ProductSearch response): Fruit &amp; Veg 9-BDB6545B,
 * Meat &amp; Poultry 9-75C37855, Fish &amp; Seafood 9_0C90660, Fridge &amp; Deli 9-1B62CA0E, Bakery 9-18EB43BA,
 * Frozen 9-5C2568E0, Pantry 9-4286804C, Drinks 9-1C861292, Beer &amp; Wine 9_32FCDBF, Health &amp; Body 9-9CE98992,
 * Baby &amp; Child 9-F51D37CD, Pet 9-50BDA4A9, Household 9-3C550DA5.
 * <p>
 * Stores: the "region_cookie" of a branch is its pickup location id (e.g. Three Kings 9529).
 * <p>
 * Session: the site hands out an anonymous "__guest__token" cookie on the first API call. This scraper
 * keeps that token itself and sends it back on every API call (it is the only cookie the API needs).
 * The chosen pickup store is tied to that guest session, and the token expires after an hour, so
 * refreshing the session always re-applies the current store.
 * <p>
 * Each product's "storeKey" tells us which store's range and prices were returned. It is pinned on the
 * first page after a store change, and a page that disagrees means the session was lost, so the session
 * is refreshed and the page retried.
 * <p>
 * Product details (ingredients, allergens) come from the product page in Next.js's "_rsc" format, and
 * dietary tags come from the site's own Vegan / Vegetarian / Organic search filters.
 */
public class WoolworthsScraper extends SupermarketScraper {
    private static final String DOMAIN = "woolworths.co.nz";
    private static final String SITE_ADDRESS = "https://www." + DOMAIN;
    private static final String GRAPHQL_ENDPOINT = SITE_ADDRESS + "/api/graphql?op-name=";
    private static final String PRODUCT_DETAIL_ADDRESS = SITE_ADDRESS + "/shop/product-details/";

    static final int PAGE_SIZE = 36;
    static final int MAX_DIETARY_PAGES = 10;

    /** Guest tokens last 60 minutes; refresh a little early so a request never races the expiry. */
    static final Duration SESSION_LIFETIME = Duration.ofMinutes(50);

    private static final String SET_STORE_OPERATION = "SetCartShoppingMode";
    private static final String SET_STORE_QUERY =
        "mutation SetCartShoppingMode($setCartShoppingModeInput: SetCartShoppingModeInput!) {"
        + " setCartShoppingMode(input: $setCartShoppingModeInput) {"
        + " shoppingMode { mode pickupLocationId pickupLocation { name } } } }";

    private static final String SEARCH_OPERATION = "ProductSearch";
    // only the fields the scraper uses (the website's own query is ~7KB)
    private static final String SEARCH_QUERY =
        "query ProductSearch($searchInput: CompositeSearchInput!) {"
        + " My { products(searchInput: $searchInput) { totalPages currentPage"
        + " results { __typename ... on ProductSummary {"
        + " sku productName slug storeKey"
        + " variants { unitOfMeasure variantPrice { sellingPrice sellingUnit } }"
        + " tags { type userSegmentId decisionInputs } } } } } }"; // tags carry Everyday Rewards member prices

    /** Product tag holding the Everyday Rewards member price, e.g. decisionInputs.promotionalPrice = 529 (cents). */
    private static final String MEMBER_PRICE_TAG = "MemberPrice";

    /** Search filters ("facets") the site offers, mapped to our Dietary tags. */
    private static final Map<String, Dietary> DIETARY_FACETS = new LinkedHashMap<>();
    static {
        DIETARY_FACETS.put("isVegan", Dietary.VEGAN);
        DIETARY_FACETS.put("isVegetarian", Dietary.VEGETARIAN);
        DIETARY_FACETS.put("isOrganic", Dietary.ORGANIC);
    }

    /**
     * A pack size inside a product name, e.g. "750g", "1.2kg", "2L", "12 x 250mL", "12x250mL".
     * The size is only ever part of the name on Woolworths (there is no size field in search results).
     */
    private static final Pattern SIZE_PATTERN = Pattern.compile(
        "(?<![\\w.])(?:(\\d+)\\s*x\\s*)?(\\d+(?:\\.\\d+)?)\\s*(kg|g|ml|ltr|litres?|l)(?![a-z])",
        Pattern.CASE_INSENSITIVE);
    /** A pack count, e.g. "12 Pack", "30pk"; only used when the name has no weight or volume. */
    private static final Pattern PACK_PATTERN = Pattern.compile("(?<![\\w.])(\\d+)\\s*(?:pk|pack)(?![a-z])",
        Pattern.CASE_INSENSITIVE);
    private static final int MAX_SIZE_LENGTH = 20; // products.size column length

    private static final String[] GRAPHQL_HEADERS = {
        "Accept", "application/graphql-response+json,application/json;q=0.9",
        "Content-Type", "application/json",
        "Origin", SITE_ADDRESS,
        "Referer", SITE_ADDRESS + "/",
        "Sec-Fetch-Dest", "empty",
        "Sec-Fetch-Mode", "cors",
        "Sec-Fetch-Site", "same-origin"
    };

    private static final String GUEST_COOKIE = "__guest__token";

    /**
     * Woolworths gets its own client without the shared cookie jar: the session is just the guest token,
     * sent as a plain "Cookie: __guest__token=..." header. (The shared jar would also send the site's
     * bot-detection cookies in RFC 2965 "$Version" format, which risks the token not being read.)
     */
    private static final HttpClient CLIENT = HttpClient.newBuilder()
        .version(HttpClient.Version.HTTP_1_1)
        .followRedirects(HttpClient.Redirect.NORMAL)
        .connectTimeout(Duration.ofSeconds(30))
        .build();

    /** The current guest session's token (null = none yet; the next API call creates one). */
    protected String guestToken;
    /** Pickup location id of the store currently selected, re-applied whenever the session is refreshed. */
    protected String currentPickupId;
    /** storeKey seen on the first page after the last store change; later pages must match it. */
    protected String pinnedStoreKey;
    /** When the current guest session was started (null = no session yet). */
    protected Instant sessionStartedAt;

    /** SKU -> dietary tags found through the filtered searches. */
    private final Map<String, Set<Dietary>> dietaryBySku = new HashMap<>();
    /** Category keys whose filtered searches have already run (dietary tags don't change between stores). */
    private final Set<String> dietaryCheckedCategories = new HashSet<>();

    public WoolworthsScraper() {
        // no per-instance configuration; Woolworths is a single site
    }

    /**
     * Sends a request over the network (overridden in tests). Mirrors HttpUtils.sendHttpRequest(),
     * but with this scraper's cookie-less client.
     * @return the response, or Optional.empty() if the request failed to send
     */
    protected <T> Optional<HttpResponse<T>> send(HttpRequest request, BodyHandler<T> handler) {
        try {
            HttpResponse<T> response = CLIENT.send(request, handler);
            if (response.statusCode() >= 400) {
                System.err.println("This link has returned an error code " + response.statusCode() + ": " + request.uri());
            }
            return Optional.of(response);
        } catch (InterruptedException exc) {
            System.err.println("Caught interrupt from " + request.uri() + ", no data returned.");
            Thread.currentThread().interrupt();
        } catch (IOException exc) {
            System.err.println("Caught IO error from " + request.uri() + ", no data returned.");
        }
        return Optional.empty();
    }

    /**
     * Sends a request as part of the current guest session: attaches the guest token (if there is one)
     * and remembers any new token the response sets.
     */
    private <T> Optional<HttpResponse<T>> exchange(HttpRequest request, BodyHandler<T> handler) {
        HttpRequest sessionRequest = guestToken == null
            ? request
            : HttpRequest.newBuilder(request, (name, value) -> true)
                .header("Cookie", GUEST_COOKIE + "=" + guestToken)
                .build();
        Optional<HttpResponse<T>> response = send(sessionRequest, handler);
        response.ifPresent(this::rememberGuestToken);
        return response;
    }

    private void rememberGuestToken(HttpResponse<?> response) {
        for (String setCookie : response.headers().allValues("Set-Cookie")) {
            if (setCookie.startsWith(GUEST_COOKIE + "=")) {
                int end = setCookie.indexOf(';');
                guestToken = setCookie.substring(GUEST_COOKIE.length() + 1, end < 0 ? setCookie.length() : end);
            }
        }
    }

    /**
     * Forgets the guest token, so the next API call starts a new guest session.
     */
    protected void clearSession() {
        guestToken = null;
    }

    /**
     * Wrapper for testing: the current time, used to refresh the session before the guest token expires.
     */
    protected Instant now() {
        return Instant.now();
    }

    // ------------------------------------------------------------------ session and store

    /**
     * Starts a fresh guest session. No request is needed: Woolworths sets the guest token cookie on the
     * first API call, which is the store change that always follows.
     */
    @Override
    public void setupCookies() {
        clearSession();
        currentPickupId = null;
        pinnedStoreKey = null;
        sessionStartedAt = null;
        // called at the start of every scrape run: re-check dietary tags, since the range changes over time
        dietaryBySku.clear();
        dietaryCheckedCategories.clear();
    }

    /**
     * Replaces the guest session (e.g. when the token has expired) and re-selects the current store,
     * since the store choice belongs to the old session.
     */
    @Override
    public void refreshCookies() {
        clearSession();
        sessionStartedAt = null;
        if (currentPickupId != null && !selectStore(currentPickupId)) {
            throw new IllegalStateException("Could not re-select Woolworths store " + currentPickupId
                + " after refreshing the session.");
        }
    }

    @Override
    public <T> HttpResponse<T> sendRequestWithAuth(HttpRequest request, BodyHandler<T> handler) {
        Optional<HttpResponse<T>> responseOptional = exchange(request, handler);
        if (responseOptional.isEmpty()) {
            throw new IllegalStateException("Request returns null, unexpected (is the link valid?) " + request.uri());
        }
        HttpResponse<T> response = responseOptional.get();
        if (response.statusCode() < 400) {
            return response;
        }

        // session expired or rejected: new guest session + same store, then retry once.
        // exchange() attaches the new session's token, so the original request can simply be resent.
        refreshCookies();
        Optional<HttpResponse<T>> retryOptional = exchange(request, handler);
        if (retryOptional.isEmpty() || retryOptional.get().statusCode() >= 400) {
            throw new IllegalStateException("Error with request, does not return after refreshing session: " + request.uri());
        }
        return retryOptional.get();
    }

    /**
     * Selects a pickup store for the guest session, which decides the product range and prices returned.
     * @param pickupLocationId the store's pickup location id (the "region_cookie" in scrape_targets.json)
     * @return true if Woolworths confirmed the store change
     */
    @Override
    public boolean changeStore(String pickupLocationId) {
        if (!selectStore(pickupLocationId)) {
            return false;
        }
        currentPickupId = pickupLocationId;
        pinnedStoreKey = null; // learnt again from the first page for this store
        return true;
    }

    /**
     * Sends the store change and checks the response echoes the requested store back.
     */
    private boolean selectStore(String pickupLocationId) {
        ObjectNode input = mapper.createObjectNode();
        input.put("pickupLocationId", pickupLocationId);
        input.put("shoppingMode", "Pickup");
        ObjectNode variables = mapper.createObjectNode();
        variables.set("setCartShoppingModeInput", input);

        Optional<HttpResponse<String>> responseOptional = exchange(
            graphqlRequest(SET_STORE_OPERATION, SET_STORE_QUERY, variables),
            HttpResponse.BodyHandlers.ofString());
        if (responseOptional.isEmpty() || responseOptional.get().statusCode() >= 400) {
            System.err.println("Woolworths store change to " + pickupLocationId + " was rejected.");
            return false;
        }

        try {
            JsonNode mode = mapper.readTree(responseOptional.get().body())
                .path("data").path("setCartShoppingMode").path("shoppingMode");
            if (!pickupLocationId.equals(mode.path("pickupLocationId").asText())) {
                System.err.println("Woolworths did not switch to store " + pickupLocationId + ": " + responseOptional.get().body());
                return false;
            }
            System.out.println("Woolworths store set to " + mode.path("pickupLocation").path("name").asText());
        } catch (JsonProcessingException | IllegalArgumentException exc) {
            System.err.println("Woolworths store change response unreadable: " + exc.getLocalizedMessage());
            return false;
        }

        if (sessionStartedAt == null) {
            sessionStartedAt = now();
        }
        return true;
    }

    /**
     * Refreshes the session ahead of time if the guest token is about to expire.
     */
    private void refreshIfSessionExpiring() {
        if (sessionStartedAt != null && Duration.between(sessionStartedAt, now()).compareTo(SESSION_LIFETIME) >= 0) {
            System.out.println("Woolworths guest session is nearly an hour old, refreshing...");
            refreshCookies();
        }
    }

    private HttpRequest graphqlRequest(String operation, String query, JsonNode variables) {
        ObjectNode body = mapper.createObjectNode();
        body.put("operationName", operation);
        body.set("variables", variables);
        body.put("query", query);

        return HttpRequest.newBuilder()
            .uri(URI.create(GRAPHQL_ENDPOINT + operation))
            .headers(HttpUtils.getGenericHeaders())
            .headers(GRAPHQL_HEADERS)
            .header("wnzx-operation-name", operation)
            .POST(HttpRequest.BodyPublishers.ofString(body.toString()))
            .build();
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
            products.addAll(extractProducts(productsNode));
            if (page + 1 >= productsNode.path("totalPages").asInt(0)) {
                break;
            }
        }
        findDietaryTags(categoryKey, products);
        return products;
    }

    /**
     * Requests one page of a category, optionally narrowed by a search filter such as "isVegan".
     * Checks every product came from the pinned store, refreshing the session and retrying once if not.
     * @return the "products" node of the response, or null if the page could not be read
     */
    JsonNode searchPage(String categoryKey, int pageIndex, String facetKey) {
        refreshIfSessionExpiring();
        JsonNode productsNode = requestSearchPage(categoryKey, pageIndex, facetKey);
        if (productsNode == null || storeKeyMatches(productsNode)) {
            return productsNode;
        }

        System.err.println("Woolworths returned another store's products (session lost?), refreshing and retrying...");
        refreshCookies();
        productsNode = requestSearchPage(categoryKey, pageIndex, facetKey);
        if (productsNode != null && !storeKeyMatches(productsNode)) {
            System.err.println("Woolworths still returns the wrong store for category " + categoryKey
                + " page " + pageIndex + ", skipping page.");
            return null;
        }
        return productsNode;
    }

    private JsonNode requestSearchPage(String categoryKey, int pageIndex, String facetKey) {
        ObjectNode byCategory = mapper.createObjectNode();
        byCategory.put("pageIndex", pageIndex);
        byCategory.put("pageSize", PAGE_SIZE);
        // facetFilters is a list of groups; each group is a list of {key, value} filters
        ArrayNode facetFilters = byCategory.putArray("facetFilters");
        if (facetKey != null) {
            ObjectNode filter = facetFilters.addArray().addObject();
            filter.put("key", facetKey);
            filter.put("value", "true");
        }
        byCategory.putArray("staticFilters");
        byCategory.put("value", categoryKey);
        byCategory.put("sortBy", "RELEVANCE");
        ObjectNode searchInput = mapper.createObjectNode();
        searchInput.set("byCategoryKey", byCategory);
        ObjectNode variables = mapper.createObjectNode();
        variables.set("searchInput", searchInput);

        HttpResponse<String> response = sendRequestWithAuth(
            graphqlRequest(SEARCH_OPERATION, SEARCH_QUERY, variables),
            HttpResponse.BodyHandlers.ofString());
        try {
            JsonNode root = mapper.readTree(response.body());
            if (root.hasNonNull("errors")) {
                System.err.println("Woolworths search error for category " + categoryKey + ": " + root.get("errors"));
                return null;
            }
            JsonNode productsNode = root.path("data").path("My").path("products");
            if (productsNode.isMissingNode() || productsNode.isNull()) {
                System.err.println("Woolworths search for category " + categoryKey + " returned no product list. API change?");
                return null;
            }
            return productsNode;
        } catch (JsonProcessingException | IllegalArgumentException exc) {
            System.err.println("Woolworths search response unreadable for category " + categoryKey + ": " + exc.getLocalizedMessage());
            return null;
        }
    }

    /**
     * Pins the storeKey on the first page after a store change; afterwards every product must match it.
     */
    private boolean storeKeyMatches(JsonNode productsNode) {
        for (JsonNode result : productsNode.path("results")) {
            if (!"ProductSummary".equals(result.path("__typename").asText())) {
                continue;
            }
            String storeKey = result.path("storeKey").asText(null);
            if (storeKey == null) {
                continue;
            }
            if (pinnedStoreKey == null) {
                pinnedStoreKey = storeKey;
            } else if (!pinnedStoreKey.equals(storeKey)) {
                return false;
            }
        }
        return true;
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
        try {
            return extractProducts(mapper.readTree(responseBody).path("data").path("My").path("products"));
        } catch (JsonProcessingException exc) {
            System.err.println("Woolworths product list unreadable: " + exc.getLocalizedMessage());
            return new ArrayList<>();
        }
    }

    private List<ProductInfo> extractProducts(JsonNode productsNode) {
        List<ProductInfo> products = new ArrayList<>();
        for (JsonNode result : productsNode.path("results")) {
            // SponsoredProduct = paid ads (often from other categories), ContentInGridResultItem = recipe banners
            if (!"ProductSummary".equals(result.path("__typename").asText())) {
                continue;
            }
            String name = result.path("productName").asText("").trim();
            String sku = result.path("sku").asText("");
            JsonNode variant = result.path("variants").path(0);
            JsonNode priceNode = variant.path("variantPrice").path("sellingPrice");
            if (name.isEmpty() || sku.isEmpty() || !priceNode.isNumber()) {
                System.err.println("Price not found for product " + name + ", skipping...");
                continue;
            }

            BigDecimal price = new BigDecimal(priceNode.asText());
            String size = extractSize(name, variant.path("unitOfMeasure").asText(""));
            // product pages live at /shop/product-details/{sku}/{slug}, so the id carries both
            String productId = sku + "/" + result.path("slug").asText("");
            BigDecimal memberPrice = extractMemberPrice(result.path("tags"), price,
                variant.path("variantPrice").path("sellingUnit").asText(""));
            products.add(new ProductInfo(name, price, size, productId, memberPrice));
        }
        return products;
    }

    /**
     * Reads the Everyday Rewards member price from a product's tags, e.g.
     * {"type": "MemberPrice", "userSegmentId": "rewards-member", "decisionInputs": {"promotionalPrice": 529, "uom": "EA"}}.
     * @param tags the product's tags
     * @param shelfPrice the regular price; a member price that isn't lower is ignored
     * @param sellingUnit the unit the shelf price is for ("EA", "KG"); a member price for another unit is ignored
     * @return the member price in dollars, or null if the product has none
     */
    static BigDecimal extractMemberPrice(JsonNode tags, BigDecimal shelfPrice, String sellingUnit) {
        for (JsonNode tag : tags) {
            if (!MEMBER_PRICE_TAG.equals(tag.path("type").asText())) {
                continue;
            }
            JsonNode inputs = tag.path("decisionInputs");
            JsonNode cents = inputs.path("promotionalPrice");
            String unit = inputs.path("uom").asText("");
            if (!cents.isNumber() || (!unit.isEmpty() && !sellingUnit.isEmpty() && !unit.equalsIgnoreCase(sellingUnit))) {
                continue;
            }
            BigDecimal memberPrice = new BigDecimal(cents.asText()).movePointLeft(2);
            if (memberPrice.signum() > 0 && memberPrice.compareTo(shelfPrice) < 0) {
                return memberPrice;
            }
        }
        return null;
    }

    /** Member prices on Woolworths are Everyday Rewards prices. */
    @Override
    public LoyaltyProgram getLoyaltyProgram() {
        return LoyaltyProgram.EVERYDAY_REWARDS;
    }

    /**
     * Works out a size in the same style as the Foodstuffs sites ("750g", "1.2kg", "2l", "12 x 250ml",
     * "12pk", "kg" for by-weight items, "ea" when no size is given).
     * @param productName the full product name, which is the only place Woolworths states a pack size
     * @param unitOfMeasure the first variant's unit, "KG" for items priced by weight
     * @return the size, at most 20 characters
     */
    static String extractSize(String productName, String unitOfMeasure) {
        // priced per kg (loose produce, meat); the "Min Order 250g" in the name is not the pack size
        if ("KG".equalsIgnoreCase(unitOfMeasure)) {
            return "kg";
        }

        String size = null;
        Matcher matcher = SIZE_PATTERN.matcher(productName);
        while (matcher.find()) { // keep the last match: the pack size is usually at the end of the name
            String unit = matcher.group(3).toLowerCase();
            if (unit.startsWith("lit") || unit.equals("ltr")) {
                unit = "l";
            }
            String amount = matcher.group(2) + unit;
            size = matcher.group(1) == null ? amount : matcher.group(1) + " x " + amount;
        }

        if (size == null) {
            Matcher packMatcher = PACK_PATTERN.matcher(productName);
            while (packMatcher.find()) {
                size = packMatcher.group(1) + "pk";
            }
        }
        if (size == null) {
            return "ea";
        }
        return size.length() > MAX_SIZE_LENGTH ? size.substring(0, MAX_SIZE_LENGTH) : size;
    }

    // ------------------------------------------------------------------ dietary tags

    /**
     * Runs the Vegan / Vegetarian / Organic filtered searches for a category and records which of the
     * given products appear in them. Each filter stops as soon as every product has been seen, after
     * MAX_DIETARY_PAGES pages, or at the last page. Runs once per category: the tags don't vary by store.
     */
    private void findDietaryTags(String categoryKey, List<ProductInfo> products) {
        if (products.isEmpty() || !dietaryCheckedCategories.add(categoryKey)) {
            return;
        }

        Set<String> skus = new HashSet<>();
        for (ProductInfo product : products) {
            skus.add(skuOf(product.productId()));
        }

        for (Map.Entry<String, Dietary> facet : DIETARY_FACETS.entrySet()) {
            Set<String> remaining = new HashSet<>(skus);
            for (int page = 0; page < MAX_DIETARY_PAGES && !remaining.isEmpty(); page++) {
                randomRequestDelay();
                JsonNode productsNode = searchPage(categoryKey, page, facet.getKey());
                if (productsNode == null) {
                    break;
                }
                for (JsonNode result : productsNode.path("results")) {
                    String sku = result.path("sku").asText("");
                    if ("ProductSummary".equals(result.path("__typename").asText()) && remaining.remove(sku)) {
                        dietaryBySku.computeIfAbsent(sku, key -> EnumSet.noneOf(Dietary.class)).add(facet.getValue());
                    }
                }
                if (page + 1 >= productsNode.path("totalPages").asInt(0)) {
                    break;
                }
            }
        }
    }

    private static String skuOf(String productId) {
        int slash = productId.indexOf('/');
        return slash < 0 ? productId : productId.substring(0, slash);
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
        Set<Dietary> productDietary = EnumSet.noneOf(Dietary.class);
        productDietary.addAll(dietaryBySku.getOrDefault(skuOf(productId), Set.of()));

        // "_rsc" asks Next.js for the page's data instead of html: the product details as plain JSON
        HttpRequest detailRequest = HttpRequest.newBuilder()
            .uri(URI.create(PRODUCT_DETAIL_ADDRESS + productId + "?_rsc=1"))
            .headers(HttpUtils.getGenericHeaders())
            .header("Accept", "*/*")
            .header("RSC", "1")
            .GET()
            .build();
        Optional<HttpResponse<String>> responseOptional = send(detailRequest, HttpResponse.BodyHandlers.ofString());
        if (responseOptional.isEmpty() || responseOptional.get().statusCode() >= 400) {
            System.err.println("Product details for Woolworths product " + productId + " unavailable, allergens unknown.");
            return Pair.of(EnumSet.noneOf(Allergen.class), productDietary);
        }

        String body = responseOptional.get().body();
        String ingredients = readStringAfterKey(body, "ingredients");
        String allergenStatement = readStringAfterKey(body, "allergenContained");
        if (ingredients == null && allergenStatement == null && !body.contains("\"ingredients\"")) {
            System.err.println("No ingredient data found for Woolworths product " + productId + ". Page format change?");
        }

        String allergenText = (ingredients == null ? "" : ingredients) + " "
            + (allergenStatement == null ? "" : allergenStatement);
        return Pair.of(matchAllergens(allergenText), productDietary);
    }

    /**
     * Finds the first {@code "key":} in the text and reads the JSON value after it.
     * The product-page payload is not one JSON document, but the product's fields are plain JSON inside it.
     * @return the string value, or null if the key is absent or its value is null / not a string
     */
    String readStringAfterKey(String text, String key) {
        String marker = "\"" + key + "\":";
        int index = text.indexOf(marker);
        if (index < 0) {
            return null;
        }
        try (JsonParser parser = mapper.getFactory().createParser(text.substring(index + marker.length()))) {
            JsonToken token = parser.nextToken();
            return token == JsonToken.VALUE_STRING ? parser.getText() : null;
        } catch (IOException exc) {
            return null;
        }
    }
}
