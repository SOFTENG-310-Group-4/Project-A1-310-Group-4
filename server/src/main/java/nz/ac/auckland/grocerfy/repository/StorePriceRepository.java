package nz.ac.auckland.grocerfy.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import nz.ac.auckland.grocerfy.model.StorePrice;

public interface StorePriceRepository extends JpaRepository<StorePrice, Long> {

	@Query("select sp from StorePrice sp join fetch sp.product join fetch sp.store")
	List<StorePrice> findAllWithRelations();
}