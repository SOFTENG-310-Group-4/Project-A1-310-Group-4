package nz.ac.auckland.grocerfy.scraper.woolworths;

import static nz.ac.auckland.grocerfy.scraper.woolworths.WoolworthsFakes.GRAPHQL;
import static nz.ac.auckland.grocerfy.scraper.woolworths.WoolworthsFakes.bodyOf;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.net.http.HttpRequest;
import java.util.Optional;

import org.junit.jupiter.api.Test;

/**
 * Unit tests for the requests sent to Woolworths: endpoints, headers and GraphQL variables.
 */
public class WoolworthsRequestsTest {

    @Test
    void setStoreSendsThePickupIdAsAGraphqlMutation() {
        HttpRequest request = WoolworthsRequests.setStore("9529");

        assertEquals("POST", request.method());
        assertEquals(GRAPHQL + "SetCartShoppingMode", request.uri().toString());
        assertEquals(Optional.of("SetCartShoppingMode"), request.headers().firstValue("wnzx-operation-name"));
        String body = bodyOf(request);
        assertTrue(body.contains("\"pickupLocationId\":\"9529\""));
        assertTrue(body.contains("\"shoppingMode\":\"Pickup\""));
    }

    @Test
    void productSearchAsksForOneCategoryPageWithMemberPriceTags() {
        String body = bodyOf(WoolworthsRequests.productSearch("9-75C37855", 2, null));

        assertTrue(body.contains("\"value\":\"9-75C37855\""));
        assertTrue(body.contains("\"pageIndex\":2"));
        assertTrue(body.contains("\"pageSize\":" + WoolworthsRequests.PAGE_SIZE));
        assertTrue(body.contains("\"facetFilters\":[]"));
        assertTrue(body.contains("tags { type userSegmentId decisionInputs }"));
    }

    @Test
    void productSearchCanBeNarrowedByAFilter() {
        String body = bodyOf(WoolworthsRequests.productSearch("9-75C37855", 0, "isVegan"));

        assertTrue(body.contains("\"facetFilters\":[[{\"key\":\"isVegan\",\"value\":\"true\"}]]"));
    }

    @Test
    void productDetailsAsksForTheNextJsDataOfTheProductPage() {
        HttpRequest request = WoolworthsRequests.productDetails("885704/weet-bix-cereal");

        assertEquals("GET", request.method());
        assertEquals("https://www.woolworths.co.nz/shop/product-details/885704/weet-bix-cereal?_rsc=1", request.uri().toString());
        assertEquals(Optional.of("1"), request.headers().firstValue("RSC"));
    }
}
