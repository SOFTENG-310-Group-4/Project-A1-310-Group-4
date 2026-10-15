package nz.ac.auckland.grocerfy.controller;

import nz.ac.auckland.grocerfy.dto.RecipeSuggestionRequest;
import nz.ac.auckland.grocerfy.service.RecipeService;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** REST controller for recipe suggestion endpoints. */
@RestController
@RequestMapping("/api/recipes")
@CrossOrigin(origins = "http://localhost:5173")
public class RecipeController {

  private final RecipeService recipeService;

  public RecipeController(RecipeService recipeService) {
    this.recipeService = recipeService;
  }

  /**
   * Suggests a recipe based on the given request.
   *
   * @param request recipe suggestion request; must be valid
   * @return plain-text recipe suggestion
   */
  @PostMapping(value = "/suggest", produces = "text/plain;charset=UTF-8")
  public String suggest(@RequestBody RecipeSuggestionRequest request) {
    return recipeService.suggest(request);
  }
}
