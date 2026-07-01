package com.demoproject.shoppingcart.service;

import com.demoproject.shoppingcart.dto.PageResponse;
import com.demoproject.shoppingcart.dto.ProductAdminDTO;
import com.demoproject.shoppingcart.dto.CreateProductRequest;
import com.demoproject.shoppingcart.dto.UpdateProductRequest;
import com.demoproject.shoppingcart.dto.UpdateStockRequest;
import com.demoproject.shoppingcart.dto.UpdateProductStatusRequest;
import org.springframework.data.domain.Pageable;

public interface AdminProductService {

    ProductAdminDTO createProduct(CreateProductRequest request);

    ProductAdminDTO updateProduct(Long id, UpdateProductRequest request);

    ProductAdminDTO getProductById(Long id);

    PageResponse<ProductAdminDTO> getAllProducts(String category, String brand, Boolean active,
                                                  String search, String sortBy, String order,
                                                  int page, int size);

    void updateStock(Long id, UpdateStockRequest request);

    void updateProductStatus(Long id, UpdateProductStatusRequest request);

    void deleteProduct(Long id);
}

