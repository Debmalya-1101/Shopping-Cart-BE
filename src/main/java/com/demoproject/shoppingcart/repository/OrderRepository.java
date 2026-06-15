package com.demoproject.shoppingcart.repository;

import com.demoproject.shoppingcart.model.AppUser;
import com.demoproject.shoppingcart.model.Order;
import com.demoproject.shoppingcart.model.OrderStatus;
import com.demoproject.shoppingcart.model.Product;
import com.demoproject.shoppingcart.model.PaymentStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface OrderRepository extends JpaRepository<Order, Long> {
    List<Order> findByUserOrderByCreatedAtDesc(AppUser user);

    Page<Order> findAll(Pageable pageable);

    Page<Order> findByStatus(OrderStatus status, Pageable pageable);

    List<Order> findByStatusAndPaymentStatusAndUpdatedAtBefore(OrderStatus status, PaymentStatus paymentStatus, java.time.LocalDateTime time);

    // Check if user has a successful payment order containing this product
    @Query("SELECT CASE WHEN COUNT(o) > 0 THEN true ELSE false END " +
           "FROM Order o JOIN o.items oi JOIN oi.product p " +
           "WHERE o.user = :user AND p = :product AND o.paymentStatus = :paymentStatus")
    boolean hasSuccessfulOrderForProduct(@Param("user") AppUser user, 
                                        @Param("product") Product product,
                                        @Param("paymentStatus") PaymentStatus paymentStatus);

    // Count total orders with successful payments
    @Query("SELECT COUNT(o) FROM Order o WHERE o.paymentStatus = 'SUCCESS'")
    Long countSuccessfulOrders();

    // Sum total revenue from successful orders
    @Query("SELECT COALESCE(SUM(o.total), 0) FROM Order o WHERE o.paymentStatus = 'SUCCESS'")
    Long totalRevenue();

    // Count orders by status
    @Query("SELECT o.status, COUNT(o) FROM Order o WHERE o.paymentStatus = 'SUCCESS' GROUP BY o.status")
    List<Object[]> countOrdersByStatus();

    // Monthly sales data
    @Query("SELECT MONTH(o.createdAt), COUNT(o), SUM(o.total) " +
           "FROM Order o WHERE o.paymentStatus = 'SUCCESS' " +
           "GROUP BY MONTH(o.createdAt) " +
           "ORDER BY MONTH(o.createdAt) ASC")
    List<Object[]> getMonthlySales();

    // Top selling products
    @Query("SELECT oi.product.id, oi.product.name, CAST(SUM(oi.quantity) AS long), " +
           "CAST(SUM(oi.quantity * oi.price) AS long), oi.product.rating " +
           "FROM OrderItem oi " +
           "JOIN oi.order o " +
           "WHERE o.paymentStatus = 'SUCCESS' " +
           "GROUP BY oi.product.id, oi.product.name, oi.product.rating " +
           "ORDER BY SUM(oi.quantity) DESC")
    List<Object[]> getTopSellingProducts();
}





