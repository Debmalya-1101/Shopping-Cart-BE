package com.demoproject.shoppingcart.service;

import com.demoproject.shoppingcart.dto.ProductListDTO;
import java.util.List;

public interface RecommendationService {
    List<ProductListDTO> getRecommendations();
}
