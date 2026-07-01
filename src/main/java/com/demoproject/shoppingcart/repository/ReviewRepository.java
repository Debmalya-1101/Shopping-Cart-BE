package com.demoproject.shoppingcart.repository;

import com.demoproject.shoppingcart.model.ProductReview;
import com.demoproject.shoppingcart.model.Product;
import com.demoproject.shoppingcart.model.AppUser;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface ReviewRepository extends JpaRepository<ProductReview, Long> {

    Page<ProductReview> findByProduct(Product product, Pageable pageable);

    Optional<ProductReview> findByProductAndUser(Product product, AppUser user);

    boolean existsByProductAndUser(Product product, AppUser user);
}

