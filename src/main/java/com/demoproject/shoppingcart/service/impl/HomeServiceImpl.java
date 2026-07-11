package com.demoproject.shoppingcart.service.impl;

import com.demoproject.shoppingcart.dto.ProductListDTO;
import com.demoproject.shoppingcart.model.Product;
import com.demoproject.shoppingcart.repository.ProductRepository;
import com.demoproject.shoppingcart.service.HomeService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@Transactional(readOnly = true)
public class HomeServiceImpl implements HomeService {

    private final ProductRepository productRepository;

    public HomeServiceImpl(ProductRepository productRepository) {
        this.productRepository = productRepository;
    }

    @Override
    public List<ProductListDTO> getFeaturedProducts() {
        return productRepository.findTop15ByActiveTrueOrderByRatingDescRatingCountDesc().stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    @Override
    public List<ProductListDTO> getNewArrivals() {
        return productRepository.findTop15ByActiveTrueOrderByCreatedAtDesc().stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    private ProductListDTO convertToDTO(Product product) {
        String categoryName = product.getCategory() != null ? product.getCategory().getName() : null;
        return new ProductListDTO(
                product.getId(),
                product.getName(),
                product.getPrice(),
                product.getImageUrl(),
                product.getRating(),
                product.getRatingCount(),
                product.getActive(),
                product.getBrand(),
                categoryName
        );
    }
}
