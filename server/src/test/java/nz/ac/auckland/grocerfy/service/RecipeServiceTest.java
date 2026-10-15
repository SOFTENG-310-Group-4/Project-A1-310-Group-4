package nz.ac.auckland.grocerfy.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Set;
import nz.ac.auckland.grocerfy.apiproxy.ApiProxyClient;
import nz.ac.auckland.grocerfy.apiproxy.ApiProxyRequest;
import nz.ac.auckland.grocerfy.apiproxy.ApiProxyResponse;
import nz.ac.auckland.grocerfy.dto.BasketItemRequest;
import nz.ac.auckland.grocerfy.dto.RecipeSuggestionRequest;
import nz.ac.auckland.grocerfy.model.Product;
import nz.ac.auckland.grocerfy.repository.ProductRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * Unit tests for the recipe suggestion service.
 *
 * <p>The service fetches products, renders a prompt from the classpath template, and forwards it to
 * the API proxy. The proxy and repository are mocked so the tests can assert on the exact prompt
 * text the service builds.
 */
@ExtendWith(MockitoExtension.class)
class RecipeServiceTest {

  private static final String APPLE = "Apple";
  private static final String MILK = "Milk";
  private static final String RECIPE_TEXT = "Recipe text.";

  @Mock private ProductRepository productRepository;
  @Mock private ApiProxyClient apiProxyClient;

  private RecipeService recipeService;

  @BeforeEach
  void setUp() {
    recipeService = new RecipeService(productRepository, apiProxyClient);
  }

  private static Product product(String name) {
    return new Product(name, "1kg", Set.of(), Set.of());
  }

  @Test
  void rejectsANullRequest() {
    assertThatThrownBy(() -> recipeService.suggest(null))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessage("Basket must contain at least one item.");

    verify(apiProxyClient, never()).send(any());
  }

  @Test
  void rejectsARequestWithNoItems() {
    RecipeSuggestionRequest request = new RecipeSuggestionRequest(List.of(), List.of());

    assertThatThrownBy(() -> recipeService.suggest(request))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessage("Basket must contain at least one item.");

    verify(apiProxyClient, never()).send(any());
  }

  @Test
  void rejectsARequestWithNullItems() {
    RecipeSuggestionRequest request = new RecipeSuggestionRequest(null, List.of());

    assertThatThrownBy(() -> recipeService.suggest(request))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessage("Basket must contain at least one item.");

    verify(apiProxyClient, never()).send(any());
  }

  @Test
  void rejectsABasketWithNoMatchingProducts() {
    when(productRepository.findAllById(List.of(99L))).thenReturn(List.of());
    RecipeSuggestionRequest request =
        new RecipeSuggestionRequest(List.of(new BasketItemRequest(99L, 1)), List.of());

    assertThatThrownBy(() -> recipeService.suggest(request))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessage("No matching products found in the catalogue.");

    verify(apiProxyClient, never()).send(any());
  }

  @Test
  void buildsThePromptFromProductNamesAndTags() {
    when(productRepository.findAllById(List.of(1L, 2L)))
        .thenReturn(List.of(product(APPLE), product(MILK)));
    when(apiProxyClient.send(any())).thenReturn(new ApiProxyResponse(RECIPE_TEXT));

    RecipeSuggestionRequest request =
        new RecipeSuggestionRequest(
            List.of(new BasketItemRequest(1L, 1), new BasketItemRequest(2L, 2)),
            List.of("vegan", "gluten_free"));

    String result = recipeService.suggest(request);

    assertThat(result).isEqualTo(RECIPE_TEXT);

    ArgumentCaptor<ApiProxyRequest> captor = ArgumentCaptor.forClass(ApiProxyRequest.class);
    verify(apiProxyClient).send(captor.capture());

    String prompt = captor.getValue().userPrompt();

    assertThat(prompt).contains(APPLE).contains(MILK).contains("vegan, gluten_free");
  }

  @Test
  void usesNoneWhenDietaryTagsAreNull() {
    when(productRepository.findAllById(List.of(1L))).thenReturn(List.of(product(APPLE)));
    when(apiProxyClient.send(any())).thenReturn(new ApiProxyResponse(RECIPE_TEXT));

    RecipeSuggestionRequest request =
        new RecipeSuggestionRequest(List.of(new BasketItemRequest(1L, 1)), null);

    recipeService.suggest(request);

    ArgumentCaptor<ApiProxyRequest> captor = ArgumentCaptor.forClass(ApiProxyRequest.class);
    verify(apiProxyClient).send(captor.capture());
    assertThat(captor.getValue().userPrompt()).contains("none");
  }

  @Test
  void usesNoneWhenDietaryTagsAreEmpty() {
    when(productRepository.findAllById(List.of(1L))).thenReturn(List.of(product(APPLE)));
    when(apiProxyClient.send(any())).thenReturn(new ApiProxyResponse(RECIPE_TEXT));

    RecipeSuggestionRequest request =
        new RecipeSuggestionRequest(List.of(new BasketItemRequest(1L, 1)), List.of());

    recipeService.suggest(request);

    ArgumentCaptor<ApiProxyRequest> captor = ArgumentCaptor.forClass(ApiProxyRequest.class);
    verify(apiProxyClient).send(captor.capture());
    assertThat(captor.getValue().userPrompt()).contains("none");
  }

