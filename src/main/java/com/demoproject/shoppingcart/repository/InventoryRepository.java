package com.demoproject.shoppingcart.repository;

import com.demoproject.shoppingcart.model.Inventory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.List;
import org.springframework.data.jpa.repository.Query;

@Repository
public interface InventoryRepository extends JpaRepository<Inventory, Long>, JpaSpecificationExecutor<Inventory> {
    Optional<Inventory> findByProductId(Long productId);

    @Query("SELECT COUNT(i) FROM Inventory i WHERE i.availableQuantity <= i.reorderLevel AND i.availableQuantity > 0")
    long countLowStock();

    @Query("SELECT COUNT(i) FROM Inventory i WHERE i.availableQuantity = 0")
    long countOutOfStock();

    @Query("SELECT SUM(i.availableQuantity * p.price), SUM(i.reservedQuantity * p.price), SUM(i.damagedQuantity * p.price) FROM Inventory i JOIN i.product p")
    Object[] getValuation();

    @Query("SELECT p.id, p.name, COALESCE(SUM(CASE WHEN t.transactionType = 'CONSUME' THEN t.quantity WHEN t.transactionType IN ('CANCEL_RESTOCK', 'RETURN_RESTOCK') THEN -t.quantity ELSE 0 END), 0L) " +
           "FROM Inventory i " +
           "JOIN i.product p " +
           "LEFT JOIN InventoryTransaction t ON t.inventory = i " +
           "AND t.transactionType IN ('CONSUME', 'CANCEL_RESTOCK', 'RETURN_RESTOCK') AND t.createdAt >= :startDate " +
           "GROUP BY p.id, p.name " +
           "ORDER BY COALESCE(SUM(CASE WHEN t.transactionType = 'CONSUME' THEN t.quantity WHEN t.transactionType IN ('CANCEL_RESTOCK', 'RETURN_RESTOCK') THEN -t.quantity ELSE 0 END), 0L) ASC")
    List<Object[]> getSlowMovingProducts(@org.springframework.data.repository.query.Param("startDate") java.time.LocalDateTime startDate, org.springframework.data.domain.Pageable pageable);
}
