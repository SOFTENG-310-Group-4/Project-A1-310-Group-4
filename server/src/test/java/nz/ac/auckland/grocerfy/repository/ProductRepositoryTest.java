package nz.ac.auckland.grocerfy.repository;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.EnumSet;
import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;

import nz.ac.auckland.grocerfy.model.Allergen;
import nz.ac.auckland.grocerfy.model.Dietary;
import nz.ac.auckland.grocerfy.model.Product;

/**
 * Tests the search query in isolation using fixtures created by the test itself.
 */
@DataJpaTest 
class ProductRepositoryTest {
	private static final Set<Dietary> NO_DIETARY = Set.of();
	private static final Set<Allergen> NO_ALLERGENS = Set.of();

	
	@Autowired 
	private ProductRepository productRepository;

	// helper methods abstracting batch commands
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

	private List<String> names(List<Product> products) {
		return products.stream().map(Product::getProductName).toList();
	}


	@BeforeEach
	void seed() {
		productRepository.saveAllAndFlush(List.of(
				new Product("Oat Milk", "1L", allergens(Allergen.GLUTEN), diets(Dietary.VEGAN, Dietary.VEGETARIAN)),
				new Product("Whole Milk", "2L", allergens(Allergen.DAIRY), diets(Dietary.VEGETARIAN)),
				new Product("Apple", "1kg", allergens(), diets(Dietary.VEGAN, Dietary.VEGETARIAN))));
	}

	@Test
	void noFiltersReturnsEverythingOrderedByName() {
		// also proves the query copes with empty IN-lists
		assertThat(names(productRepository.search("%", NO_DIETARY, 0, NO_ALLERGENS, 0)))
				.containsExactly("Apple", "Oat Milk", "Whole Milk");
	}
 
	@Test
	void searchCaseInsensitive() {
		assertThat(names(productRepository.search("%milk%", NO_DIETARY, 0, NO_ALLERGENS, 0)))
				.containsExactly("Oat Milk", "Whole Milk");
		assertThat(names(productRepository.search("%MILK%", NO_DIETARY, 0, NO_ALLERGENS, 0)))
				.containsExactly("Oat Milk", "Whole Milk");
	}
 
	@Test
	void dietaryFilterRequiresEveryRequestedTag() {
		// Whole Milk is vegetarian but not vegan, so it must be excluded when both are required
		assertThat(names(productRepository.search("%", diets(Dietary.VEGAN, Dietary.VEGETARIAN), 2, NO_ALLERGENS, 0)))
				.containsExactly("Apple", "Oat Milk");
		assertThat(names(productRepository.search("%", diets(Dietary.VEGETARIAN), 1, NO_ALLERGENS, 0)))
				.containsExactly("Apple", "Oat Milk", "Whole Milk");
	}
 
	@Test
	void allergenFilterExcludesProductsContainingAnyListedAllergen() {
		assertThat(names(productRepository.search("%", NO_DIETARY, 0, allergens(Allergen.DAIRY), 1)))
				.containsExactly("Apple", "Oat Milk");
		assertThat(names(productRepository.search("%", NO_DIETARY, 0, allergens(Allergen.DAIRY, Allergen.GLUTEN), 2)))
				.containsExactly("Apple");
	}
 
	@Test
	void nameDietaryAndAllergenFiltersCombine() {
		assertThat(names(productRepository.search("%milk%", diets(Dietary.VEGAN), 1, allergens(Allergen.DAIRY), 1)))
				.containsExactly("Oat Milk");
	}
}