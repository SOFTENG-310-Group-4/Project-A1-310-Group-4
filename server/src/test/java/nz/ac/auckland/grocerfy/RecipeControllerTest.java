package nz.ac.auckland.grocerfy;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import nz.ac.auckland.grocerfy.controller.RecipeController;
import nz.ac.auckland.grocerfy.dto.RecipeSuggestionRequest;
import nz.ac.auckland.grocerfy.service.RecipeService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

/**
 * Web layer tests for the recipe suggestion endpoint.
 *
 * <p>The service is mocked, so these tests only exercise the controller contract: routing, request
 * deserialisation, response content type, and status codes.
 */
@WebMvcTest(RecipeController.class)
class RecipeControllerTest {

  private static final String VALID_BODY =
      """
      {
        "items": [{ "productId": 1, "quantity": 2 }],
        "dietaryTags": ["vegan"]
      }
      """;

  @Autowired private MockMvc mockMvc;

  @MockitoBean private RecipeService recipeService;

  @Test
  void returnsTheSuggestionAsPlainText() throws Exception {
    when(recipeService.suggest(any())).thenReturn("Tomato pasta with garlic.");

    mockMvc
        .perform(
            post("/api/recipes/suggest")
                .contentType(MediaType.APPLICATION_JSON)
                .content(VALID_BODY))
        .andExpect(status().isOk())
        .andExpect(content().contentTypeCompatibleWith(MediaType.TEXT_PLAIN))
        .andExpect(content().string("Tomato pasta with garlic."));
  }

  @Test
  void passesTheDeserialisedRequestToTheService() throws Exception {
    when(recipeService.suggest(any())).thenReturn("ok");

    mockMvc
        .perform(
            post("/api/recipes/suggest")
                .contentType(MediaType.APPLICATION_JSON)
                .content(VALID_BODY))
        .andExpect(status().isOk());

    verify(recipeService).suggest(any(RecipeSuggestionRequest.class));
  }

  @Test
  void returnsBadRequestWhenTheServiceRejectsTheBasket() throws Exception {
    when(recipeService.suggest(any()))
        .thenThrow(new IllegalArgumentException("Basket must contain at least one item."));
    mockMvc
        .perform(
            post("/api/recipes/suggest")
                .contentType(MediaType.APPLICATION_JSON)
                .content(VALID_BODY))
        .andExpect(status().isBadRequest());
  }

  @Test
  void rejectsMalformedJson() throws Exception {
    mockMvc
        .perform(
            post("/api/recipes/suggest")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{not json"))
        .andExpect(status().isBadRequest());
  }
}
