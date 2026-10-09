package nz.ac.auckland.grocerfy.service;

import java.util.List;
import java.util.Set;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import nz.ac.auckland.grocerfy.dto.ProductSearchResponse;
import nz.ac.auckland.grocerfy.model.Allergen;
import nz.ac.auckland.grocerfy.model.Dietary;
import nz.ac.auckland.grocerfy.model.Product;
import nz.ac.auckland.grocerfy.repository.ProductRepository;

/**
 * Service backing the product search endpoint. It is responsible for querying the repository and mapping the results onto the response DTO. 
 * It also validates and normalises the caller's dietary tag names.
 */
@Service
public class ProductSearchService {

	private final ProductRepository productRepository;

	public ProductSearchService(ProductRepository productRepository) {
		this.productRepository = productRepository;
	}

	/**
	 * Finds products whose name contains the given text, which carry every requested dietary tag
	 * and none of the listed allergens.
	 * @param query     substring to match against the product name (case-insensitive); null or blank matches all
	 * @param dietary   dietary tags a product must all carry; null or empty means no dietary filter
	 * @param allergens allergens a product must not contain; null or empty means no allergen filter
	 * @return matching products ordered by name
	 */
	@Transactional(readOnly = true)
	public List<ProductSearchResponse> search(String query, Set<Dietary> dietary, Set<Allergen> allergens) {

		// empty strings are treated as displaying all, otherwise wrap in % wildcard
		String pattern;
		if (query == null || query.isBlank()) {
			pattern = "%";
		} else {
			// first remove all existing wildcards in query
			String noPercent = query.replace("%", "");
			String noWildcards = noPercent.replace("_", "");

			pattern = "%" + noWildcards.trim() + "%";
		}

		Set<Dietary> requiredDietary = dietary == null ? Set.of() : dietary;
		Set<Allergen> excludedAllergens = allergens == null ? Set.of() : allergens;

		return productRepository.search(
				pattern,
				requiredDietary,
				requiredDietary.size(),
				excludedAllergens,
				excludedAllergens.size()
				)
				.stream()
				.map(this::toResponse)
				.toList();
	}

	private ProductSearchResponse toResponse(Product product) {
		return new ProductSearchResponse(
				product.getProductName(),
				product.getSize(),
				product.getAllergens(),
				product.getDietInfo());
	}
}