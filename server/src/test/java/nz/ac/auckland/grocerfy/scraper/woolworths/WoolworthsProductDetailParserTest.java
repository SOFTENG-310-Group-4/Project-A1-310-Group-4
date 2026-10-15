package nz.ac.auckland.grocerfy.scraper.woolworths;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

/**
 * Unit tests for reading ingredient and allergen text from product details pages.
 */
public class WoolworthsProductDetailParserTest {

    @Test
    void readStringAfterKeyHandlesEscapesNullsAndMissingKeys() {
        String text = "x:{\"ingredients\":\"Milk \\\"A2\\\" (98%), Salt\",\"allergenContained\":null}";

        assertEquals("Milk \"A2\" (98%), Salt", WoolworthsProductDetailParser.readStringAfterKey(text, "ingredients"));
        assertNull(WoolworthsProductDetailParser.readStringAfterKey(text, "allergenContained"));
        assertNull(WoolworthsProductDetailParser.readStringAfterKey(text, "directionsOfUse"));
    }

    @Test
    void allergenTextCombinesIngredientsAndAllergenStatement() {
        String page = "5:[\"$\",\"div\",null,{\"product\":{\"ingredients\":\"Water, Sugar\","
            + "\"allergenContained\":\"Contains Milk and Soy\"}}]";

        assertEquals("Water, Sugar Contains Milk and Soy", WoolworthsProductDetailParser.allergenText(page, "1/test"));
    }

    @Test
    void allergenTextIsBlankWhenThePageHasNoIngredientData() {
        assertTrue(WoolworthsProductDetailParser.allergenText("{}", "1/test").isBlank());
    }
}
