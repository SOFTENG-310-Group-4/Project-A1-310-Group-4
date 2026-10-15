package nz.ac.auckland.grocerfy.service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import nz.ac.auckland.grocerfy.dto.BasketComparisonRequest;
import nz.ac.auckland.grocerfy.dto.BasketComparisonResponse;
import nz.ac.auckland.grocerfy.dto.BasketItemRequest;
import nz.ac.auckland.grocerfy.dto.Coordinates;
import nz.ac.auckland.grocerfy.dto.MissingItemResponse;
import nz.ac.auckland.grocerfy.dto.MissingItemResponse.MissingReason;
import nz.ac.auckland.grocerfy.dto.ProductSummaryResponse;
import nz.ac.auckland.grocerfy.dto.StoreComparisonResponse;
import nz.ac.auckland.grocerfy.dto.StoreLineItemResponse;
import nz.ac.auckland.grocerfy.model.LoyaltyProgram;
import nz.ac.auckland.grocerfy.model.Product;
import nz.ac.auckland.grocerfy.model.StockStatus;
import nz.ac.auckland.grocerfy.model.Store;
import nz.ac.auckland.grocerfy.model.StorePrice;
import nz.ac.auckland.grocerfy.repository.ProductRepository;
import nz.ac.auckland.grocerfy.repository.StoreRepository;
import nz.ac.auckland.grocerfy.repository.StorePriceRepository;

/**
 * Service responsible for comparing a user basket across all stores.
 * It determines whether each store can fulfil the full basket (including whether it has
 * enough stock of each item, when the stock level is known), lists missing items,
 * and identifies the cheapest available store.
 */
@Service
public class BasketComparisonService {

	private final ProductRepository productRepository;
	private final StoreRepository storeRepository;
	private final StorePriceRepository storePriceRepository;
  private final StoreLocationLookup storeLocations;

	public BasketComparisonService(ProductRepository productRepository, StoreRepository storeRepository,
			StorePriceRepository storePriceRepository, StoreLocationLookup storeLocations) {
		this.productRepository = productRepository;
		this.storeRepository = storeRepository;
		this.storePriceRepository = storePriceRepository;
		this.storeLocations = storeLocations;
	}

	/**
	 * Compare a basket payload against all stores in the catalogue.
	 *
	 * @param request basket request with product IDs and quantities, and optionally the shopper's
	 *        loyalty programmes (member prices are used instead of shelf prices for those)
	 * @return response containing store comparison results and the cheapest available store
	 */
	@Transactional(readOnly = true)
	public BasketComparisonResponse compareBasket(BasketComparisonRequest request) {
		List<BasketItemRequest> basketItems = request == null || request.items() == null ? List.of() : request.items();
		if (basketItems.isEmpty()) {
			throw new IllegalArgumentException("Basket must contain at least one item.");
		}
		// loyalty programmes the shopper belongs to; their member prices replace shelf prices
		Set<LoyaltyProgram> memberships = request.loyaltyPrograms() == null
				? Set.of()
				: request.loyaltyPrograms().stream().filter(Objects::nonNull).collect(Collectors.toUnmodifiableSet());

		// Normalize basket items by product ID and sum duplicate quantities.
		Map<Long, BasketItemRequest> normalizedItems = new LinkedHashMap<>();
		for (BasketItemRequest item : basketItems) {
			if (item == null || item.productId() == null) {
				throw new IllegalArgumentException("Each basket item must include a productId.");
			}
			if (item.quantity() <= 0) {
				throw new IllegalArgumentException("Basket item quantities must be greater than zero.");
			}

			normalizedItems.merge(item.productId(), new BasketItemRequest(item.productId(), item.quantity()),
				(existing, next) -> new BasketItemRequest(existing.productId(), existing.quantity() + next.quantity()));
		}

		List<Product> requestedProducts = productRepository.findAllById(normalizedItems.keySet());
		Map<Long, Product> productsById = requestedProducts.stream()
				.collect(Collectors.toMap(Product::getId, product -> product));

		if (productsById.size() != normalizedItems.size()) {
			List<Long> missingProductIds = normalizedItems.keySet().stream()
					.filter(productId -> !productsById.containsKey(productId))
					.toList();
			throw new IllegalArgumentException("Unknown product ids: " + missingProductIds);
		}

		List<StorePrice> allStorePrices = storePriceRepository.findAllWithRelations();
		Map<Long, List<StorePrice>> pricesByStore = allStorePrices.stream()
				.collect(Collectors.groupingBy(storePrice -> storePrice.getStore().getId()));

		List<StoreComparisonResponse> stores = storeRepository.findAll().stream()
				.map(store -> toStoreComparison(store, pricesByStore.getOrDefault(store.getId(), List.of()), normalizedItems, productsById, memberships))
				.sorted(Comparator.comparing(StoreComparisonResponse::available).reversed()
						.thenComparing(StoreComparisonResponse::availableSubtotal, Comparator.nullsLast(Comparator.naturalOrder())))
				.toList();

		StoreComparisonResponse cheapestAvailableStore = stores.stream()
				.filter(StoreComparisonResponse::available)
				.min(Comparator.comparing(StoreComparisonResponse::availableSubtotal))
				.orElse(null);

		StoreComparisonResponse cheapestStore = cheapestAvailableStore != null ? cheapestAvailableStore : stores.stream()
				.min(Comparator.comparing(StoreComparisonResponse::availableSubtotal))
				.orElse(null);

		List<ProductSummaryResponse> requestedItemSummaries = normalizedItems.values().stream()
				.map(item -> {
					Product product = productsById.get(item.productId());
					return new ProductSummaryResponse(product.getId(), product.getProductName());
				})
				.toList();

		return new BasketComparisonResponse(requestedItemSummaries, stores, cheapestAvailableStore, cheapestStore);
	}

