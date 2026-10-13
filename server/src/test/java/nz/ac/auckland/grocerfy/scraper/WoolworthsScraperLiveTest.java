package nz.ac.auckland.grocerfy.scraper;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.math.BigDecimal;
import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.data.util.Pair;

import nz.ac.auckland.grocerfy.dto.ProductInfo;
import nz.ac.auckland.grocerfy.model.Allergen;
import nz.ac.auckland.grocerfy.model.Dietary;

/**
 * Smoke test against the real woolworths.co.nz (about 7 requests). Skipped unless WOOLWORTHS_LIVE=true,
 * so normal builds never touch the network. Run it after changes, or if Woolworths scrapes start failing:
 * <pre>
 *   PowerShell:  $env:WOOLWORTHS_LIVE="true"; ./mvnw test -Dtest=WoolworthsScraperLiveTest
 *   bash:        WOOLWORTHS_LIVE=true ./mvnw test -Dtest=WoolworthsScraperLiveTest
 * </pre>
 */
@EnabledIfEnvironmentVariable(named = "WOOLWORTHS_LIVE", matches = "true")
public class WoolworthsScraperLiveTest {

	@Test
	void scrapesOnePageAndOneProductFromTheLiveSite() {
		WoolworthsScraper scraper = new WoolworthsScraper();
		scraper.setupCookies();
		scraper.setMaxPages(1);

		assertTrue(scraper.changeStore("9529"), "store change to Three Kings failed");
		assertNotNull(scraper.guestToken, "Woolworths did not hand out a guest token");

		List<ProductInfo> products = scraper.fetchProducts("9-BDB6545B"); // Fruit & Veg
		assertFalse(products.isEmpty(), "no products returned");
		assertEquals("9270", scraper.pinnedStoreKey, "products did not come from Three Kings");
		for (ProductInfo product : products) {
			assertTrue(product.price().compareTo(BigDecimal.ZERO) > 0, "bad price for " + product.productName());
			assertFalse(product.productSize().isBlank());
		}
		System.out.println("Live Woolworths sample: " + products.subList(0, Math.min(5, products.size())));

		Pair<Set<Allergen>, Set<Dietary>> weetBix = scraper.getProductInfo("885704/weet-bix-cereal", "9529");
		assertTrue(weetBix.getFirst().contains(Allergen.WHEAT), "Weet-Bix ingredients not read: " + weetBix.getFirst());
	}
}
