package nz.ac.auckland.grocerfy.scraper;

import java.net.URI;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.http.HttpResponse.BodyHandler;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ThreadLocalRandom;
import java.util.regex.Pattern;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.springframework.data.util.Pair;

import com.fasterxml.jackson.databind.ObjectMapper;

import nz.ac.auckland.grocerfy.dto.ProductInfo;
import nz.ac.auckland.grocerfy.model.Allergen;
import nz.ac.auckland.grocerfy.model.Dietary;
import nz.ac.auckland.grocerfy.util.HttpUtils;

public abstract class SupermarketScraper {

    protected final ObjectMapper mapper = new ObjectMapper();

    /** Maximum catalogue pages to read per link, for scrapers whose links are paginated. */
    protected int maxPages = 1;

    /** Random delay range (ms) between requests a scraper makes on its own, e.g. paging. */
    protected long requestDelayMin = 0;
    protected long requestDelayMax = 0;

    /**
     * Sets up necessary boilerplate data to be able to properly access the website without
     * getting 401s or 403s. For example, getting an auth token to use in all subsequent requests.
     */
    public abstract void setupCookies();

    /**
     * Refreshes the cookies as required. For cases of authentication, they typically have a
     * short-spanned expiry, where scraping may not be completed in time, therefore a refresh
     * is required.
     */
    public abstract void refreshCookies();

    /**
     * A wrapper function to HttpUtils.sendHttpRequest that on receiving an error message,
     * first attempts to refreshCookies; if still fails, throws an error (unlikely future attempts
     * will work, meaning scraping must end).
     * @param <T> The expected return type of the HttpResponse body content
     * @param request The HttpRequest object we send
     * @param handler The HttpResponse.BodyHandler method to return body content
     * as a specific type
     * @return
     * @see nz.ac.auckland.grocerfy.scraper.SupermarketScraper#refreshCookies()
     * @see nz.ac.auckland.grocerfy.util.HttpUtils#sendHttpRequest()
     */
    public abstract <T> HttpResponse<T> sendRequestWithAuth(HttpRequest request, BodyHandler<T> handler);

    /**
     * Changes the store the scraper is currently on; important because most stores have
     * regional pricing (product costs change from store to store), allowing proper
     * comparison.
     * @param storeData The store-specific data interpretable to change the store (e.g. store code)
     * @return true if the store successfully changes, else false
     */
    public abstract boolean changeStore(String storeData);

    /**
     * Fetches one entry from the "links" list in scrape_targets.json and returns every product on it.
     * The default treats the link as an HTML catalogue page: a GET request, parsed by Jsoup and passed
     * to extractProducts(Document). Scrapers whose catalogues are APIs or span several pages override this.
     * @param link the link (or scraper-specific catalogue identifier) to fetch
     * @return the products found for that link
     */
    public List<ProductInfo> fetchProducts(String link) {
        HttpRequest request = HttpRequest.newBuilder()
            .uri(URI.create(link))
            .headers(HttpUtils.getGenericHeaders())
            .headers(HttpUtils.getGetHeaders())
            .GET()
            .build();

        HttpResponse<String> response = sendRequestWithAuth(request, HttpResponse.BodyHandlers.ofString());
        Document doc = Jsoup.parse(response.body());
        return extractProducts(doc);
    }

    /**
     * Given a catalogue page (many products), extracts the name, size, price, and id
     * of each product, packaging into the ProductInfo DTO.
     * @param pageData the DOM of the product page to scrape
     * @return a list of ProductInfo DTOs, from the provided html page
     */
    public abstract List<ProductInfo> extractProducts(Document pageData);

    /**
     * Accesses extra product info (their own info page) to further see allergen info, ingredients, etc.
     * @param productId the id for the product being examined.
     * @param storeUuid the specific store data/code (foodstuff namely, biased)
     * @return
     */
    public abstract Pair<Set<Allergen>, Set<Dietary>> getProductInfo(String productId, String storeUuid);

    /**
     * Sets how many pages a paginated scraper reads per link. Values below 1 are treated as 1.
     * @param maxPages the page limit per link
     */
    public void setMaxPages(int maxPages) {
        this.maxPages = Math.max(1, maxPages);
    }

    /**
     * Sets the random delay range used between requests the scraper makes by itself (e.g. paging).
     * @param minMillis minimum delay in milliseconds
     * @param maxMillis maximum delay in milliseconds
     */
    public void setRequestDelay(long minMillis, long maxMillis) {
        this.requestDelayMin = minMillis;
        this.requestDelayMax = Math.max(minMillis, maxMillis);
    }

    /**
     * Sleeps for a random time within the configured delay range, as basic rate-limit prevention.
     */
    protected void randomRequestDelay() {
        if (requestDelayMax <= 0) {
            return;
        }
        try {
            Thread.sleep(ThreadLocalRandom.current().nextLong(requestDelayMin, requestDelayMax + 1L));
        } catch (InterruptedException exc) {
            Thread.currentThread().interrupt();
        }
    }

    /**
     * Finds every allergen whose keywords appear as whole words in the given text
     * (an ingredient list, an allergen statement, or both joined together).
     * @param text the text to search; matching is case-insensitive
     * @return the matched allergens, empty if the text is null or blank
     */
    protected Set<Allergen> matchAllergens(String text) {
        Set<Allergen> found = EnumSet.noneOf(Allergen.class);
        if (text == null || text.isBlank()) {
            return found;
        }
        String lowered = text.toLowerCase();
        for (Allergen allergen : Allergen.values()) {
            for (String keyword : allergen.getKeywords()) {
                Pattern keywordPattern = Pattern.compile("\\b" + keyword + "\\b");
                if (keywordPattern.matcher(lowered).find()) {
                    found.add(allergen);
                    break; // check next allergen
                }
            }
        }
        return found;
    }
}
