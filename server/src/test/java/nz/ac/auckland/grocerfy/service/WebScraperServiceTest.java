package nz.ac.auckland.grocerfy.service;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;
import org.springframework.data.util.Pair;
import org.springframework.test.util.ReflectionTestUtils;

import nz.ac.auckland.grocerfy.dto.ProductInfo;
import nz.ac.auckland.grocerfy.model.Allergen;
import nz.ac.auckland.grocerfy.model.Dietary;
import nz.ac.auckland.grocerfy.model.LoyaltyProgram;
import nz.ac.auckland.grocerfy.model.Product;
import nz.ac.auckland.grocerfy.model.StorePrice;
import nz.ac.auckland.grocerfy.repository.ProductRepository;
import nz.ac.auckland.grocerfy.repository.StorePriceRepository;
import nz.ac.auckland.grocerfy.repository.StoreRepository;
import nz.ac.auckland.grocerfy.scraper.SupermarketScraper;

/**
 * Pipeline tests: the real WebScraperService and ScraperDatabaseService write to an in-memory H2,
 * and only the network is faked (a mocked SupermarketScraper). Needs the package-private
 * WebScraperService constructor that takes the scrapers and the delay.
 *
 * Both services are built by hand per test because they keep in-memory state (caches, current scraper).
 * scheduledScrape() and checkAndScrapeOnStartup() are public, so the private executeScraping() is
 * exercised through them.
 */
@DataJpaTest
public class WebScraperServiceTest {

	@Autowired
	private ProductRepository productRepository;

	@Autowired
	private StoreRepository storeRepository;

	@Autowired
	private StorePriceRepository storePriceRepository;

	private ScraperDatabaseService databaseService;
	private SupermarketScraper scraper;

	@BeforeEach
	void setUp() {
		databaseService = new ScraperDatabaseService(productRepository, storeRepository, storePriceRepository);
		scraper = mock(SupermarketScraper.class);
	}

	private static Set<Allergen> allergens(Allergen... values) {
		Set<Allergen> set = EnumSet.noneOf(Allergen.class);
		set.addAll(List.of(values));
		return set;
	}
 
	private static Set<Dietary> diets(Dietary... values) {
		Set<Dietary> set = EnumSet.noneOf(Dietary.class);
		set.addAll(List.of(values));
		return set;
	}

	private List<Product> setupProducts() {
		return productRepository.saveAllAndFlush(List.of(
				new Product("Oat Milk", "1L", allergens(Allergen.GLUTEN), diets(Dietary.VEGAN, Dietary.VEGETARIAN)),
				new Product("Whole Milk", "2L", allergens(Allergen.DAIRY), diets(Dietary.VEGETARIAN)),
				new Product("Apple", "1kg", allergens(), diets(Dietary.VEGAN, Dietary.VEGETARIAN))));
	}

	/**
	 * A scrape_targets.json with one store and link for doc purposes
	 */
	private static Resource stubbedScrapeTargets() {
		String json = """
				[
					{
						"supermarket": "Test store",
						"links": [
							"https://www.someteststore.com/shop/category/weekly-deals"
						],
						"branches": [
							{
								"store_name": "Patternisation",
								"address": "123 Example Road, Example, World 0123",
								"region_cookie": "test-cookie"
							}
						]
					}
				]
				""";
		return new ByteArrayResource(json.getBytes(StandardCharsets.UTF_8));
	}

	private static ProductInfo info(String name, String size, String price, String id) {
		return new ProductInfo(name, new BigDecimal(price), size, id);
	}

	/** Every link the scraper "fetches" yields these products; every product-info lookup returns no tags. */
	private void givenScraperReturns(ProductInfo... products) {
		when(scraper.changeStore(anyString())).thenReturn(true);
		when(scraper.fetchProducts(anyString())).thenReturn(List.of(products));
		when(scraper.getProductInfo(anyString(), anyString())).thenReturn(Pair.of(allergens(), diets()));
	}

	private WebScraperService serviceWith(Resource targets) {
		// delay 0..0 so the tests don't sleep 1-3 seconds per link and product
		WebScraperService service = new WebScraperService(databaseService, Map.of("Test store", scraper), 0, 0);
		// overwrite the Resource scrap_targets.json
		ReflectionTestUtils.setField(service, "targets", targets);
		return service;
	}

	@Test
	void scrapesProductsStoresAndPricesIntoDatabase() {
		givenScraperReturns(info("Milk", "2L", "4.99", "ID-1"), info("Bread", "600g", "2.50", "ID-2"));
		when(scraper.getProductInfo(eq("ID-1"), anyString()))
				.thenReturn(Pair.of(allergens(Allergen.DAIRY), diets(Dietary.VEGETARIAN)));

		serviceWith(stubbedScrapeTargets()).scheduledScrape();

		assertEquals(1, storeRepository.count());
		assertEquals(2, productRepository.count());
		assertEquals(2, storePriceRepository.count());
		// the branch's region_cookie is what getProductInfo receives as the store code
		verify(scraper).getProductInfo("ID-1", "test-cookie");
		Product milk = productRepository.findAll().stream()
				.filter(p -> p.getProductName().equals("Milk")).findFirst().orElseThrow();
		assertTrue(milk.getAllergens().contains(Allergen.DAIRY));
		assertTrue(milk.getDietInfo().contains(Dietary.VEGETARIAN));
	}

	@Test
	void skipsStoreWhenChangeStoreFails() {
		givenScraperReturns(info("Milk", "2L", "4.99", "ID-1"));
		when(scraper.changeStore(anyString())).thenReturn(false);

		serviceWith(stubbedScrapeTargets()).scheduledScrape();

		assertEquals(0, storeRepository.count());
		verify(scraper, never()).fetchProducts(anyString());
	}

