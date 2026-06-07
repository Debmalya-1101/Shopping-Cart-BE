package com.demoproject.shoppingcart.repository;

import com.demoproject.shoppingcart.model.ProductImage;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ProductImageRepository extends JpaRepository<ProductImage, Long> {
}

