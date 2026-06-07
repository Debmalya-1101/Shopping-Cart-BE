package com.demoproject.shoppingcart.repository;

import com.demoproject.shoppingcart.model.AttributeKey;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface AttributeKeyRepository extends JpaRepository<AttributeKey, Long> {

    List<AttributeKey> findByCategoryId(Long categoryId);

    @Query("SELECT COUNT(pa) > 0 FROM ProductAttribute pa WHERE pa.attributeKey.id = :keyId")
    boolean isUsedByProducts(@Param("keyId") Long keyId);
}


