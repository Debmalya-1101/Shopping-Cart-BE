package com.demoproject.shoppingcart.service;

import com.demoproject.shoppingcart.dto.ProductListDTO;
import java.util.List;

public interface HomeService {
    List<ProductListDTO> getFeaturedProducts();
    List<ProductListDTO> getNewArrivals();
}
