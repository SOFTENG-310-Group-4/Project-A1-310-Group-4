package nz.ac.auckland.grocerfy.service;


import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.context.annotation.Import;

import nz.ac.auckland.grocerfy.dto.ProductSearchResponse;
import nz.ac.auckland.grocerfy.model.Allergen;
import nz.ac.auckland.grocerfy.model.Dietary;
import nz.ac.auckland.grocerfy.model.Product;
import nz.ac.auckland.grocerfy.repository.ProductRepository;

@DataJpaTest
@Import(ProductSearchService.class)
class ProductSearchServiceTest {
	private static final Set<Dietary> NO_DIETARY = Set.of();
	private static final Set<Allergen> NO_ALLERGENS = Set.of();
	private final Map<String, Product> availableProducts = new HashMap<>();
	@Autowired 
	private ProductRepository productRepository;

	@Autowired
	private ProductSearchService service;

	// HELPER METHODS
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

	private ProductSearchResponse nameToResponse(String name) {
		Product product = availableProducts.get(name);
		return new ProductSearchResponse(
			product.getProductName(),
			product.getSize(),
			product.getAllergens(),
			product.getDietInfo()
		);
	}

	// HELPER METHODS END


	@BeforeEach
	void seed() {
		List<Product> products = productRepository.saveAllAndFlush(List.of(
			new Product("Oat Milk", "500mL", allergens(Allergen.GLUTEN), diets(Dietary.VEGAN, Dietary.VEGETARIAN)),
			new Product("Milk", "2L", allergens(Allergen.DAIRY), diets(Dietary.VEGETARIAN)),
			new Product("Apple", "1kg", allergens(), diets(Dietary.VEGAN, Dietary.VEGETARIAN)),
			new Product("Potato Chips", "250g", allergens(), diets(Dietary.VEGAN, Dietary.VEGETARIAN)),
			new Product("Chicken Nuggets", "1kg", allergens(Allergen.GLUTEN), diets())
		));
		for (Product product : products) {
			availableProducts.put(product.getProductName(), product);
		}
	}


	@Test 
	void nullOrBlankSearchReturnsAllProducts() {
		List<ProductSearchResponse> allProducts = List.of(
			nameToResponse("Milk"),
			nameToResponse("Oat Milk"),
			nameToResponse("Apple"),
			nameToResponse("Potato Chips"),
			nameToResponse("Chicken Nuggets")
		);
		assertThat(service.search(null, NO_DIETARY, NO_ALLERGENS)).containsAll(allProducts);
		assertThat(service.search("", NO_DIETARY, NO_ALLERGENS)).containsAll(allProducts);
	}

	@Test 
	void searchTrimsWhitespace() {
		List<ProductSearchResponse> allProducts = List.of(
			nameToResponse("Milk"),
			nameToResponse("Oat Milk"),
			nameToResponse("Apple"),
			nameToResponse("Potato Chips"),
			nameToResponse("Chicken Nuggets")
		);
		assertThat(service.search("     ", NO_DIETARY, NO_ALLERGENS)).containsAll(allProducts);
		assertThat(service.search("     milk    ", NO_DIETARY, NO_ALLERGENS)).containsAll(
			List.of(nameToResponse("Milk"), nameToResponse("Oat Milk"))
		);
	}

	@Test
	void expectedProductsFromNoQueryButFilter() {
		assertThat(service.search("", Set.of(Dietary.VEGAN), NO_ALLERGENS)).containsAll(
			List.of(nameToResponse("Apple"), nameToResponse("Oat Milk"), nameToResponse("Potato Chips"))
		);
	}

	@Test 
	void unmatchedQueryReturnsNoProducts() {
		assertThat(service.search("unmatchable strings", NO_DIETARY, NO_ALLERGENS)).isEmpty();
	}

	@Test
	void unmatchedFilterReturnsNoProducts() {
		assertThat(service.search("", Set.of(Dietary.ORGANIC), Set.of(Allergen.EGG))).isEmpty();
	}

	@Test
	void wildCardQueryNulled() {
		assertThat(service.search("mi_l%k", NO_DIETARY, NO_ALLERGENS)).containsAll(
			List.of(nameToResponse("Milk"), nameToResponse("Oat Milk"))
		);
	}
}

