package com.demoproject.shoppingcart.controller;

import com.demoproject.shoppingcart.dto.ProductListDTO;
import com.demoproject.shoppingcart.service.RecommendationService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/recommendations")
public class RecommendationController {

    private final RecommendationService recommendationService;

    public RecommendationController(RecommendationService recommendationService) {
        this.recommendationService = recommendationService;
    }

    @GetMapping
    public List<ProductListDTO> getRecommendations() {
        return recommendationService.getRecommendations();
    }
}
