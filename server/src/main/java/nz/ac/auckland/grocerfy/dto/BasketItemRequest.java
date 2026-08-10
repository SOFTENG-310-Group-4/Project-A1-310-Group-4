package nz.ac.auckland.grocerfy.dto;

/**
 * A single item requested in the shopping basket.
 * Contains the product ID and requested quantity for backend comparison.
 */
public record BasketItemRequest(Long productId, int quantity) {
}