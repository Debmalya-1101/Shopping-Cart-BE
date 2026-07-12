package com.demoproject.shoppingcart.controller;

import com.demoproject.shoppingcart.dto.ProductListDTO;
import com.demoproject.shoppingcart.service.HomeService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/home")
public class HomeController {

    private final HomeService homeService;

    public HomeController(HomeService homeService) {
        this.homeService = homeService;
    }

    @GetMapping("/featured-products")
    public ResponseEntity<List<ProductListDTO>> getFeaturedProducts() {
        return ResponseEntity.ok(homeService.getFeaturedProducts());
    }

    @GetMapping("/new-arrivals")
    public ResponseEntity<List<ProductListDTO>> getNewArrivals() {
        return ResponseEntity.ok(homeService.getNewArrivals());
    }
}
