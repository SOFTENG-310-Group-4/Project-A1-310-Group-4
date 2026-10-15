package nz.ac.auckland.grocerfy.service;

import java.util.List;
import java.util.Locale;
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

  /** Returned instead of the model's reply when the recipe uses nothing from the basket. */
  static final String NO_FOOD_MESSAGE = "No recipe available: your basket has no food items.";

  private static final String NOT_IN_BASKET = "(NOT IN BASKET)";

  /**
   * Words in a product name that mark it as not food. Matched as whole words, ignoring case, so
   * "pet" does not match "Petite" and "dog food" does not match "Hot Dog Rolls". Keep terms
   * clear-cut: a term that appears in real food names would hide that food from the recipe.
   */
  private static final List<String> NON_FOOD_TERMS =
      List.of(
          // pets
          "cat food",
          "dog food",
          "cat treats",
          "dog treats",
          "kitten",
          "puppy",
          "pet",
          "litter",
          // toiletries and personal care
          "toothpaste",
          "toothbrush",
          "toothbrushes",
          "mouthwash",
          "shampoo",
          "conditioner",
          "soap",
          "body wash",
          "deodorant",
          "nappies",
          // cleaning
          "laundry",
          "detergent",
          "dishwashing",
          "dishwasher",
          "bleach",
          "cleaner",
          "disinfectant",
          "fabric softener",
          // household
          "foil",
          "baking paper",
          "cling wrap",
          "rubbish bags");

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

    // Non-food items are removed before the model sees them, because models will happily
    // turn cat food or toothpaste into a recipe whatever the prompt says.
    List<Product> foodProducts =
        products.stream().filter(product -> !isNonFood(product.getProductName())).toList();
    if (foodProducts.isEmpty()) {
      return NO_FOOD_MESSAGE;
    }

    String ingredients =
        foodProducts.stream().map(Product::getProductName).collect(Collectors.joining(", "));

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
    String recipe = response.content();
    // Models often ignore the prompt's "no food" rule and invent a recipe made only of added
    // ingredients (e.g. for a basket of toothpaste and soap), so that case is checked here.
    if (onlyUsesAddedIngredients(recipe)) {
      return NO_FOOD_MESSAGE;
    }
    return recipe;
  }

  /**
   * Checks whether a product name contains one of the non-food terms as whole words.
   *
   * @param productName the product's name; may be null
   * @return true if the product is a known non-food item
   */
  static boolean isNonFood(String productName) {
    if (productName == null) {
      return false;
    }
    // pad with spaces and turn anything that isn't a letter into a space, so terms only match
    // whole words
    String words = " " + productName.toLowerCase(Locale.ROOT).replaceAll("[^a-z]+", " ") + " ";
    return NON_FOOD_TERMS.stream().anyMatch(term -> words.contains(" " + term + " "));
  }

  /**
   * Checks whether a recipe's ingredients list has items but every one is marked (NOT IN BASKET),
   * meaning none of the basket was used. A reply without an ingredients list (for example the
   * prompt's own no-food line) is left alone.
   *
   * @param recipe the model's plain-text reply
   * @return true if the ingredients list exists and contains only added ingredients
   */
  static boolean onlyUsesAddedIngredients(String recipe) {
    if (recipe == null) {
      return false;
    }
    boolean inIngredients = false;
    int ingredientCount = 0;
    for (String rawLine : recipe.split("\\R")) {
      String line = rawLine.strip();
      String heading = line.replaceAll("[#*:]", "").strip().toLowerCase(Locale.ROOT);
      if (heading.equals("ingredients list") || heading.equals("ingredients")) {
        inIngredients = true;
      } else if (heading.equals("steps")) {
        break;
      } else if (inIngredients && isListItem(line)) {
        ingredientCount++;
        String item = line.substring(1).strip().toUpperCase(Locale.ROOT);
        if (!item.startsWith(NOT_IN_BASKET)) {
          return false;
        }
      }
    }
    return ingredientCount > 0;
  }

  private static boolean isListItem(String line) {
    return line.startsWith("-") || line.startsWith("*") || line.startsWith("\u2022");
  }

  private String renderPrompt(String ingredients, String dietary) {
    return promptTemplate.replace("{{ingredients}}", ingredients).replace("{{dietary}}", dietary);
  }
}
