package nz.ac.auckland.grocerfy.service;

import java.math.BigDecimal;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import static org.mockito.Mockito.when;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import nz.ac.auckland.grocerfy.dto.BasketComparisonRequest;
import nz.ac.auckland.grocerfy.dto.BasketComparisonResponse;
import nz.ac.auckland.grocerfy.dto.BasketItemRequest;
import nz.ac.auckland.grocerfy.dto.Coordinates;
import nz.ac.auckland.grocerfy.dto.MissingItemResponse;
import nz.ac.auckland.grocerfy.dto.MissingItemResponse.MissingReason;
import nz.ac.auckland.grocerfy.dto.StoreComparisonResponse;
import nz.ac.auckland.grocerfy.dto.StoreLineItemResponse;
import nz.ac.auckland.grocerfy.model.Allergen;
import nz.ac.auckland.grocerfy.model.Dietary;
import nz.ac.auckland.grocerfy.model.LoyaltyProgram;
import nz.ac.auckland.grocerfy.model.Product;
import nz.ac.auckland.grocerfy.model.StockStatus;
import nz.ac.auckland.grocerfy.model.Store;
import nz.ac.auckland.grocerfy.model.StorePrice;
import nz.ac.auckland.grocerfy.repository.ProductRepository;
import nz.ac.auckland.grocerfy.repository.StorePriceRepository;
import nz.ac.auckland.grocerfy.repository.StoreRepository;

@ExtendWith(MockitoExtension.class)
class BasketComparisonServiceTest {

	@Mock
	private ProductRepository productRepository;

	@Mock
	private StoreRepository storeRepository;

	@Mock
	private StorePriceRepository storePriceRepository;

  @Mock
  private StoreLocationLookup storeLocations;

	@InjectMocks
	private BasketComparisonService basketComparisonService;

	// @Test
	void compareBasketMergesDuplicateItemsAndChoosesCheapestAvailableStore() {
		Product apple = new Product("Ambrosia Apple", "1kg", new HashSet<>(), Set.of(Dietary.VEGETARIAN, Dietary.VEGAN));
		Product bread = new Product("White Bread", "200g", Set.of(Allergen.WHEAT, Allergen.GLUTEN, Allergen.EGG, Allergen.DAIRY), new HashSet<>());
		Store storeA = new Store("Pak'nSave testStore", "Auckland", "1 Main St", "hypothetical uuid");
		Store storeB = new Store("New World testStore", "Auckland", "2 Main St", "potential uuid");

		when(productRepository.findAllById(Set.of(1L, 2L))).thenReturn(List.of(apple, bread));
		when(storePriceRepository.findAllWithRelations()).thenReturn(List.of(
				new StorePrice(apple, storeA, new BigDecimal("1.50")),
				new StorePrice(bread, storeA, new BigDecimal("4.00")),
				new StorePrice(apple, storeB, new BigDecimal("1.80")),
				new StorePrice(bread, storeB, new BigDecimal("3.50"))));
		when(storeRepository.findAll()).thenReturn(List.of(storeA, storeB));

		BasketComparisonRequest request = new BasketComparisonRequest(List.of(
				new BasketItemRequest(1L, 2),
				new BasketItemRequest(1L, 1),
				new BasketItemRequest(2L, 1)), null);

		BasketComparisonResponse response = basketComparisonService.compareBasket(request);

		assertThat(response.requestedItems()).hasSize(2);
		assertThat(response.stores()).hasSize(2);
		assertThat(response.cheapestAvailableStore()).isNotNull();
		assertThat(response.cheapestAvailableStore().storeName()).isEqualTo("Pak'nSave testStore");
		assertThat(response.cheapestAvailableStore().availableSubtotal()).isEqualByComparingTo("8.50");
		assertThat(response.stores().stream().filter(StoreComparisonResponse::available).map(StoreComparisonResponse::storeName))
				.containsExactly("Pak'nSave testStore", "New World testStore");
	}

	// ------------------------------------------------------------------ Everyday Rewards member prices

	private Product yoghurt;
	private Product bread;
	private Store woolworths;
	private Store newWorld;

	/** Entities get ids from the database; set them by hand so the service can key maps on them. */
	private static <T> T withId(T entity, long id) {
		ReflectionTestUtils.setField(entity, "id", id);
		return entity;
	}

