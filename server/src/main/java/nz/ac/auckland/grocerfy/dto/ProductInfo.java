package nz.ac.auckland.grocerfy.dto;

import java.math.BigDecimal;

/**
 * A product as read from a supermarket catalogue page.
 * @param memberPrice lower price for loyalty-programme members (e.g. Everyday Rewards), or null if none
 */
public record ProductInfo(String productName, BigDecimal price, String productSize, String productId, BigDecimal memberPrice) {

    /**
     * A product without a member price.
     */
    public ProductInfo(String productName, BigDecimal price, String productSize, String productId) {
        this(productName, price, productSize, productId, null);
    }
}
