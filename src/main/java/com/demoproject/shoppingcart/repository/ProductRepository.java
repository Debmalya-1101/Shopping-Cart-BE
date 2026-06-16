package com.demoproject.shoppingcart.repository;

import com.demoproject.shoppingcart.model.Product;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import java.util.List;

public interface ProductRepository extends JpaRepository<Product, Long>,
        JpaSpecificationExecutor<Product> {

    @Query("SELECT DISTINCT p.category.name FROM Product p WHERE p.active = true AND p.category IS NOT NULL ORDER BY p.category.name")
    List<String> findDistinctCategoryNames();

    @Query("SELECT DISTINCT p.brand FROM Product p WHERE p.active = true AND p.brand IS NOT NULL ORDER BY p.brand")
    List<String> findDistinctBrands();
}
