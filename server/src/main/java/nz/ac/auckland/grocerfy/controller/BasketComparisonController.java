package nz.ac.auckland.grocerfy.controller;

import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import nz.ac.auckland.grocerfy.dto.BasketComparisonRequest;
import nz.ac.auckland.grocerfy.dto.BasketComparisonResponse;
import nz.ac.auckland.grocerfy.service.BasketComparisonService;

/**
 * REST controller for basket comparison operations.
 * The frontend sends a JSON basket payload and the backend returns store availability,
 * missing items, and the cheapest available option.
 */
@RestController
@RequestMapping("/api")
@CrossOrigin(origins = "http://localhost:5173")
public class BasketComparisonController {

	private final BasketComparisonService basketComparisonService;

	public BasketComparisonController(BasketComparisonService basketComparisonService) {
		this.basketComparisonService = basketComparisonService;
	}

	/**
	 * Compare a basket across all stores.
	 *
	 * @param request JSON payload containing requested product IDs and quantities
	 * @return store comparison results including availability, missing items, and cheapest options
	 */
	@PostMapping("/basket/compare")
	public BasketComparisonResponse compareBasket(@RequestBody BasketComparisonRequest request) {
		return basketComparisonService.compareBasket(request);
	}
}
