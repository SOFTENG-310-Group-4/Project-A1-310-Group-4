package nz.ac.auckland.grocerfy.dto;

/**
 * Represents a requested product that is missing from a given store.
 * Includes the requested quantity so the frontend can show what is unavailable.
 */
public record MissingItemResponse(Long productId, String productName, int quantity) {
}