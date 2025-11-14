package com.demoproject.shoppingcart.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.demoproject.shoppingcart.model.Cart;

public interface CartRepository extends JpaRepository<Cart, Long> {
	
	public Cart findByProductId(Long productId);
	public Cart findByProductIdAndUserId(Long productId, int userId);
	public List<Cart> findByUserId(int userId);
	public void deleteByProductIdAndUserId(Long productId, int userId);
	public void deleteByUserId(int userId);

}
