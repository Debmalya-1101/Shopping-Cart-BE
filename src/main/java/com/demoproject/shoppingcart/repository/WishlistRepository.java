package com.demoproject.shoppingcart.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.demoproject.shoppingcart.model.Wishlist;

public interface WishlistRepository extends JpaRepository<Wishlist, Long> {
	
	//public void deleteByProductId(Long productId);
	public List<Wishlist> findByUserId(int userId);
	public void deleteByProductIdAndUserId(Long productId, int userId);

}
