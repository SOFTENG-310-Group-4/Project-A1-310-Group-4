package nz.ac.auckland.grocerfy.scraper.woolworths;

import java.util.EnumSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import com.fasterxml.jackson.databind.JsonNode;

import nz.ac.auckland.grocerfy.dto.ProductInfo;
import nz.ac.auckland.grocerfy.model.Dietary;

/**
 * Finds dietary tags through the site's own Vegan / Vegetarian / Organic search filters, and remembers
 * them by SKU until the product details are read.
 */
class WoolworthsDietaryTags {

    /** Most filtered pages to read per filter and category. */
    static final int MAX_PAGES = 10;

    /** Search filters ("facets") the site offers, mapped to our Dietary tags. */
    private static final Map<String, Dietary> FACETS = new LinkedHashMap<>();
    static {
        FACETS.put("isVegan", Dietary.VEGAN);
        FACETS.put("isVegetarian", Dietary.VEGETARIAN);
        FACETS.put("isOrganic", Dietary.ORGANIC);
    }

    /** Fetches one filtered search page, or returns null if it could not be read. */
    @FunctionalInterface
    interface PageSearch {
        JsonNode searchPage(String categoryKey, int pageIndex, String facetKey);
    }

    private final PageSearch pageSearch;
    private final Runnable betweenRequests;

    /** SKU -> dietary tags found through the filtered searches. */
    private final Map<String, Set<Dietary>> tagsBySku = new HashMap<>();
    /** Category keys whose filtered searches have already run (dietary tags don't change between stores). */
    private final Set<String> checkedCategories = new HashSet<>();

    /**
     * @param pageSearch fetches the filtered search pages
     * @param betweenRequests run before each request, e.g. a polite random delay
     */
    WoolworthsDietaryTags(PageSearch pageSearch, Runnable betweenRequests) {
        this.pageSearch = pageSearch;
        this.betweenRequests = betweenRequests;
    }

    /**
     * Forgets everything found, e.g. at the start of a scrape run since the range changes over time.
     */
    void clear() {
        tagsBySku.clear();
        checkedCategories.clear();
    }

    /**
     * Runs the filtered searches for a category and records which of the given products appear in them.
     * Each filter stops as soon as every product has been seen, after MAX_PAGES pages, or at the last page.
     * Runs once per category: the tags don't vary by store.
     */
    void findFor(String categoryKey, List<ProductInfo> products) {
        if (products.isEmpty() || !checkedCategories.add(categoryKey)) {
            return;
        }

        Set<String> skus = products.stream()
                .map(product -> WoolworthsProductParser.skuOf(product.productId()))
                .collect(Collectors.toSet());
        for (Map.Entry<String, Dietary> facet : FACETS.entrySet()) {
            findForFacet(categoryKey, facet.getKey(), facet.getValue(), new HashSet<>(skus));
        }
    }

    /**
     * @param productId "{sku}/{slug}"
     * @return the dietary tags found for the product (a new, modifiable set)
     */
    Set<Dietary> tagsFor(String productId) {
        Set<Dietary> tags = EnumSet.noneOf(Dietary.class);
        tags.addAll(tagsBySku.getOrDefault(WoolworthsProductParser.skuOf(productId), Set.of()));
        return tags;
    }

    private void findForFacet(String categoryKey, String facetKey, Dietary tag, Set<String> remaining) {
        for (int page = 0; page < MAX_PAGES && !remaining.isEmpty(); page++) {
            betweenRequests.run();
            JsonNode productsNode = pageSearch.searchPage(categoryKey, page, facetKey);
            if (productsNode == null) {
                return;
            }
            for (String sku : WoolworthsProductParser.skus(productsNode)) {
                if (remaining.remove(sku)) {
                    tagsBySku.computeIfAbsent(sku, key -> EnumSet.noneOf(Dietary.class)).add(tag);
                }
            }
            if (WoolworthsProductParser.isLastPage(productsNode, page)) {
                return;
            }
        }
    }
}
