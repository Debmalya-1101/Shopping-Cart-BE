package com.demoproject.shoppingcart.service;

import com.demoproject.shoppingcart.dto.PageResponse;
import com.demoproject.shoppingcart.dto.ProductDetailDTO;
import com.demoproject.shoppingcart.dto.ProductListDTO;

public interface ProductService {

    PageResponse<ProductListDTO> getAllProducts(int page,
                                                int size,
                                                String category,
                                                String brand,
                                                String search,
                                                Long minPrice,
                                                Long maxPrice,
                                                String sortBy,
                                                String order);


    ProductDetailDTO getProductById(Long id);
}
