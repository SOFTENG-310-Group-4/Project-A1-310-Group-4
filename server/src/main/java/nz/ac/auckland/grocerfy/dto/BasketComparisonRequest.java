package nz.ac.auckland.grocerfy.dto;

import java.util.List;

public record BasketComparisonRequest(List<BasketItemRequest> items) {
}