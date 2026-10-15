package nz.ac.auckland.grocerfy.service;

import java.math.BigDecimal;
import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import static org.mockito.Mockito.when;
import org.mockito.junit.jupiter.MockitoExtension;

import nz.ac.auckland.grocerfy.dto.BasketComparisonRequest;
import nz.ac.auckland.grocerfy.dto.BasketComparisonResponse;
import nz.ac.auckland.grocerfy.dto.BasketItemRequest;
import nz.ac.auckland.grocerfy.dto.MissingItemResponse;
import nz.ac.auckland.grocerfy.dto.MissingItemResponse.MissingReason;
import nz.ac.auckland.grocerfy.dto.StoreComparisonResponse;
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

	@InjectMocks
	private BasketComparisonService basketComparisonService;

	@Test
	void compareBasketMergesDuplicateItemsAndChoosesCheapestAvailableStore() {
		Product apple = new Product(1L, "Apple", "Fresh", "Fruit", "1kg", true, false, true, true);
		Product bread = new Product(2L, "White Bread", "Tip Top", "Bakery", "1 loaf", true, false, true, true);
		Store storeA = new Store("Pak n Save", "Auckland", "1 Main St");
		Store storeB = new Store("Countdown", "Auckland", "2 Main St");
		storeA.setStoreId(10L);
		storeB.setStoreId(11L);

		when(productRepository.findAllById(Set.of(1L, 2L))).thenReturn(List.of(apple, bread));
		when(storePriceRepository.findAllWithRelations()).thenReturn(List.of(
				new StorePrice(apple, storeA, new BigDecimal("1.50"), 20),
				new StorePrice(bread, storeA, new BigDecimal("4.00"), 20),
				new StorePrice(apple, storeB, new BigDecimal("1.80"), 20),
				new StorePrice(bread, storeB, new BigDecimal("3.50"), 20)));
		when(storeRepository.findAll()).thenReturn(List.of(storeA, storeB));

		BasketComparisonRequest request = new BasketComparisonRequest(List.of(
				new BasketItemRequest(1L, 2),
				new BasketItemRequest(1L, 1),
				new BasketItemRequest(2L, 1)));

		BasketComparisonResponse response = basketComparisonService.compareBasket(request);

		assertThat(response.requestedItems()).hasSize(2);
		assertThat(response.stores()).hasSize(2);
		assertThat(response.cheapestAvailableStore()).isNotNull();
		assertThat(response.cheapestAvailableStore().storeName()).isEqualTo("Pak n Save");
		assertThat(response.cheapestAvailableStore().availableSubtotal()).isEqualByComparingTo("8.50");
		assertThat(response.stores().stream().filter(StoreComparisonResponse::available).map(StoreComparisonResponse::storeName))
				.containsExactly("Pak n Save", "Countdown");
	}

	@Test
	void compareBasketRejectsMissingProductIdsAndEmptyBasket() {
		assertThatThrownBy(() -> basketComparisonService.compareBasket(new BasketComparisonRequest(List.of())))
				.isInstanceOf(IllegalArgumentException.class)
				.hasMessageContaining("at least one item");

		when(productRepository.findAllById(Set.of(99L))).thenReturn(List.of());

		assertThatThrownBy(() -> basketComparisonService.compareBasket(new BasketComparisonRequest(List.of(new BasketItemRequest(99L, 1)))))
				.isInstanceOf(IllegalArgumentException.class)
				.hasMessageContaining("Unknown product ids");
	}

	@Test
	void compareBasketTreatsItemsWithoutEnoughStockAsMissing() {
		Product apple = new Product(1L, "Apple", "Fresh", "Fruit", "1kg", true, false, true, true);
		Product bread = new Product(2L, "White Bread", "Tip Top", "Bakery", "1 loaf", true, false, true, true);
		Store outOfStock = new Store("Pak n Save", "Auckland", "1 Main St");
		Store lowStock = new Store("Countdown", "Auckland", "2 Main St");
		Store notStocked = new Store("New World", "Auckland", "3 Main St");
		outOfStock.setStoreId(10L);
		lowStock.setStoreId(11L);
		notStocked.setStoreId(12L);

		when(productRepository.findAllById(Set.of(1L, 2L))).thenReturn(List.of(apple, bread));
		when(storePriceRepository.findAllWithRelations()).thenReturn(List.of(
				new StorePrice(apple, outOfStock, new BigDecimal("1.50"), 20),
				new StorePrice(bread, outOfStock, new BigDecimal("3.00"), 0),
				new StorePrice(apple, lowStock, new BigDecimal("1.80"), 20),
				new StorePrice(bread, lowStock, new BigDecimal("3.50"), 1),
				new StorePrice(apple, notStocked, new BigDecimal("2.00"), 20)));
		when(storeRepository.findAll()).thenReturn(List.of(outOfStock, lowStock, notStocked));

		BasketComparisonResponse response = basketComparisonService.compareBasket(new BasketComparisonRequest(List.of(
				new BasketItemRequest(1L, 1),
				new BasketItemRequest(2L, 2))));

		assertThat(response.cheapestAvailableStore()).isNull();
		assertThat(response.stores()).noneMatch(StoreComparisonResponse::available);

		MissingItemResponse outOfStockBread = missingItemAt(response, "Pak n Save");
		assertThat(outOfStockBread.reason()).isEqualTo(MissingReason.OUT_OF_STOCK);
		assertThat(outOfStockBread.availableQuantity()).isZero();

		MissingItemResponse lowStockBread = missingItemAt(response, "Countdown");
		assertThat(lowStockBread.reason()).isEqualTo(MissingReason.INSUFFICIENT_STOCK);
		assertThat(lowStockBread.availableQuantity()).isEqualTo(1);
		assertThat(lowStockBread.quantity()).isEqualTo(2);

		assertThat(missingItemAt(response, "New World").reason()).isEqualTo(MissingReason.NOT_STOCKED);

		// The apple is still listed as an available line with its stock status.
		assertThat(storeNamed(response, "Pak n Save").lineItems())
				.singleElement()
				.satisfies(line -> assertThat(line.stockStatus()).isEqualTo(StockStatus.IN_STOCK));
	}

	@Test
	void compareBasketAllowsLowStockWhenQuantityCanStillBeCovered() {
		Product apple = new Product(1L, "Apple", "Fresh", "Fruit", "1kg", true, false, true, true);
		Store store = new Store("Pak n Save", "Auckland", "1 Main St");
		store.setStoreId(10L);

		when(productRepository.findAllById(Set.of(1L))).thenReturn(List.of(apple));
		when(storePriceRepository.findAllWithRelations()).thenReturn(List.of(
				new StorePrice(apple, store, new BigDecimal("1.50"), 3)));
		when(storeRepository.findAll()).thenReturn(List.of(store));

		BasketComparisonResponse response = basketComparisonService.compareBasket(
				new BasketComparisonRequest(List.of(new BasketItemRequest(1L, 3))));

		assertThat(response.cheapestAvailableStore()).isNotNull();
		assertThat(response.cheapestAvailableStore().availableSubtotal()).isEqualByComparingTo("4.50");
		assertThat(response.cheapestAvailableStore().lineItems())
				.singleElement()
				.satisfies(line -> assertThat(line.stockStatus()).isEqualTo(StockStatus.LOW_STOCK));
	}

	private static StoreComparisonResponse storeNamed(BasketComparisonResponse response, String storeName) {
		return response.stores().stream()
				.filter(store -> store.storeName().equals(storeName))
				.findFirst()
				.orElseThrow();
	}

	private static MissingItemResponse missingItemAt(BasketComparisonResponse response, String storeName) {
		List<MissingItemResponse> missingItems = storeNamed(response, storeName).missingItems();
		assertThat(missingItems).hasSize(1);
		return missingItems.get(0);
	}
}
