package nz.ac.auckland.grocerfy.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import nz.ac.auckland.grocerfy.model.Product;

public interface ProductRepository extends JpaRepository<Product, Long> {
}