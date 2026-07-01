package com.demoproject.shoppingcart.service.impl;

import com.demoproject.shoppingcart.dto.AttributeDTO;
import com.demoproject.shoppingcart.dto.PageResponse;
import com.demoproject.shoppingcart.dto.ProductDetailDTO;
import com.demoproject.shoppingcart.dto.ProductListDTO;
import com.demoproject.shoppingcart.exception.ResourceNotFoundException;
import com.demoproject.shoppingcart.model.Product;
import com.demoproject.shoppingcart.model.ProductAttribute;
import com.demoproject.shoppingcart.model.ProductImage;
import com.demoproject.shoppingcart.repository.ProductRepository;
import com.demoproject.shoppingcart.service.ProductService;
import com.demoproject.shoppingcart.specification.ProductSpecifications;
import jakarta.transaction.Transactional;
import org.springframework.data.domain.*;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class ProductServiceImpl implements ProductService {

    private final ProductRepository productRepository;

    public ProductServiceImpl(ProductRepository productRepository) {
        this.productRepository = productRepository;
    }

    @Override
    public PageResponse<ProductListDTO> getAllProducts(int page,
                                                       int size,
                                                       String category,
                                                       String brand,
                                                       String search,
                                                       Long minPrice,
                                                       Long maxPrice,
                                                       String sortBy,
                                                       String order) {

        // Validate & map sort field
        String sortField = mapSortField(sortBy);
        Sort.Direction direction = "asc".equalsIgnoreCase(order)
                ? Sort.Direction.ASC
                : Sort.Direction.DESC;

        Pageable pageable = PageRequest.of(page, size, Sort.by(direction, sortField));

        Specification<Product> spec = ProductSpecifications.buildSpecification(
                category, brand, search, minPrice, maxPrice
        );

        Page<Product> productPage = productRepository.findAll(spec, pageable);

        List<ProductListDTO> dtoList = productPage
                .getContent()
                .stream()
                .map(this::toProductListDTO)
                .collect(Collectors.toList());

        return new PageResponse<>(
                dtoList,
                productPage.getNumber(),
                productPage.getSize(),
                productPage.getTotalElements(),
                productPage.getTotalPages(),
                productPage.isLast()
        );
    }

    @Override
    @Transactional
    public ProductDetailDTO getProductById(Long id) {
        Product product = productRepository.findById(id)
                .orElseThrow(() ->
                        new ResourceNotFoundException("Product", "id", id));

        // If inactive, treat as not found for user APIs
        if (Boolean.FALSE.equals(product.getActive())) {
            throw new ResourceNotFoundException("Product", "id", id);
        }

        return toProductDetailDTO(product);
    }

    @Override
    public List<String> getDistinctCategoryNames() {
        return productRepository.findDistinctCategoryNames();
    }

    @Override
    public List<String> getDistinctBrands() {
        return productRepository.findDistinctBrands();
    }

    // ----------------- Mapping helpers -----------------

    private ProductListDTO toProductListDTO(Product product) {
        String categoryName = product.getCategory() != null
                ? product.getCategory().getName()
                : null;

        Long ratingCount = product.getRatingCount() != null ? product.getRatingCount() : 0L;

        return new ProductListDTO(
                product.getId(),
                product.getName(),
                product.getPrice(),
                product.getImageUrl(),
                product.getRating(),
                ratingCount,
                product.getActive(),
                product.getBrand(),
                categoryName
        );
    }

    private ProductDetailDTO toProductDetailDTO(Product product) {
        String categoryName = product.getCategory() != null
                ? product.getCategory().getName()
                : null;

        List<String> gallery = product.getImages()
                .stream()
                .map(ProductImage::getImageUrl)
                .collect(Collectors.toList());

        List<AttributeDTO> specs = product.getAttributes()
                .stream()
                .map(this::toAttributeDTO)
                .collect(Collectors.toList());

        Long ratingCount = product.getRatingCount() != null ? product.getRatingCount() : 0L;

        return new ProductDetailDTO(
                product.getId(),
                product.getName(),
                product.getFullName(),
                product.getDescription(),
                product.getPrice(),
                product.getImageUrl(),
                product.getRating(),
                ratingCount,
                product.getActive(),
                product.getBrand(),
                categoryName,
                gallery,
                specs
        );
    }

    private AttributeDTO toAttributeDTO(ProductAttribute attr) {
        String key = attr.getAttributeKey() != null
                ? attr.getAttributeKey().getKeyName()
                : null;

        // Prefer valueText as display value; you can later enhance to use valueNumber
        String value = null;
        if (StringUtils.hasText(attr.getValueText())) {
            value = attr.getValueText();
        } else if (attr.getValueNumber() != null) {
            value = String.valueOf(attr.getValueNumber());
        }

        return new AttributeDTO(key, value);
    }

    private String mapSortField(String sortBy) {
        if ("price".equalsIgnoreCase(sortBy)) {
            return "price";
        }
        if ("rating".equalsIgnoreCase(sortBy)) {
            return "rating";
        }
        if ("name".equalsIgnoreCase(sortBy)) {
            return "name";
        }
        // Default: createdAt (for "latest products first")
        return "createdAt";
    }
}
