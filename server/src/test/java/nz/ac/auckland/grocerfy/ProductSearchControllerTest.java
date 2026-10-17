package nz.ac.auckland.grocerfy;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import nz.ac.auckland.grocerfy.controller.ProductController;
import nz.ac.auckland.grocerfy.dto.ProductSearchResponse;
import nz.ac.auckland.grocerfy.model.Allergen;
import nz.ac.auckland.grocerfy.model.Dietary;
import nz.ac.auckland.grocerfy.service.ProductSearchService;

/**
 * Web layer tests for the product search endpoint.
 *
 * The service is mocked, so these tests only exercise the controller contract: routing, query
 * parameter binding and the JSON response shape.
 */
@WebMvcTest(ProductController.class)
class ProductSearchControllerTest {

	@Autowired
	private MockMvc mockMvc;

	@MockitoBean
	private ProductSearchService productSearchService;

	@Test
	void returnsMatchingProductsAsJson() throws Exception {
		when(productSearchService.search(any(), any(), any())).thenReturn(List.of(
				new ProductSearchResponse(1L, "Anchor Butter", "500g", Set.of(Allergen.DAIRY), Set.of())));

		mockMvc.perform(get("/api/product").param("query", "butter"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$[0].productId").value(1))
				.andExpect(jsonPath("$[0].productName").value("Anchor Butter"))
				.andExpect(jsonPath("$[0].packageSize").value("500g"))
				.andExpect(jsonPath("$[0].allergens[0]").value("DAIRY"));
	}

	@Test
	void bindsDietaryAndAllergenFiltersToTheService() throws Exception {
		when(productSearchService.search(any(), any(), any())).thenReturn(List.of());

		mockMvc.perform(get("/api/product")
						.param("query", "milk")
						.param("dietary", "VEGAN", "VEGETARIAN")
						.param("allergens", "DAIRY"))
				.andExpect(status().isOk());

		verify(productSearchService).search("milk", Set.of(Dietary.VEGAN, Dietary.VEGETARIAN), Set.of(Allergen.DAIRY));
	}

	@Test
	void allParametersAreOptional() throws Exception {
		when(productSearchService.search(any(), any(), any())).thenReturn(List.of());

		mockMvc.perform(get("/api/product"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$").isEmpty());

		verify(productSearchService).search(isNull(), isNull(), isNull());
	}

	@Test
	void rejectsAnUnknownDietaryTag() throws Exception {
		mockMvc.perform(get("/api/product").param("dietary", "NOT_A_TAG"))
				.andExpect(status().isBadRequest());

		verifyNoInteractions(productSearchService);
	}
}
