package nz.ac.auckland.grocerfy.controller;

import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import nz.ac.auckland.grocerfy.dto.BasketComparisonRequest;
import nz.ac.auckland.grocerfy.dto.BasketComparisonResponse;
import nz.ac.auckland.grocerfy.service.BasketComparisonService;

@RestController
@RequestMapping("/api")
@CrossOrigin(origins = "http://localhost:5173")
public class BasketComparisonController {

	private final BasketComparisonService basketComparisonService;

	public BasketComparisonController(BasketComparisonService basketComparisonService) {
		this.basketComparisonService = basketComparisonService;
	}

	@PostMapping("/basket/compare")
	public BasketComparisonResponse compareBasket(@RequestBody BasketComparisonRequest request) {
		return basketComparisonService.compareBasket(request);
	}
}