package nz.ac.auckland.grocerfy.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import nz.ac.auckland.grocerfy.model.Product;

public interface ProductRepository extends JpaRepository<Product, Long> {

    /**
     * Search products by name substring and dietary tags
     *
     * The name pattern is matched case-insensitively, callers pass an
     * already-lowercased LIKE pattern (for example "%milk%", or "%" to match
     * everything).
     * Each dietary flag is an independent AND clause, when a flag is false the
     * clause is satisfied by every row, and when true only products carrying that
     * tag survive.
     * @param pattern     lowercased SQL LIKE pattern for the product name
     * @param lactoseFree restrict results to lactose free products
     * @param glutenFree  restrict results to gluten free products
     * @param vegetarian  restrict results to vegetarian products
     * @param vegan       restrict results to vegan products
     * @return matching products ordered by name
     */
    @Query("""
			SELECT p FROM Product p
			WHERE LOWER(p.productName) LIKE :pattern
			  AND (:lactoseFree = FALSE OR p.lactoseFree = TRUE)
			  AND (:glutenFree  = FALSE OR p.glutenFree  = TRUE)
			  AND (:vegetarian  = FALSE OR p.vegetarian  = TRUE)
			  AND (:vegan       = FALSE OR p.vegan       = TRUE)
			ORDER BY p.productName
			""")
    List<Product> search(@Param("pattern") String pattern,
                         @Param("lactoseFree") boolean lactoseFree,
                         @Param("glutenFree") boolean glutenFree,
                         @Param("vegetarian") boolean vegetarian,
                         @Param("vegan") boolean vegan);
}