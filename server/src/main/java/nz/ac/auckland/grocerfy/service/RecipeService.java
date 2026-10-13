package nz.ac.auckland.grocerfy.service;

import java.util.List;
import java.util.stream.Collectors;
import nz.ac.auckland.grocerfy.apiproxy.ApiProxyClient;
import nz.ac.auckland.grocerfy.apiproxy.ApiProxyPromptLoader;
import nz.ac.auckland.grocerfy.apiproxy.ApiProxyRequest;
import nz.ac.auckland.grocerfy.apiproxy.ApiProxyResponse;
import nz.ac.auckland.grocerfy.dto.RecipeSuggestionRequest;
import nz.ac.auckland.grocerfy.model.Product;
import nz.ac.auckland.grocerfy.repository.ProductRepository;
import org.springframework.stereotype.Service;

/** Builds a recipe suggestion prompt from a basket and calls the API proxy. */
@Service
public class RecipeService {

  private static final String PROMPT_RESOURCE = "/prompts/recipe-prompt.txt";

  private final ProductRepository productRepository;
  private final ApiProxyClient apiProxyClient;
  private final String promptTemplate;

  public RecipeService(ProductRepository productRepository, ApiProxyClient apiProxyClient) {
    this.productRepository = productRepository;
    this.apiProxyClient = apiProxyClient;
    this.promptTemplate = ApiProxyPromptLoader.load(PROMPT_RESOURCE);
  }

  /**
   * Suggests a recipe based on the supplied basket and dietary tags.
   *
   * @param request basket and optional dietary tags
   * @return plain-text recipe suggestion
   * @throws IllegalArgumentException if the basket is missing or empty
   */
  public String suggest(RecipeSuggestionRequest request) {
    if (request == null || request.items() == null || request.items().isEmpty()) {
      throw new IllegalArgumentException("Basket must contain at least one item.");
    }

    List<Long> ids = request.items().stream().map(item -> item.productId()).toList();

    List<Product> products = productRepository.findAllById(ids);
    if (products.isEmpty()) {
      throw new IllegalArgumentException("No matching products found in the catalogue.");
    }

    String ingredients =
        products.stream().map(Product::getProductName).collect(Collectors.joining(", "));

    List<String> rawTags = request.dietaryTags();
    List<String> tags;
    if (rawTags == null) {
      tags = List.of();
    } else {
      tags = rawTags;
    }

    String dietary;
    if (tags.isEmpty()) {
      dietary = "none";
    } else {
      dietary = String.join(", ", tags);
    }

    String prompt = renderPrompt(ingredients, dietary);

    ApiProxyResponse response = apiProxyClient.send(new ApiProxyRequest(prompt));
    return response.content();
  }

  private String renderPrompt(String ingredients, String dietary) {
    return promptTemplate.replace("{{ingredients}}", ingredients).replace("{{dietary}}", dietary);
  }
}
