package nz.ac.auckland.grocerfy.scraper.woolworths;

import static nz.ac.auckland.grocerfy.scraper.woolworths.WoolworthsFakes.resource;
import static nz.ac.auckland.grocerfy.scraper.woolworths.WoolworthsFakes.searchPage;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.math.BigDecimal;
import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import nz.ac.auckland.grocerfy.dto.ProductInfo;

/**
 * Unit tests for reading ProductSearch responses. No network: recorded responses and small JSON snippets.
 */
public class WoolworthsProductParserTest {

    private final ObjectMapper json = new ObjectMapper();

    private JsonNode productsNode(String responseBody) {
        return WoolworthsProductParser.readProductsNode(responseBody, "test-category");
    }

    @Test
    void parseProductsReadsRealSearchResponseAndSkipsAdsAndBanners() {
        List<ProductInfo> products = WoolworthsProductParser.parseProducts(resource("/woolworths/search_page.json"));

        // the fixture holds 1 sponsored ad, 1 recipe banner and 5 real products
        assertEquals(5, products.size());

        ProductInfo broccoli = products.get(0);
        assertEquals("Woolworths Fresh Broccoli Head", broccoli.productName());
        assertEquals(new BigDecimal("2"), broccoli.price());
        assertEquals("ea", broccoli.productSize());
        assertEquals("281082/woolworths-fresh-broccoli-head", broccoli.productId());

        // sold by weight: first variant is per kg, the "Min Order 250g" is not the size
        ProductInfo bananas = products.get(1);
        assertEquals(new BigDecimal("3.99"), bananas.price());
        assertEquals("kg", bananas.productSize());

        ProductInfo mince = products.get(2);
        assertEquals("Woolworths Essentials Beef Mince Value 400g", mince.productName());
        assertEquals("400g", mince.productSize());
    }

    @Test
    void parseProductsReadsEverydayRewardsMemberPrice() {
        List<ProductInfo> products = WoolworthsProductParser.parseProducts(resource("/woolworths/search_page_member_price.json"));

        // real product: $4.75 shelf, MemberPrice tag with promotionalPrice 420 (cents)
        ProductInfo yoghurt = products.get(0);
        assertEquals("Gopala Yoghurt Tub Natural Full Cream 750g", yoghurt.productName());
        assertEquals(new BigDecimal("4.75"), yoghurt.price());
        assertEquals(new BigDecimal("4.20"), yoghurt.memberPrice());

        // broccoli has a multibuy tag with a promotionalPrice too, which is not a member price
        assertNull(products.get(1).memberPrice());
    }

    @Test
    void parseProductsSkipsProductsWithoutAPriceAndReturnsEmptyForUnreadableBody() throws Exception {
        JsonNode noPrice = json.readTree("{\"results\":[{\"__typename\":\"ProductSummary\",\"sku\":\"1\","
            + "\"productName\":\"Mystery Item\",\"variants\":[{\"variantPrice\":{}}]}]}");

        assertTrue(WoolworthsProductParser.parseProducts(noPrice).isEmpty());
        assertTrue(WoolworthsProductParser.parseProducts("<html>Access Denied</html>").isEmpty());
    }

    @Test
    void memberPriceIsIgnoredWhenNotCheaperForAnotherUnitOrMissing() throws Exception {
        String memberTag = "[{\"type\":\"MemberPrice\",\"userSegmentId\":\"rewards-member\","
            + "\"decisionInputs\":{\"promotionalPrice\":%s,\"uom\":\"%s\"}}]";

        assertEquals(new BigDecimal("4.20"), WoolworthsProductParser.extractMemberPrice(
            json.readTree(String.format(memberTag, "420", "EA")), new BigDecimal("4.75"), "EA"));
        // not lower than the shelf price
        assertNull(WoolworthsProductParser.extractMemberPrice(
            json.readTree(String.format(memberTag, "475", "EA")), new BigDecimal("4.75"), "EA"));
        // priced per each, but the shelf price is per kg
        assertNull(WoolworthsProductParser.extractMemberPrice(
            json.readTree(String.format(memberTag, "300", "EA")), new BigDecimal("3.99"), "KG"));
        // no tags at all
        assertNull(WoolworthsProductParser.extractMemberPrice(json.readTree("[]"), new BigDecimal("4.75"), "EA"));
    }

    @Test
    void extractSizeHandlesWoolworthsNameFormats() {
        assertEquals("750g", WoolworthsProductParser.extractSize("Gopala Yoghurt Tub Natural Full Cream 750g", "EACH"));
        assertEquals("1.2kg", WoolworthsProductParser.extractSize("Weet-Bix Cereal 1.2kg", "EACH"));
        assertEquals("12 x 250ml", WoolworthsProductParser.extractSize("Up&Go Protein Liquid Breakfast Choc Hit Fridge Pack 12 x 250mL", "EACH"));
        assertEquals("12 x 250ml", WoolworthsProductParser.extractSize("Up&Go Protein Breakfast Drink Vanilla 12x250mL Cartons", "EACH"));
        assertEquals("2 x 95g", WoolworthsProductParser.extractSize("Aunt Betty's Steamed Pudding Gooey Caramel 2 x 95g Pottles", "EACH"));
        assertEquals("204g", WoolworthsProductParser.extractSize("Oreo Creme Filled Biscuits Original Minis 10 Pack - 204g", "EACH"));
        assertEquals("1kg", WoolworthsProductParser.extractSize("Woolworths NZ Beef Mince Grass Fed 18% Fat 1kg tray", "EACH"));
        assertEquals("2l", WoolworthsProductParser.extractSize("Anchor Blue Top Milk 2L", "EACH"));
        assertEquals("12pk", WoolworthsProductParser.extractSize("Hellers Sausages Precooked 12 Pack", "EACH"));
        assertEquals("kg", WoolworthsProductParser.extractSize("Woolworths Lamb Leg Frozen Bone In Min Order 3kg", "KG"));
        assertEquals("ea", WoolworthsProductParser.extractSize("Woolworths Fresh Cucumbers Telegraph", "EACH"));
    }

    @Test
    void readProductsNodeRejectsErrorsMissingListsAndNonJson() {
        assertNotNull(productsNode(searchPage("9270", 1, "1")));
        assertNull(productsNode("{\"errors\":[{\"message\":\"bad\"}]}"));
        assertNull(productsNode("{\"data\":{\"My\":{}}}"));
        assertNull(productsNode("<html>Access Denied</html>"));
    }

    @Test
    void readsSkusStoreKeysAndLastPageFromASearchPage() {
        JsonNode page = productsNode(searchPage("9270", 2, "100", "200"));

        assertEquals(Set.of("100", "200"), WoolworthsProductParser.skus(page));
        assertEquals(List.of("9270", "9270"), WoolworthsProductParser.storeKeys(page));
        assertFalse(WoolworthsProductParser.isLastPage(page, 0));
        assertTrue(WoolworthsProductParser.isLastPage(page, 1));
    }

    @Test
    void skuOfTakesThePartBeforeTheSlug() {
        assertEquals("885704", WoolworthsProductParser.skuOf("885704/weet-bix-cereal"));
        assertEquals("885704", WoolworthsProductParser.skuOf("885704"));
    }
}
