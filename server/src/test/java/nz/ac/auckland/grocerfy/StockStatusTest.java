package nz.ac.auckland.grocerfy;

import java.math.BigDecimal;
import java.util.HashSet;

import static org.assertj.core.api.Assertions.assertThat;
import org.junit.jupiter.api.Test;

import nz.ac.auckland.grocerfy.model.Product;
import nz.ac.auckland.grocerfy.model.StockStatus;
import nz.ac.auckland.grocerfy.model.Store;
import nz.ac.auckland.grocerfy.model.StorePrice;

class StockStatusTest {

	@Test
	void stockQuantityMapsToStatusBands() {
		assertThat(StockStatus.fromQuantity(0)).isEqualTo(StockStatus.OUT_OF_STOCK);
		assertThat(StockStatus.fromQuantity(-1)).isEqualTo(StockStatus.OUT_OF_STOCK);
		assertThat(StockStatus.fromQuantity(1)).isEqualTo(StockStatus.LOW_STOCK);
		assertThat(StockStatus.fromQuantity(StockStatus.LOW_STOCK_THRESHOLD)).isEqualTo(StockStatus.LOW_STOCK);
		assertThat(StockStatus.fromQuantity(StockStatus.LOW_STOCK_THRESHOLD + 1)).isEqualTo(StockStatus.IN_STOCK);
	}

	@Test
	void storePriceReportsStockStatusOnlyWhenStockIsKnown() {
		Product product = new Product("Apple", "1kg", new HashSet<>(), new HashSet<>());
		Store store = new Store("Local Market", "Auckland", "2 High St", "uuid");

		StorePrice known = new StorePrice(product, store, new BigDecimal("2.10"), null, null, 3);
		StorePrice unknown = new StorePrice(product, store, new BigDecimal("2.10"));

		assertThat(known.getStockQuantity()).isEqualTo(3);
		assertThat(known.getStockStatus()).isEqualTo(StockStatus.LOW_STOCK);
		assertThat(unknown.getStockQuantity()).isNull();
		assertThat(unknown.getStockStatus()).isNull();
	}
}
