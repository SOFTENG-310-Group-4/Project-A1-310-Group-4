package nz.ac.auckland.grocerfy.scraper.woolworths;

import java.net.URI;
import java.net.http.HttpRequest;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import nz.ac.auckland.grocerfy.util.HttpUtils;

/**
 * Builds every request the Woolworths scraper sends: the GraphQL store change and product search, and
 * the product details page. Everything about the shape of Woolworths' API (endpoints, headers, query
 * text, variables) lives here, so an API change is a change to this class only.
 */
final class WoolworthsRequests {
    static final String SITE_ADDRESS = "https://www.woolworths.co.nz";
    static final String GRAPHQL_ENDPOINT = SITE_ADDRESS + "/api/graphql?op-name=";
    static final String PRODUCT_DETAIL_ADDRESS = SITE_ADDRESS + "/shop/product-details/";

    static final String SET_STORE_OPERATION = "SetCartShoppingMode";
    static final String SEARCH_OPERATION = "ProductSearch";

    /** Products per search page (the site's own page size). */
    static final int PAGE_SIZE = 36;

    private static final String SET_STORE_QUERY =
        "mutation SetCartShoppingMode($setCartShoppingModeInput: SetCartShoppingModeInput!) {"
        + " setCartShoppingMode(input: $setCartShoppingModeInput) {"
        + " shoppingMode { mode pickupLocationId pickupLocation { name } } } }";

    // only the fields the scraper uses (the website's own query is ~7KB)
    private static final String SEARCH_QUERY =
        "query ProductSearch($searchInput: CompositeSearchInput!) {"
        + " My { products(searchInput: $searchInput) { totalPages currentPage"
        + " results { __typename ... on ProductSummary {"
        + " sku productName slug storeKey"
        + " variants { unitOfMeasure variantPrice { sellingPrice sellingUnit } }"
        + " tags { type userSegmentId decisionInputs } } } } } }"; // tags carry Everyday Rewards member prices

    private static final String[] GRAPHQL_HEADERS = {
        "Accept", "application/graphql-response+json,application/json;q=0.9",
        "Content-Type", "application/json",
        "Origin", SITE_ADDRESS,
        "Referer", SITE_ADDRESS + "/",
        "Sec-Fetch-Dest", "empty",
        "Sec-Fetch-Mode", "cors",
        "Sec-Fetch-Site", "same-origin"
    };

    private static final ObjectMapper MAPPER = new ObjectMapper();

    private WoolworthsRequests() {
    }

    /**
     * Selects a pickup store for the guest session, which decides the product range and prices returned.
     * @param pickupLocationId the store's pickup location id (the "region_cookie" in scrape_targets.json)
     */
    static HttpRequest setStore(String pickupLocationId) {
        ObjectNode input = MAPPER.createObjectNode();
        input.put("pickupLocationId", pickupLocationId);
        input.put("shoppingMode", "Pickup");
        ObjectNode variables = MAPPER.createObjectNode();
        variables.set("setCartShoppingModeInput", input);
        return graphql(SET_STORE_OPERATION, SET_STORE_QUERY, variables);
    }

    /**
     * One page of a category, optionally narrowed by a search filter ("facet") such as "isVegan".
     * @param categoryKey a category key from scrape_targets.json, e.g. "9-BDB6545B"
     * @param pageIndex the page, starting at 0
     * @param facetKey a filter to apply, or null for the whole category
     */
    static HttpRequest productSearch(String categoryKey, int pageIndex, String facetKey) {
        ObjectNode byCategory = MAPPER.createObjectNode();
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
        ObjectNode searchInput = MAPPER.createObjectNode();
        searchInput.set("byCategoryKey", byCategory);
        ObjectNode variables = MAPPER.createObjectNode();
        variables.set("searchInput", searchInput);
        return graphql(SEARCH_OPERATION, SEARCH_QUERY, variables);
    }

    /**
     * A product's details page. "_rsc" asks Next.js for the page's data instead of html, which holds the
     * product details as plain JSON.
     * @param productId "{sku}/{slug}", as produced by WoolworthsProductParser
     */
    static HttpRequest productDetails(String productId) {
        return HttpRequest.newBuilder()
            .uri(URI.create(PRODUCT_DETAIL_ADDRESS + productId + "?_rsc=1"))
            .headers(HttpUtils.getGenericHeaders())
            .header("Accept", "*/*")
            .header("RSC", "1")
            .GET()
            .build();
    }

    private static HttpRequest graphql(String operation, String query, JsonNode variables) {
        ObjectNode body = MAPPER.createObjectNode();
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
}
