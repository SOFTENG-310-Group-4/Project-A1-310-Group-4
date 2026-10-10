package nz.ac.auckland.grocerfy.service;
 
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
 
import java.math.BigDecimal;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Supplier;
 
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
 
import jakarta.persistence.EntityManager;
import nz.ac.auckland.grocerfy.model.Allergen;
import nz.ac.auckland.grocerfy.model.Dietary;
import nz.ac.auckland.grocerfy.model.Product;
import nz.ac.auckland.grocerfy.model.Store;
import nz.ac.auckland.grocerfy.model.StorePrice;
import nz.ac.auckland.grocerfy.repository.ProductRepository;
import nz.ac.auckland.grocerfy.repository.StorePriceRepository;
import nz.ac.auckland.grocerfy.repository.StoreRepository;
 
@DataJpaTest
class ScraperDatabaseServiceTest {
 
	@Autowired
	private ProductRepository productRepository;
 
	@Autowired
	private StoreRepository storeRepository;
 
	@Autowired
	private StorePriceRepository storePriceRepository;
 
	@Autowired
	private EntityManager entityManager;
 
	private ScraperDatabaseService service;

    private List<Product> productList;
    private List<Store> storeList;
 
	@BeforeEach
	void setUp() {
		service = new ScraperDatabaseService(productRepository, storeRepository, storePriceRepository);
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

    private void setupProducts() {
        productList = productRepository.saveAllAndFlush(List.of(
            new Product("Oat Milk", "1L", allergens(Allergen.GLUTEN), diets(Dietary.VEGAN, Dietary.VEGETARIAN)),
            new Product("Whole Milk", "2L", allergens(Allergen.DAIRY), diets(Dietary.VEGETARIAN)),
            new Product("Apple", "1kg", allergens(), diets(Dietary.VEGAN, Dietary.VEGETARIAN))));
    }

    private void setupStores() {
        storeList = storeRepository.saveAllAndFlush(List.of(
            new Store("New World Albany", "Albany", "1 Example St", "uuid-1"),
            new Store("Paknsave Gore", "Gore", "123 Example Road", "paknsave-uuid")
        ));
    }
 
	private StorePrice newPrice(String amount) {
		return new StorePrice(productList.get(0), storeList.get(0), new BigDecimal(amount));
	}
 
	@Test
	void databaseIsNewOnlyWhileProductsTableIsEmpty() {
		assertTrue(service.isDatabaseNew());
		setupProducts();
		assertFalse(service.isDatabaseNew());
	}
 
	@Test
	void addStorePersistsNewStoreAndReusesExistingOne() {
		Store first = service.addStore("New World Albany", "Albany", "1 Example St", "uuid-1");
		Store second = service.addStore("New World Albany", "Albany", "1 Example St", "uuid-1");
 
		assertSame(first, second);
		assertEquals(1, storeRepository.count());
	}
 
	@Test
	void sameNameAndSizeIsOnlyCreatedOnce() {
		AtomicInteger supplierCalls = new AtomicInteger();
		Supplier<Product> supplier = () -> {
			supplierCalls.incrementAndGet();
			return new Product("Milk", "2L", allergens(), diets());
		};
 
		Product first = service.createOrGetProduct("Milk", "2L", supplier);
		Product second = service.createOrGetProduct("Milk", "2L", supplier);
 
		assertSame(first, second);
		assertEquals(1, supplierCalls.get());
		assertEquals(1, productRepository.count());
	}
 
	@Test
	void generateProductCacheStopsExistingProductsBeingCreatedAgain() {
		// simulates an app restart: products already in the DB, service cache empty
		setupProducts();
		entityManager.clear();

        Product oatMilk = productList.get(0);
 
		service.generateProductCache();
		Product found = service.createOrGetProduct(oatMilk.getProductName(), oatMilk.getSize(),
				() -> {
					throw new AssertionError("existing product should have come from the cache");
				});
 
		assertEquals(oatMilk.getId(), found.getId());
		assertEquals(3, productRepository.count());
	}
 
	@Test
	void saveProductPricePersistsNewPrice() {
		setupProducts();
        setupStores();
 
		service.saveProductPrice(newPrice("4.99"));
		entityManager.flush();
 
		assertEquals(1, storePriceRepository.count());
	}
 
	@Test
	void equalPricesFromSeparateObjectsAreWrittenOnce() {
		setupProducts();
        setupStores();
 
		service.saveProductPrice(newPrice("4.99"));
		service.saveProductPrice(newPrice("4.99"));
		entityManager.flush();
 
		assertEquals(1, storePriceRepository.count());
	}
 
	@Test
	void clearPricesWipesTableAndLetsSamePricesBeSavedAgain() {
		setupProducts();
        setupStores();
		service.saveProductPrice(newPrice("4.99"));
		entityManager.flush();
 
		service.clearPrices();
		assertEquals(0, storePriceRepository.count());
 
		service.saveProductPrice(newPrice("4.99"));
		entityManager.flush();
 
		assertEquals(1, storePriceRepository.count());
	}
}