  @Test
  void returnsTheContentFromTheApiProxy() {
    when(productRepository.findAllById(List.of(1L))).thenReturn(List.of(product(APPLE)));
    when(apiProxyClient.send(any())).thenReturn(new ApiProxyResponse("Tomato pasta with garlic."));

    RecipeSuggestionRequest request =
        new RecipeSuggestionRequest(List.of(new BasketItemRequest(1L, 1)), List.of());

    assertThat(recipeService.suggest(request)).isEqualTo("Tomato pasta with garlic.");
  }

  private String suggestWithReply(String modelReply) {
    when(productRepository.findAllById(List.of(1L))).thenReturn(List.of(product("Bread")));
    when(apiProxyClient.send(any())).thenReturn(new ApiProxyResponse(modelReply));

    return recipeService.suggest(
        new RecipeSuggestionRequest(List.of(new BasketItemRequest(1L, 1)), List.of()));
  }

  @Test
  void replacesARecipeThatOnlyUsesAddedIngredients() {
    String reply =
        """
        Recipe name
        Minty Cheese Toast

        Ingredients list
        - (NOT IN BASKET) Bread
        - (NOT IN BASKET) Cheddar cheese
        - (NOT IN BASKET) Butter

        Steps
        1. Toast the bread and spread with butter.
        2. Top with cheddar and grill until bubbling.
        """;

    assertThat(suggestWithReply(reply)).isEqualTo(RecipeService.NO_FOOD_MESSAGE);
  }

  @Test
  void keepsARecipeThatUsesAtLeastOneBasketIngredient() {
    String reply =
        """
        Recipe name
        Steak with Garlic Broccoli

        Ingredients list
        - Steak
        - (NOT IN BASKET) Garlic
        - (NOT IN BASKET) Olive oil

        Steps
        1. Cook the steak.
        2. Fry the garlic and serve.
        """;

    assertThat(suggestWithReply(reply)).isEqualTo(reply);
  }

  @Test
  void keepsTheModelsOwnNoFoodReply() {
    assertThat(suggestWithReply(RecipeService.NO_FOOD_MESSAGE))
        .isEqualTo(RecipeService.NO_FOOD_MESSAGE);
  }

  @Test
  void detectsAddedOnlyIngredientsWithMarkdownHeadingsAndBullets() {
    String reply =
        """
        **Recipe name**
        Yogurt Cup

        **Ingredients list:**
        * (not in basket) Plain yogurt
        \u2022 (NOT IN BASKET) Honey

        **Steps**
        1. Spoon yogurt into a cup and drizzle with honey.
        """;

    assertThat(RecipeService.onlyUsesAddedIngredients(reply)).isTrue();
  }

  @Test
  void ignoresIngredientMarkersOutsideTheIngredientsList() {
    String reply =
        """
        Recipe name
        Apple Slices

        Ingredients list
        - Apple

        Steps
        - (NOT IN BASKET) is not used here
        """;

    assertThat(RecipeService.onlyUsesAddedIngredients(reply)).isFalse();
    assertThat(RecipeService.onlyUsesAddedIngredients(null)).isFalse();
  }

  @Test
  void returnsTheNoFoodMessageWithoutCallingTheModelWhenNothingIsFood() {
    when(productRepository.findAllById(List.of(1L, 2L)))
        .thenReturn(
            List.of(
                product("Dine Perfect Portions Pate Entree With Gourmet Turkey Wet Cat Food"),
                product("Colgate Cavity Protection Toothpaste")));

    RecipeSuggestionRequest request =
        new RecipeSuggestionRequest(
            List.of(new BasketItemRequest(1L, 1), new BasketItemRequest(2L, 1)), List.of());

    assertThat(recipeService.suggest(request)).isEqualTo(RecipeService.NO_FOOD_MESSAGE);
    verify(apiProxyClient, never()).send(any());
  }

  @Test
  void leavesNonFoodItemsOutOfThePrompt() {
    when(productRepository.findAllById(List.of(1L, 2L)))
        .thenReturn(List.of(product("Beef Steak"), product("Ajax Multipurpose Cleaner")));
    when(apiProxyClient.send(any())).thenReturn(new ApiProxyResponse(RECIPE_TEXT));

    RecipeSuggestionRequest request =
        new RecipeSuggestionRequest(
            List.of(new BasketItemRequest(1L, 1), new BasketItemRequest(2L, 1)), List.of());

    recipeService.suggest(request);

    ArgumentCaptor<ApiProxyRequest> captor = ArgumentCaptor.forClass(ApiProxyRequest.class);
    verify(apiProxyClient).send(captor.capture());
    assertThat(captor.getValue().userPrompt()).contains("Beef Steak").doesNotContain("Ajax");
  }

  @Test
  void recognisesNonFoodProductNamesIgnoringCase() {
    assertThat(RecipeService.isNonFood("Colgate Toothpaste")).isTrue();
    assertThat(RecipeService.isNonFood("DOVE SOAP BAR")).isTrue();
    assertThat(RecipeService.isNonFood("Whiskas Kitten Pouches")).isTrue();
    assertThat(RecipeService.isNonFood("Finish Dishwasher Tablets")).isTrue();
  }

  @Test
  void onlyMatchesNonFoodTermsAsWholeWords() {
    assertThat(RecipeService.isNonFood("Hellers Hot Dog Rolls")).isFalse();
    assertThat(RecipeService.isNonFood("Wattie's Petite Peas")).isFalse();
    assertThat(RecipeService.isNonFood("Beef Steak")).isFalse();
    assertThat(RecipeService.isNonFood(null)).isFalse();
  }
}