	private StoreComparisonResponse toStoreComparison(Store store,
			List<StorePrice> storePrices,
			Map<Long, BasketItemRequest> basketItems,
			Map<Long, Product> productsById,
			Set<LoyaltyProgram> memberships) {
		Map<Long, StorePrice> storePricesByProduct = storePrices.stream()
				.collect(Collectors.toMap(price -> price.getProduct().getId(), price -> price));

		List<StoreLineItemResponse> lineItems = new ArrayList<>();
		List<MissingItemResponse> missingItems = new ArrayList<>();
		BigDecimal subtotal = BigDecimal.ZERO;
		BigDecimal memberSavingApplied = BigDecimal.ZERO;
		BigDecimal memberSavingAvailable = BigDecimal.ZERO;
		LoyaltyProgram storeProgram = null;

		for (BasketItemRequest item : basketItems.values()) {
			Product product = productsById.get(item.productId());
			StorePrice price = storePricesByProduct.get(item.productId());
			// A store that does not sell the product, or cannot cover the requested
			// quantity from its current stock, cannot fulfil this basket line.
			if (price == null) {
				missingItems.add(new MissingItemResponse(product.getId(), product.getProductName(), item.quantity(),
						MissingReason.NOT_STOCKED, 0));
				continue;
			}
			Integer stockQuantity = price.getStockQuantity();
			if (stockQuantity != null && stockQuantity < item.quantity()) {
				MissingReason reason = price.getStockStatus() == StockStatus.OUT_OF_STOCK
						? MissingReason.OUT_OF_STOCK
						: MissingReason.INSUFFICIENT_STOCK;
				missingItems.add(new MissingItemResponse(product.getId(), product.getProductName(), item.quantity(),
						reason, Math.max(stockQuantity, 0)));
				continue;
			}

			BigDecimal quantity = BigDecimal.valueOf(item.quantity());
			BigDecimal regularUnitPrice = price.getPrice().setScale(2, RoundingMode.HALF_UP);
			BigDecimal unitPrice = regularUnitPrice;
			boolean memberPriceApplied = false;

			if (hasMemberDiscount(price)) {
				storeProgram = price.getLoyaltyProgram();
				BigDecimal memberUnitPrice = price.getMemberPrice().setScale(2, RoundingMode.HALF_UP);
				BigDecimal lineSaving = regularUnitPrice.subtract(memberUnitPrice).multiply(quantity);
				if (memberships.contains(price.getLoyaltyProgram())) {
					unitPrice = memberUnitPrice;
					memberPriceApplied = true;
					memberSavingApplied = memberSavingApplied.add(lineSaving);
				} else {
					memberSavingAvailable = memberSavingAvailable.add(lineSaving);
				}
			}

			BigDecimal lineTotal = unitPrice.multiply(quantity).setScale(2, RoundingMode.HALF_UP);
			subtotal = subtotal.add(lineTotal);
			lineItems.add(new StoreLineItemResponse(
					product.getId(),
					product.getProductName(),
					item.quantity(),
					unitPrice,
					lineTotal,
					regularUnitPrice,
					memberPriceApplied,
					price.getStockStatus()));
		}

    Coordinates location = storeLocations.find(store.getName());
		boolean available = missingItems.isEmpty();
		BigDecimal availableSubtotal = subtotal.setScale(2, RoundingMode.HALF_UP);
		return new StoreComparisonResponse(
				store.getId(),
				store.getName(),
				store.getRegion(),
				store.getAddress(),
				available,
				availableSubtotal,
				lineItems,
				missingItems,
				storeProgram,
				memberSavingApplied.setScale(2, RoundingMode.HALF_UP),
				memberSavingAvailable.setScale(2, RoundingMode.HALF_UP),
        location == null ? null : location.latitude(),
        location == null ? null : location.longitude()
    );
	}

	/**
	 * A store price offers a member discount when it has a member price, the programme it belongs to,
	 * and the member price is actually lower than the shelf price.
	 */
	private static boolean hasMemberDiscount(StorePrice price) {
		return price.getMemberPrice() != null
				&& price.getLoyaltyProgram() != null
				&& price.getMemberPrice().compareTo(price.getPrice()) < 0;
	}
}