	/**
	 * Woolworths: yoghurt $4.75 ($4.20 Everyday Rewards), bread $3.00 (no member price).
	 * New World: yoghurt $4.50, bread $3.10. Basket: 2 yoghurt, 1 bread.
	 */
	private BasketComparisonRequest givenMemberPricedBasket(Set<LoyaltyProgram> loyaltyPrograms) {
		yoghurt = withId(new Product("Natural Yoghurt", "750g", new HashSet<>(), new HashSet<>()), 1L);
		bread = withId(new Product("White Bread", "600g", new HashSet<>(), new HashSet<>()), 2L);
		woolworths = withId(new Store("Woolworths Three Kings", "Three Kings", "532 Mt Albert Road", "9529"), 10L);
		newWorld = withId(new Store("New World Albany", "Albany", "219 Don McKinnon Drive", "uuid"), 20L);

		when(productRepository.findAllById(Set.of(1L, 2L))).thenReturn(List.of(yoghurt, bread));
		when(storePriceRepository.findAllWithRelations()).thenReturn(List.of(
				new StorePrice(yoghurt, woolworths, new BigDecimal("4.75"), new BigDecimal("4.20"), LoyaltyProgram.EVERYDAY_REWARDS),
				new StorePrice(bread, woolworths, new BigDecimal("3.00")),
				new StorePrice(yoghurt, newWorld, new BigDecimal("4.50")),
				new StorePrice(bread, newWorld, new BigDecimal("3.10"))));
		when(storeRepository.findAll()).thenReturn(List.of(woolworths, newWorld));

		return new BasketComparisonRequest(List.of(new BasketItemRequest(1L, 2), new BasketItemRequest(2L, 1)), loyaltyPrograms);
	}

	private static StoreComparisonResponse storeNamed(BasketComparisonResponse response, String name) {
		return response.stores().stream().filter(store -> store.storeName().equals(name)).findFirst().orElseThrow();
	}

	@Test
	void withoutMembershipShelfPricesAreUsedAndTheSavingIsReported() {
		BasketComparisonResponse response = basketComparisonService.compareBasket(givenMemberPricedBasket(null));

		StoreComparisonResponse ww = storeNamed(response, "Woolworths Three Kings");
		assertThat(ww.availableSubtotal()).isEqualByComparingTo("12.50"); // 2 x 4.75 + 3.00
		assertThat(ww.loyaltyProgram()).isEqualTo(LoyaltyProgram.EVERYDAY_REWARDS);
		assertThat(ww.memberSavingAvailable()).isEqualByComparingTo("1.10"); // 2 x (4.75 - 4.20)
		assertThat(ww.memberSavingApplied()).isEqualByComparingTo("0.00");
		StoreLineItemResponse yoghurtLine = ww.lineItems().get(0);
		assertThat(yoghurtLine.unitPrice()).isEqualByComparingTo("4.75");
		assertThat(yoghurtLine.memberPriceApplied()).isFalse();

		// New World at 2 x 4.50 + 3.10 = 12.10 is cheaper on shelf prices
		assertThat(response.cheapestAvailableStore().storeName()).isEqualTo("New World Albany");
	}

	@Test
	void withEverydayRewardsMemberPricesAreAppliedAndCanChangeTheCheapestStore() {
		BasketComparisonResponse response = basketComparisonService.compareBasket(
				givenMemberPricedBasket(Set.of(LoyaltyProgram.EVERYDAY_REWARDS)));

		StoreComparisonResponse ww = storeNamed(response, "Woolworths Three Kings");
		assertThat(ww.availableSubtotal()).isEqualByComparingTo("11.40"); // 2 x 4.20 + 3.00
		assertThat(ww.memberSavingApplied()).isEqualByComparingTo("1.10");
		assertThat(ww.memberSavingAvailable()).isEqualByComparingTo("0.00");

		StoreLineItemResponse yoghurtLine = ww.lineItems().get(0);
		assertThat(yoghurtLine.unitPrice()).isEqualByComparingTo("4.20");
		assertThat(yoghurtLine.regularUnitPrice()).isEqualByComparingTo("4.75");
		assertThat(yoghurtLine.lineTotal()).isEqualByComparingTo("8.40");
		assertThat(yoghurtLine.memberPriceApplied()).isTrue();
		StoreLineItemResponse breadLine = ww.lineItems().get(1);
		assertThat(breadLine.memberPriceApplied()).isFalse();
		assertThat(breadLine.unitPrice()).isEqualByComparingTo("3.00");

		// other chains are unaffected
		StoreComparisonResponse nw = storeNamed(response, "New World Albany");
		assertThat(nw.availableSubtotal()).isEqualByComparingTo("12.10");
		assertThat(nw.loyaltyProgram()).isNull();
		assertThat(nw.memberSavingAvailable()).isEqualByComparingTo("0.00");

		assertThat(response.cheapestAvailableStore().storeName()).isEqualTo("Woolworths Three Kings");
	}

