package com.demoproject.shoppingcart.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.demoproject.shoppingcart.model.Product;

public interface ProductRepository extends JpaRepository<Product, Long>{
	
	public List<Product> findByPriceBetween(Long startprice, Long endprice);

}
