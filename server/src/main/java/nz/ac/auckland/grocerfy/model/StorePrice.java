package nz.ac.auckland.grocerfy.model;

import java.math.BigDecimal;
import java.util.Objects;

import org.hibernate.annotations.Immutable;

import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

@Entity
@Immutable
@Table(name = "store_prices",
    uniqueConstraints = @UniqueConstraint(columnNames = {"product_id", "store_id"})
)
public class StorePrice {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "product_id", nullable = false)
    private Product product;

    @ManyToOne
    @JoinColumn(name = "store_id", nullable = false)
    private Store store;

    private BigDecimal price;

    /** Lower price for members of loyaltyProgram, or null if the product has no member price. */
    private BigDecimal memberPrice;

    @Enumerated(EnumType.STRING)
    private LoyaltyProgram loyaltyProgram;

    public StorePrice() { }

    public StorePrice(Product product, Store store, BigDecimal price) {
        this(product, store, price, null, null);
    }

    public StorePrice(Product product, Store store, BigDecimal price, BigDecimal memberPrice, LoyaltyProgram loyaltyProgram) {
        this.product = product;
        this.store = store;
        this.price = price;
        // a member price only makes sense together with the programme it belongs to
        boolean hasMemberPrice = memberPrice != null && loyaltyProgram != null;
        this.memberPrice = hasMemberPrice ? memberPrice : null;
        this.loyaltyProgram = hasMemberPrice ? loyaltyProgram : null;
    }

    public Product getProduct() {
        return product;
    }

    public Store getStore() {
        return store;
    }

    public BigDecimal getPrice() {
        return price;
    }

    public BigDecimal getMemberPrice() {
        return memberPrice;
    }

    public LoyaltyProgram getLoyaltyProgram() {
        return loyaltyProgram;
    }

    @Override 
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (!(obj instanceof StorePrice)) {
            return false;
        }

        StorePrice compPrice = (StorePrice) obj;
        return (getProduct().getId().equals(compPrice.getProduct().getId())
                && getStore().getId().equals(compPrice.getStore().getId()));
    }

    @Override 
    public int hashCode() {
        return Objects.hash(getProduct(), getStore());
    }
}