	@Test
	void sameProductAcrossStoresIsCreatedOnceWithOnePricePerStore() {
		givenScraperReturns(info("Milk", "2L", "4.99", "ID-1"), info("Bread", "600g", "2.50", "ID-2"));

		String twoStoreRaw = """
				[
					{
						"supermarket": "Test store",
						"links": [
							"https://www.someteststore.com/shop/category/weekly-deals"
						],
						"branches": [
							{
								"store_name": "Patternisation",
								"address": "123 Example Road, Example, World 0123",
								"region_cookie": "test-cookie"
							},
							{
								"store_name": "Sequentialistica",
								"address": "123 Example Cres, Example, World 0123",
								"region_cookie": "test-cookie2"
							}
						]
					}
				]
				""";
		Resource twoStoreFile = new ByteArrayResource(twoStoreRaw.getBytes(StandardCharsets.UTF_8));

		serviceWith(twoStoreFile).scheduledScrape();

		assertEquals(2, storeRepository.count());
		assertEquals(2, productRepository.count());
		assertEquals(4, storePriceRepository.count());
		// product info is fetched once per product, not once per store
		verify(scraper, times(1)).getProductInfo(eq("ID-1"), anyString());
	}

	/** Needs StorePrice.equals()/hashCode(): the same product on two pages must not be priced twice. */
	@Test
	void sameProductOnTwoLinksOfOneStoreIsPricedOnce() {
		givenScraperReturns(info("Milk", "2L", "4.99", "ID-1"), info("Bread", "600g", "2.50", "ID-2"));

		serviceWith(stubbedScrapeTargets()).scheduledScrape();

		assertEquals(2, productRepository.count());
		assertEquals(2, storePriceRepository.count());
	}

	@Test
	void productAlreadyInDatabaseIsReusedNotRequestedAgain() {
		// simulates a rescrape after a restart: the product is already stored
		Product oat = setupProducts().get(0);
		givenScraperReturns(new ProductInfo(oat.getProductName(), new BigDecimal("3.20"), oat.getSize(), "ID-OAT"));

		serviceWith(stubbedScrapeTargets()).scheduledScrape();

		verify(scraper, never()).getProductInfo(eq("ID-OAT"), anyString());
		assertEquals(3, productRepository.count()); // the 3 fixture products, no duplicate
		assertEquals(1, storePriceRepository.count());
	}

	@Test
	void startupScrapeRunsWhenDatabaseIsEmpty() {
		givenScraperReturns(info("Milk", "2L", "4.99", "ID-1"));

		serviceWith(stubbedScrapeTargets()).checkAndScrapeOnStartup();

		verify(scraper).setupCookies();
		assertEquals(1, productRepository.count());
	}

	@Test
	void startupScrapeIsSkippedWhenDatabaseAlreadyHasProducts() {
		setupProducts();

		serviceWith(stubbedScrapeTargets()).checkAndScrapeOnStartup();

		verify(scraper, never()).setupCookies();
	}

	@Test
	void memberPricesAreSavedWithTheScrapersLoyaltyProgram() {
		givenScraperReturns(
				new ProductInfo("Yoghurt", new BigDecimal("4.75"), "750g", "ID-Y", new BigDecimal("4.20")),
				info("Bread", "600g", "3.00", "ID-B"));
		when(scraper.getLoyaltyProgram()).thenReturn(LoyaltyProgram.EVERYDAY_REWARDS);

		serviceWith(stubbedScrapeTargets()).scheduledScrape();

		Map<String, StorePrice> pricesByProduct = storePriceRepository.findAllWithRelations().stream()
				.collect(Collectors.toMap(price -> price.getProduct().getProductName(), price -> price));
		StorePrice yoghurt = pricesByProduct.get("Yoghurt");
		assertEquals(0, new BigDecimal("4.20").compareTo(yoghurt.getMemberPrice()));
		assertEquals(LoyaltyProgram.EVERYDAY_REWARDS, yoghurt.getLoyaltyProgram());
		// no member price: nothing stored for the programme either
		assertNull(pricesByProduct.get("Bread").getMemberPrice());
		assertNull(pricesByProduct.get("Bread").getLoyaltyProgram());
	}

	@Test
	void eachLinkIsFetchedThroughTheScraperWithDefaultPageLimit() {
		givenScraperReturns(info("Milk", "2L", "4.99", "ID-1"));

		serviceWith(stubbedScrapeTargets()).scheduledScrape();

		verify(scraper).fetchProducts("https://www.someteststore.com/shop/category/weekly-deals");
		verify(scraper).setMaxPages(1); // no max_pages in the config
		verify(scraper).setRequestDelay(0, 0);
	}

	@Test
	void maxPagesFromConfigIsPassedToScraper() {
		givenScraperReturns(info("Milk", "2L", "4.99", "ID-1"));
		String json = """
				[
					{
						"supermarket": "Test store",
						"max_pages": 3,
						"links": ["9-BDB6545B"],
						"branches": [
							{
								"store_name": "Patternisation",
								"address": "123 Example Road, Example, World 0123",
								"region_cookie": "9529"
							}
						]
					}
				]
				""";

		serviceWith(new ByteArrayResource(json.getBytes(StandardCharsets.UTF_8))).scheduledScrape();

		verify(scraper).setMaxPages(3);
		verify(scraper, never()).setMaxPages(1);
		verify(scraper).fetchProducts("9-BDB6545B");
		assertEquals(1, productRepository.count());
	}
}