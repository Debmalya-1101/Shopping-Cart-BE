package com.demoproject.shoppingcart.repository;

import com.demoproject.shoppingcart.model.ProductAttribute;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ProductAttributeRepository extends JpaRepository<ProductAttribute, Long> {
}

