package com.demoproject.shoppingcart.repository;

import com.demoproject.shoppingcart.model.Faq;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface FaqRepository extends JpaRepository<Faq, Long> {
    List<Faq> findByActiveTrue();
}
