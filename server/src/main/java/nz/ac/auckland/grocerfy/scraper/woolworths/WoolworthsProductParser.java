package nz.ac.auckland.grocerfy.scraper.woolworths;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import nz.ac.auckland.grocerfy.dto.ProductInfo;

/**
 * Reads Woolworths ProductSearch responses into ProductInfo. Pure functions with no network or state,
 * so every rule about the response format (which results are real products, sizes, member prices) can be
 * tested on its own with a recorded response.
 */
final class WoolworthsProductParser {

    // ProductSearch response fields; only ProductSummary results are real products
    private static final String RESULTS = "results";
    private static final String TYPENAME = "__typename";
    private static final String PRODUCT_SUMMARY = "ProductSummary";

    /** Product tag holding the Everyday Rewards member price, e.g. decisionInputs.promotionalPrice = 529 (cents). */
    private static final String MEMBER_PRICE_TAG = "MemberPrice";

    /**
     * A quantity inside a product name: a number and the word after it, optionally after a multipack count,
     * e.g. "750g", "1.2kg", "2L", "12 x 250mL", "12x250mL", "12 Pack". Woolworths only states sizes in the name
     * (there is no size field in search results). The word is checked against WEIGHT_UNITS / PACK_UNITS in
     * code rather than in the regex, and the quantifiers are possessive, so matching never backtracks.
     */
    private static final Pattern QUANTITY_PATTERN = Pattern.compile(
        "\\b(?:(\\d++)\\s*+x\\s*+)?(\\d++(?:\\.\\d++)?)\\s*+(\\p{Alpha}++)", Pattern.CASE_INSENSITIVE);
    private static final Set<String> WEIGHT_UNITS = Set.of("kg", "g", "ml", "l", "ltr", "litre", "litres");
    /** Pack counts ("12 Pack", "30pk") are only used when the name has no weight or volume. */
    private static final Set<String> PACK_UNITS = Set.of("pk", "pack");
    private static final int MAX_SIZE_LENGTH = 20; // products.size column length

    private static final ObjectMapper MAPPER = new ObjectMapper();

    private WoolworthsProductParser() {
    }

    /**
     * Finds the product list in a ProductSearch response body.
     * @param responseBody the JSON returned by the ProductSearch GraphQL operation
     * @param categoryKey the category searched, for error messages
     * @return the "products" node, or null if the response has errors, no product list, or isn't JSON
     */
    static JsonNode readProductsNode(String responseBody, String categoryKey) {
        try {
            JsonNode root = MAPPER.readTree(responseBody);
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
     * Extracts products from a raw ProductSearch response body.
     * @return the products in it (sponsored ads and recipe banners are skipped), or an empty list if unreadable
     */
    static List<ProductInfo> parseProducts(String responseBody) {
        try {
            return parseProducts(MAPPER.readTree(responseBody).path("data").path("My").path("products"));
        } catch (JsonProcessingException exc) {
            System.err.println("Woolworths product list unreadable: " + exc.getLocalizedMessage());
            return new ArrayList<>();
        }
    }

    /**
     * Extracts products from the "products" node of a ProductSearch response.
     * @return the products in it, in the site's order (sponsored ads and recipe banners are skipped)
     */
    static List<ProductInfo> parseProducts(JsonNode productsNode) {
        List<ProductInfo> products = new ArrayList<>();
        for (JsonNode result : productSummaries(productsNode)) {
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
     * @return the SKUs of the real products on a search page, in order
     */
    static Set<String> skus(JsonNode productsNode) {
        Set<String> skus = new LinkedHashSet<>();
        for (JsonNode result : productSummaries(productsNode)) {
            skus.add(result.path("sku").asText(""));
        }
        return skus;
    }

    /**
     * @return the storeKey of every real product on a search page that has one, i.e. which store's range
     *     and prices were returned
     */
    static List<String> storeKeys(JsonNode productsNode) {
        List<String> storeKeys = new ArrayList<>();
        for (JsonNode result : productSummaries(productsNode)) {
            String storeKey = result.path("storeKey").asText(null);
            if (storeKey != null) {
                storeKeys.add(storeKey);
            }
        }
        return storeKeys;
    }

    /**
     * @return true if the page is the last one of its search
     */
    static boolean isLastPage(JsonNode productsNode, int pageIndex) {
        return pageIndex + 1 >= productsNode.path("totalPages").asInt(0);
    }

    /**
     * @param productId "{sku}/{slug}", as produced by parseProducts()
     * @return the SKU part
     */
    static String skuOf(String productId) {
        int slash = productId.indexOf('/');
        return slash < 0 ? productId : productId.substring(0, slash);
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
        String packSize = null;
        Matcher matcher = QUANTITY_PATTERN.matcher(productName);
        while (matcher.find()) { // keep the last match: the pack size is usually at the end of the name
            String word = matcher.group(3).toLowerCase(Locale.ROOT);
            if (WEIGHT_UNITS.contains(word)) {
                String unit = word.startsWith("l") ? "l" : word; // l, ltr, litre(s) -> l
                String amount = matcher.group(2) + unit;
                size = matcher.group(1) == null ? amount : matcher.group(1) + " x " + amount;
            } else if (PACK_UNITS.contains(word)) {
                packSize = matcher.group(2) + "pk";
            }
        }

        if (size == null) {
            size = packSize;
        }
        if (size == null) {
            return "ea";
        }
        return size.length() > MAX_SIZE_LENGTH ? size.substring(0, MAX_SIZE_LENGTH) : size;
    }

    /**
     * The real products on a search page. SponsoredProduct results are paid ads (often from other
     * categories) and ContentInGridResultItem results are recipe banners.
     */
    private static List<JsonNode> productSummaries(JsonNode productsNode) {
        List<JsonNode> summaries = new ArrayList<>();
        for (JsonNode result : productsNode.path(RESULTS)) {
            if (PRODUCT_SUMMARY.equals(result.path(TYPENAME).asText())) {
                summaries.add(result);
            }
        }
        return summaries;
    }
}
