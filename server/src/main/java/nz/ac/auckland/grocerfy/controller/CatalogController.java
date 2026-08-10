package nz.ac.auckland.grocerfy.controller;

import java.util.List;

import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import nz.ac.auckland.grocerfy.dto.ProductSummaryResponse;
import nz.ac.auckland.grocerfy.repository.ProductRepository;

@RestController
@RequestMapping("/api")
@CrossOrigin(origins = "http://localhost:5173")
public class CatalogController {

	private final ProductRepository productRepository;

	public CatalogController(ProductRepository productRepository) {
		this.productRepository = productRepository;
	}

	@GetMapping("/products")
	public List<ProductSummaryResponse> getProducts() {
		return productRepository.findAll().stream()
				.map(product -> new ProductSummaryResponse(product.getProductId(), product.getName(), product.getDisplayName()))
				.toList();
	}
}