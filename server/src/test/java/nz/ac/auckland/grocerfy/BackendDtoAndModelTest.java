package nz.ac.auckland.grocerfy;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.util.Set;

import org.junit.jupiter.api.Test;

import nz.ac.auckland.grocerfy.model.Allergen;
import nz.ac.auckland.grocerfy.model.Dietary;
import nz.ac.auckland.grocerfy.model.LoyaltyProgram;
import nz.ac.auckland.grocerfy.model.Product;
import nz.ac.auckland.grocerfy.model.Store;
import nz.ac.auckland.grocerfy.model.StorePrice;

/**
 * Unit tests for the model classes' constructors and the rules they enforce.
 */
class BackendDtoAndModelTest {

	private final Product butter = new Product("Anchor Butter", "500g", Set.of(Allergen.DAIRY), Set.of(Dietary.VEGETARIAN));
	private final Store store = new Store("Woolworths Three Kings", "Three Kings", "532 Mt Albert Road", "9529");

	@Test
	void productAndStoreExposeTheirFields() {
		assertThat(butter.getProductName()).isEqualTo("Anchor Butter");
		assertThat(butter.getSize()).isEqualTo("500g");
		assertThat(butter.getAllergens()).containsExactly(Allergen.DAIRY);
		assertThat(butter.getDietInfo()).containsExactly(Dietary.VEGETARIAN);

		assertThat(store.getName()).isEqualTo("Woolworths Three Kings");
		assertThat(store.getRegion()).isEqualTo("Three Kings");
		assertThat(store.getAddress()).isEqualTo("532 Mt Albert Road");
		assertThat(store.getCode()).isEqualTo("9529");
	}

	@Test
	void shelfPriceOnlyHasNoMemberPrice() {
		StorePrice price = new StorePrice(butter, store, new BigDecimal("9.29"));

		assertThat(price.getPrice()).isEqualByComparingTo("9.29");
		assertThat(price.getMemberPrice()).isNull();
		assertThat(price.getLoyaltyProgram()).isNull();
	}

	@Test
	void memberPriceIsKeptTogetherWithItsLoyaltyProgram() {
		StorePrice price = new StorePrice(butter, store, new BigDecimal("9.29"), new BigDecimal("8.50"),
				LoyaltyProgram.EVERYDAY_REWARDS);

		assertThat(price.getMemberPrice()).isEqualByComparingTo("8.50");
		assertThat(price.getLoyaltyProgram()).isEqualTo(LoyaltyProgram.EVERYDAY_REWARDS);
	}

	@Test
	void memberPriceWithoutALoyaltyProgramIsDropped() {
		StorePrice price = new StorePrice(butter, store, new BigDecimal("9.29"), new BigDecimal("8.50"), null);

		assertThat(price.getMemberPrice()).isNull();
		assertThat(price.getLoyaltyProgram()).isNull();
	}

	@Test
	void loyaltyProgramWithoutAMemberPriceIsDropped() {
		StorePrice price = new StorePrice(butter, store, new BigDecimal("9.29"), null, LoyaltyProgram.EVERYDAY_REWARDS);

		assertThat(price.getMemberPrice()).isNull();
		assertThat(price.getLoyaltyProgram()).isNull();
	}
}
