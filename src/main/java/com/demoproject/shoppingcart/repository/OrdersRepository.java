package com.demoproject.shoppingcart.repository;

import java.util.List;
import java.util.Map;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.demoproject.shoppingcart.model.Orders;
import com.demoproject.shoppingcart.model.graphData;

public interface OrdersRepository extends JpaRepository<Orders, Long>{
	public List<Orders> findByUserId(int userId);
	
	@Query("SELECT new com.demoproject.shoppingcart.model.graphData(MONTH(o.orderDate) AS m, COUNT(DISTINCT o.orderId))FROM Orders as o where year(o.orderDate)=?1 GROUP BY m")
	public List<graphData> getGraphData(@Param("orderDate") int year);
}
