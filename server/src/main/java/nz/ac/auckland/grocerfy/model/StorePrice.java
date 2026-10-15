package nz.ac.auckland.grocerfy.model;

import java.math.BigDecimal;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

/**
 * Create store price entities for different products and their store prices.
 * Also tracks how many units of the product the store currently has in stock.
 */
@Entity
@Table(name = "store_prices", uniqueConstraints = @UniqueConstraint(columnNames = { "product_id", "store_id" }))
public class StorePrice {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long storePriceId;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "product_id", nullable = false)
	private Product product;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "store_id", nullable = false)
	private Store store;

	@Column(nullable = false, precision = 10, scale = 2)
	private BigDecimal price;

	@Column(name = "stock_quantity", nullable = false)
	private int stockQuantity;

	protected StorePrice() {
	}

	public StorePrice(Product product, Store store, BigDecimal price, int stockQuantity) {
		this.product = product;
		this.store = store;
		this.price = price;
		this.stockQuantity = stockQuantity;
	}

	public Long getStorePriceId() {
		return storePriceId;
	}

	public void setStorePriceId(Long storePriceId) {
		this.storePriceId = storePriceId;
	}

	public Product getProduct() {
		return product;
	}

	public void setProduct(Product product) {
		this.product = product;
	}

	public Store getStore() {
		return store;
	}

	public void setStore(Store store) {
		this.store = store;
	}

	public BigDecimal getPrice() {
		return price;
	}

	public void setPrice(BigDecimal price) {
		this.price = price;
	}

	public int getStockQuantity() {
		return stockQuantity;
	}

	public void setStockQuantity(int stockQuantity) {
		this.stockQuantity = stockQuantity;
	}

	public StockStatus getStockStatus() {
		return StockStatus.fromQuantity(stockQuantity);
	}

	public String getPriceLabel() {
		return "$" + price.setScale(2, java.math.RoundingMode.HALF_UP);
	}
}