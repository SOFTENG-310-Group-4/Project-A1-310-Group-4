package nz.ac.auckland.grocerfy.service;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import nz.ac.auckland.grocerfy.dto.ProductSearchResponse;
import nz.ac.auckland.grocerfy.model.Product;
import nz.ac.auckland.grocerfy.repository.ProductRepository;

/**
 * Service backing the product search endpoint. It is responsible for querying the repository and mapping the results onto the response DTO. 
 * It also validates and normalises the caller's dietary tag names.
 */
@Service
public class ProductSearchService {

	private static final String LACTOSE_FREE = "lactosefree";
	private static final String GLUTEN_FREE = "glutenfree";
	private static final String VEGETARIAN = "vegetarian";
	private static final String VEGAN = "vegan";

	private static final Set<String> SUPPORTED_TAGS = Set.of(LACTOSE_FREE, GLUTEN_FREE, VEGETARIAN, VEGAN);

	private final ProductRepository productRepository;

	public ProductSearchService(ProductRepository productRepository) {
		this.productRepository = productRepository;
	}

	/**
	 * Find products whose name contains the given query and which carry every one
	 * of the requested dietary tags.
	 * @param query   substring to match against the product name, case-insensitive
	 * @param dietary dietary tag names, matched products must carry all of them
	 * @return matching products ordered by name
	 */
	@Transactional(readOnly = true)
	public List<ProductSearchResponse> search(String query, List<String> dietary) {
		Set<String> tags = normaliseTags(dietary);

		String pattern = (query == null || query.isBlank())
				? "%"
				: "%" + query.trim().toLowerCase() + "%";

		return productRepository.search(
				pattern,
				tags.contains(LACTOSE_FREE),
				tags.contains(GLUTEN_FREE),
				tags.contains(VEGETARIAN),
				tags.contains(VEGAN))
				.stream()
				.map(this::toResponse)
				.toList();
	}

	/**
	 * Normalise supplied tag names by lowercasing and stripping separators, so
	 * "gluten-free", "gluten_free" and "glutenFree" are all accepted.
	 */
	private Set<String> normaliseTags(List<String> dietary) {
		Set<String> tags = new HashSet<>();
		if (dietary == null) {
			return tags;
		}

		for (String raw : dietary) {
			if (raw == null || raw.isBlank()) {
				continue;
			}
			String tag = raw.trim().toLowerCase().replaceAll("[^a-z]", "");
			if (!SUPPORTED_TAGS.contains(tag)) {
				throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
						"Unknown dietary tag: '" + raw.trim() + "'. Supported tags are: "
								+ "lactose_free, gluten_free, vegetarian, vegan.");
			}
			tags.add(tag);
		}
		return tags;
	}

	private ProductSearchResponse toResponse(Product product) {
		return new ProductSearchResponse(
				product.getProductId(),
				product.getName(),
				product.getDisplayName(),
				product.getBrand(),
				product.getCategory(),
				product.getPackageSize(),
				product.isLactoseFree(),
				product.isGlutenFree(),
				product.isVegetarian(),
				product.isVegan());
	}
}