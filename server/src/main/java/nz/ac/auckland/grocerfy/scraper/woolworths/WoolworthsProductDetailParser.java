package nz.ac.auckland.grocerfy.scraper.woolworths;

import java.io.IOException;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;

/**
 * Reads ingredient and allergen text from a Woolworths product details page in Next.js's "_rsc" format.
 * The payload is not one JSON document, but the product's fields are plain JSON inside it.
 */
final class WoolworthsProductDetailParser {

    private static final JsonFactory JSON = new JsonFactory();

    private WoolworthsProductDetailParser() {
    }

    /**
     * Combines the product's ingredients and allergen statement into one text to search for allergens.
     * @param body the product details page
     * @param productId the product, for the warning if the page has no ingredient data
     * @return the ingredients and allergen statement, either of which may be missing (never null)
     */
    static String allergenText(String body, String productId) {
        String ingredients = readStringAfterKey(body, "ingredients");
        String allergenStatement = readStringAfterKey(body, "allergenContained");
        if (ingredients == null && allergenStatement == null && !body.contains("\"ingredients\"")) {
            System.err.println("No ingredient data found for Woolworths product " + productId + ". Page format change?");
        }
        return (ingredients == null ? "" : ingredients) + " "
            + (allergenStatement == null ? "" : allergenStatement);
    }

    /**
     * Finds the first {@code "key":} in the text and reads the JSON value after it.
     * @return the string value, or null if the key is absent or its value is null / not a string
     */
    static String readStringAfterKey(String text, String key) {
        String marker = "\"" + key + "\":";
        int index = text.indexOf(marker);
        if (index < 0) {
            return null;
        }
        try (JsonParser parser = JSON.createParser(text.substring(index + marker.length()))) {
            JsonToken token = parser.nextToken();
            return token == JsonToken.VALUE_STRING ? parser.getText() : null;
        } catch (IOException exc) {
            return null;
        }
    }
}