	@Test
	void memberPriceThatIsNotLowerIsIgnored() {
		Product milk = withId(new Product("Milk", "2l", new HashSet<>(), new HashSet<>()), 3L);
		Store store = withId(new Store("Woolworths Manukau", "Manukau", "652 Great South Road", "9161"), 30L);
		when(productRepository.findAllById(Set.of(3L))).thenReturn(List.of(milk));
		when(storePriceRepository.findAllWithRelations()).thenReturn(List.of(
				new StorePrice(milk, store, new BigDecimal("4.00"), new BigDecimal("4.00"), LoyaltyProgram.EVERYDAY_REWARDS)));
		when(storeRepository.findAll()).thenReturn(List.of(store));

		BasketComparisonResponse response = basketComparisonService.compareBasket(new BasketComparisonRequest(
				List.of(new BasketItemRequest(3L, 1)), Set.of(LoyaltyProgram.EVERYDAY_REWARDS)));

		StoreComparisonResponse result = response.stores().get(0);
		assertThat(result.availableSubtotal()).isEqualByComparingTo("4.00");
		assertThat(result.loyaltyProgram()).isNull();
		assertThat(result.lineItems().get(0).memberPriceApplied()).isFalse();
	}

	@Test
	void compareBasketRejectsMissingProductIdsAndEmptyBasket() {
		assertThatThrownBy(() -> basketComparisonService.compareBasket(new BasketComparisonRequest(List.of(), null)))
				.isInstanceOf(IllegalArgumentException.class)
				.hasMessageContaining("at least one item");

		when(productRepository.findAllById(Set.of(99L))).thenReturn(List.of());

		assertThatThrownBy(() -> basketComparisonService.compareBasket(new BasketComparisonRequest(List.of(new BasketItemRequest(99L, 1)), null)))
				.isInstanceOf(IllegalArgumentException.class)
				.hasMessageContaining("Unknown product ids");
	}

  @Test
  void storeResponseIncludesCoordinatesFromLookup() {
      when(storeLocations.find("New World Albany")).thenReturn(new Coordinates(-36.72, 174.71));
      when(storeLocations.find("Woolworths Three Kings")).thenReturn(null);

      BasketComparisonResponse response = basketComparisonService.compareBasket(givenMemberPricedBasket(null));

      StoreComparisonResponse nw = storeNamed(response, "New World Albany");
      assertThat(nw.latitude()).isEqualTo(-36.72);
      assertThat(nw.longitude()).isEqualTo(174.71);

      // a store the lookup has no entry for has no coordinates
      StoreComparisonResponse ww = storeNamed(response, "Woolworths Three Kings");
      assertThat(ww.latitude()).isNull();
      assertThat(ww.longitude()).isNull();
  }

	// ------------------------------------------------------------------ Per-store stock status (#73)

	private static StorePrice stockedPrice(Product product, Store store, String price, Integer stockQuantity) {
		return new StorePrice(product, store, new BigDecimal(price), null, null, stockQuantity);
	}

	private static MissingItemResponse onlyMissingItem(BasketComparisonResponse response, String storeName) {
		List<MissingItemResponse> missingItems = storeNamed(response, storeName).missingItems();
		assertThat(missingItems).hasSize(1);
		return missingItems.get(0);
	}

