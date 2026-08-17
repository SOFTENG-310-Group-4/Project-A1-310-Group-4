package nz.ac.auckland.grocerfy.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/**
 * Table to store information about different stores and their attributes.
 */
@Entity
@Table(name = "stores")
public class Store {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long storeId;

	@Column(nullable = false, unique = true, length = 150)
	private String storeName;

	@Column(nullable = false, length = 100)
	private String region;

	@Column(length = 255)
	private String address;

	protected Store() {
	}

	public Store(String storeName, String region, String address) {
		this.storeName = storeName;
		this.region = region;
		this.address = address;
	}

	public Long getStoreId() {
		return storeId;
	}

	public void setStoreId(Long storeId) {
		this.storeId = storeId;
	}

	public String getStoreName() {
		return storeName;
	}

	public void setStoreName(String storeName) {
		this.storeName = storeName;
	}

	public String getRegion() {
		return region;
	}

	public void setRegion(String region) {
		this.region = region;
	}

	public String getAddress() {
		return address;
	}

	public void setAddress(String address) {
		this.address = address;
	}

	public String getDisplayName() {
		if (region == null || region.isBlank()) {
			return storeName;
		}
		return storeName + " - " + region;
	}
}