package com.demoproject.shoppingcart.repository;

import com.demoproject.shoppingcart.model.InventoryTransaction;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface InventoryTransactionRepository extends JpaRepository<InventoryTransaction, Long>, JpaSpecificationExecutor<InventoryTransaction> {
    List<InventoryTransaction> findByInventoryIdOrderByCreatedAtDesc(Long inventoryId);

    @org.springframework.data.jpa.repository.Query("SELECT p.id, p.name, " +
           "SUM(CASE WHEN t.transactionType = 'CONSUME' THEN t.quantity " +
           "WHEN t.transactionType IN ('CANCEL_RESTOCK', 'RETURN_RESTOCK') THEN -t.quantity ELSE 0 END) " +
           "FROM InventoryTransaction t " +
           "JOIN t.inventory i " +
           "JOIN i.product p " +
           "WHERE t.transactionType IN ('CONSUME', 'CANCEL_RESTOCK', 'RETURN_RESTOCK') AND t.createdAt >= :startDate " +
           "GROUP BY p.id, p.name " +
           "ORDER BY SUM(CASE WHEN t.transactionType = 'CONSUME' THEN t.quantity WHEN t.transactionType IN ('CANCEL_RESTOCK', 'RETURN_RESTOCK') THEN -t.quantity ELSE 0 END) DESC")
    List<Object[]> getFastMovingProducts(@org.springframework.data.repository.query.Param("startDate") java.time.LocalDateTime startDate, org.springframework.data.domain.Pageable pageable);

    @org.springframework.data.jpa.repository.Query("SELECT t.transactionType, SUM(t.quantity) " +
           "FROM InventoryTransaction t " +
           "WHERE t.createdAt >= :startDate " +
           "GROUP BY t.transactionType")
    List<Object[]> getTransactionSummaries(@org.springframework.data.repository.query.Param("startDate") java.time.LocalDateTime startDate);
}
