package nz.ac.auckland.grocerfy.dto;

import java.util.List;

/**
 * Request payload for recipe suggestions.
 *
 * @param items basket items to base the suggestion on;
 * @param dietaryTags optional dietary tags (e.g. "vegetarian", "gluten-free")
 */
public record RecipeSuggestionRequest(List<BasketItemRequest> items, List<String> dietaryTags) {}
