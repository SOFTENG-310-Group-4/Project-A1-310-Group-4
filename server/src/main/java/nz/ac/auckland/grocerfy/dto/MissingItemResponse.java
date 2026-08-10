package nz.ac.auckland.grocerfy.dto;

public record MissingItemResponse(Long productId, String productName, int quantity) {
}