	@Test
	void itemsWithoutEnoughStockAreTreatedAsMissing() {
		Product apple = withId(new Product("Apple", "1kg", new HashSet<>(), new HashSet<>()), 1L);
		Product bread = withId(new Product("White Bread", "600g", new HashSet<>(), new HashSet<>()), 2L);
		Store outOfStock = withId(new Store("Pak'nSave Albany", "Albany", "1 Main St", "uuid-a"), 10L);
		Store lowStock = withId(new Store("Woolworths Albany", "Albany", "2 Main St", "9001"), 11L);
		Store notStocked = withId(new Store("New World Albany", "Albany", "3 Main St", "uuid-b"), 12L);

		when(productRepository.findAllById(Set.of(1L, 2L))).thenReturn(List.of(apple, bread));
		when(storePriceRepository.findAllWithRelations()).thenReturn(List.of(
				stockedPrice(apple, outOfStock, "1.50", 20),
				stockedPrice(bread, outOfStock, "3.00", 0),
				stockedPrice(apple, lowStock, "1.80", 20),
				stockedPrice(bread, lowStock, "3.50", 1),
				stockedPrice(apple, notStocked, "2.00", 20)));
		when(storeRepository.findAll()).thenReturn(List.of(outOfStock, lowStock, notStocked));

		BasketComparisonResponse response = basketComparisonService.compareBasket(new BasketComparisonRequest(List.of(
				new BasketItemRequest(1L, 1),
				new BasketItemRequest(2L, 2)), null));

		assertThat(response.cheapestAvailableStore()).isNull();
		assertThat(response.stores()).noneMatch(StoreComparisonResponse::available);

		MissingItemResponse outOfStockBread = onlyMissingItem(response, "Pak'nSave Albany");
		assertThat(outOfStockBread.reason()).isEqualTo(MissingReason.OUT_OF_STOCK);
		assertThat(outOfStockBread.availableQuantity()).isZero();

		MissingItemResponse lowStockBread = onlyMissingItem(response, "Woolworths Albany");
		assertThat(lowStockBread.reason()).isEqualTo(MissingReason.INSUFFICIENT_STOCK);
		assertThat(lowStockBread.availableQuantity()).isEqualTo(1);
		assertThat(lowStockBread.quantity()).isEqualTo(2);

		assertThat(onlyMissingItem(response, "New World Albany").reason()).isEqualTo(MissingReason.NOT_STOCKED);

		// the apple is still listed as an available line with its stock status
		assertThat(storeNamed(response, "Pak'nSave Albany").lineItems())
				.singleElement()
				.satisfies(line -> assertThat(line.stockStatus()).isEqualTo(StockStatus.IN_STOCK));
	}

	@Test
	void lowStockIsAllowedWhenItStillCoversTheRequestedQuantity() {
		Product apple = withId(new Product("Apple", "1kg", new HashSet<>(), new HashSet<>()), 1L);
		Store store = withId(new Store("Pak'nSave Albany", "Albany", "1 Main St", "uuid-a"), 10L);

		when(productRepository.findAllById(Set.of(1L))).thenReturn(List.of(apple));
		when(storePriceRepository.findAllWithRelations()).thenReturn(List.of(stockedPrice(apple, store, "1.50", 3)));
		when(storeRepository.findAll()).thenReturn(List.of(store));

		BasketComparisonResponse response = basketComparisonService.compareBasket(
				new BasketComparisonRequest(List.of(new BasketItemRequest(1L, 3)), null));

		assertThat(response.cheapestAvailableStore()).isNotNull();
		assertThat(response.cheapestAvailableStore().availableSubtotal()).isEqualByComparingTo("4.50");
		assertThat(response.cheapestAvailableStore().lineItems())
				.singleElement()
				.satisfies(line -> assertThat(line.stockStatus()).isEqualTo(StockStatus.LOW_STOCK));
	}

	@Test
	void unknownStockIsTreatedAsAvailableWithNoStockStatus() {
		// scraped prices carry no stock level, so a store must still be able to fulfil the basket
		BasketComparisonResponse response = basketComparisonService.compareBasket(givenMemberPricedBasket(null));

		assertThat(response.stores()).allMatch(StoreComparisonResponse::available);
		assertThat(response.stores())
				.flatMap(StoreComparisonResponse::lineItems)
				.allSatisfy(line -> assertThat(line.stockStatus()).isNull());
	}
}
