package com.demoproject.shoppingcart.service.impl;

import com.demoproject.shoppingcart.dto.PageResponse;
import com.demoproject.shoppingcart.dto.ProductAdminDTO;
import com.demoproject.shoppingcart.dto.AdminAttributeDTO;
import com.demoproject.shoppingcart.dto.CreateProductRequest;
import com.demoproject.shoppingcart.dto.UpdateProductRequest;
import com.demoproject.shoppingcart.dto.UpdateStockRequest;
import com.demoproject.shoppingcart.dto.UpdateProductStatusRequest;
import com.demoproject.shoppingcart.exception.ResourceNotFoundException;
import com.demoproject.shoppingcart.model.Product;
import com.demoproject.shoppingcart.model.ProductAttribute;
import com.demoproject.shoppingcart.model.ProductImage;
import com.demoproject.shoppingcart.model.Category;
import com.demoproject.shoppingcart.model.AttributeKey;
import com.demoproject.shoppingcart.model.AttributeType;
import com.demoproject.shoppingcart.repository.ProductRepository;
import com.demoproject.shoppingcart.repository.CategoryRepository;
import com.demoproject.shoppingcart.repository.AttributeKeyRepository;
import com.demoproject.shoppingcart.service.AdminProductService;
import com.demoproject.shoppingcart.specification.AdminProductSpecifications;
import jakarta.transaction.Transactional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.stream.Collectors;

@Service
@Transactional
public class AdminProductServiceImpl implements AdminProductService {

    private final ProductRepository productRepository;
    private final CategoryRepository categoryRepository;
    private final AttributeKeyRepository attributeKeyRepository;

    public AdminProductServiceImpl(ProductRepository productRepository,
                                  CategoryRepository categoryRepository,
                                  AttributeKeyRepository attributeKeyRepository) {
        this.productRepository = productRepository;
        this.categoryRepository = categoryRepository;
        this.attributeKeyRepository = attributeKeyRepository;
    }

    @Override
    public ProductAdminDTO createProduct(CreateProductRequest request) {
        // Fetch and validate category
        Category category = categoryRepository.findById(request.getCategoryId())
                .orElseThrow(() -> new ResourceNotFoundException("Category", "id", request.getCategoryId()));

        // Create new product
        Product product = new Product();
        product.setName(request.getName());
        product.setPrice(request.getPrice());
        product.setDescription(request.getDescription());
        product.setBrand(request.getBrand());
        product.setStock(request.getStock());
        product.setCategory(category);
        product.setImageUrl(request.getImageUrl());
        // Use active from request; default to true if not provided
        product.setActive(request.getActive() != null ? request.getActive() : true);
        product.setRating(0.0);

        // Add attributes
        if (request.getAttributes() != null && !request.getAttributes().isEmpty()) {
            for (AdminAttributeDTO attrDTO : request.getAttributes()) {
                AttributeKey attributeKey = attributeKeyRepository.findById(attrDTO.getKeyId())
                        .orElseThrow(() -> new ResourceNotFoundException("AttributeKey", "id", attrDTO.getKeyId()));

                ProductAttribute attr = new ProductAttribute();
                attr.setAttributeKey(attributeKey);
                attr.setValueText(attrDTO.getValue());
                // Also populate valueNumber for NUMBER-typed attributes
                if (AttributeType.NUMBER.equals(attributeKey.getType()) && attrDTO.getValue() != null) {
                    try {
                        attr.setValueNumber(Double.parseDouble(attrDTO.getValue()));
                    } catch (NumberFormatException ignored) {
                        // value is not a valid number; leave valueNumber null
                    }
                }
                product.addAttribute(attr);
            }
        }

        // Add additional images
        if (request.getAdditionalImageUrls() != null && !request.getAdditionalImageUrls().isEmpty()) {
            for (String imageUrl : request.getAdditionalImageUrls()) {
                ProductImage image = new ProductImage();
                image.setImageUrl(imageUrl);
                product.addImage(image);
            }
        }

        Product savedProduct = productRepository.save(product);
        return toProductAdminDTO(savedProduct);
    }

    @Override
    public ProductAdminDTO updateProduct(Long id, UpdateProductRequest request) {
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Product", "id", id));

        Category category = categoryRepository.findById(request.getCategoryId())
                .orElseThrow(() -> new ResourceNotFoundException("Category", "id", request.getCategoryId()));

        // Update basic fields
        product.setName(request.getName());
        product.setPrice(request.getPrice());
        product.setDescription(request.getDescription());
        product.setBrand(request.getBrand());
        product.setStock(request.getStock());
        product.setCategory(category);
        product.setImageUrl(request.getImageUrl());
        // Update active status if explicitly provided
        if (request.getActive() != null) {
            product.setActive(request.getActive());
        }

        // Clear and update attributes
        product.getAttributes().clear();
        if (request.getAttributes() != null && !request.getAttributes().isEmpty()) {
            for (AdminAttributeDTO attrDTO : request.getAttributes()) {
                AttributeKey attributeKey = attributeKeyRepository.findById(attrDTO.getKeyId())
                        .orElseThrow(() -> new ResourceNotFoundException("AttributeKey", "id", attrDTO.getKeyId()));

                ProductAttribute attr = new ProductAttribute();
                attr.setAttributeKey(attributeKey);
                attr.setValueText(attrDTO.getValue());
                // Also populate valueNumber for NUMBER-typed attributes
                if (AttributeType.NUMBER.equals(attributeKey.getType()) && attrDTO.getValue() != null) {
                    try {
                        attr.setValueNumber(Double.parseDouble(attrDTO.getValue()));
                    } catch (NumberFormatException ignored) {
                        // value is not a valid number; leave valueNumber null
                    }
                }
                product.addAttribute(attr);
            }
        }

        // Clear and update images
        product.getImages().clear();
        if (request.getAdditionalImageUrls() != null && !request.getAdditionalImageUrls().isEmpty()) {
            for (String imageUrl : request.getAdditionalImageUrls()) {
                ProductImage image = new ProductImage();
                image.setImageUrl(imageUrl);
                product.addImage(image);
            }
        }

        Product savedProduct = productRepository.save(product);
        return toProductAdminDTO(savedProduct);
    }

    @Override
    public ProductAdminDTO getProductById(Long id) {
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Product", "id", id));
        return toProductAdminDTO(product);
    }

    @Override
    public PageResponse<ProductAdminDTO> getAllProducts(String category, String brand, Boolean active,
                                                       String search, String sortBy, String order,
                                                       int page, int size) {
        // Validate & map sort field
        String sortField = mapSortField(sortBy);
        Sort.Direction direction = "asc".equalsIgnoreCase(order)
                ? Sort.Direction.ASC
                : Sort.Direction.DESC;

        Pageable pageable = PageRequest.of(page, size, Sort.by(direction, sortField));

        // Build specification for admin products (no activeOnly filter - admin sees all)
        Specification<Product> spec = AdminProductSpecifications.buildSpecification(
                category, brand, search, active
        );

        Page<Product> productPage = productRepository.findAll(spec, pageable);

        List<ProductAdminDTO> dtoList = productPage.getContent()
                .stream()
                .map(this::toProductAdminDTO)
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
    public void updateStock(Long id, UpdateStockRequest request) {
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Product", "id", id));

        product.setStock(request.getStock());
        productRepository.save(product);
    }

    @Override
    public void updateProductStatus(Long id, UpdateProductStatusRequest request) {
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Product", "id", id));

        product.setActive(request.getActive());
        productRepository.save(product);
    }

    @Override
    public void deleteProduct(Long id) {
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Product", "id", id));

        // Soft delete: set active to false
        product.setActive(false);
        productRepository.save(product);
    }

    // ============ Helper Methods ============

    private ProductAdminDTO toProductAdminDTO(Product product) {
        Long categoryId = product.getCategory() != null ? product.getCategory().getId() : null;
        String categoryName = product.getCategory() != null ? product.getCategory().getName() : null;

        List<String> imageUrls = product.getImages()
                .stream()
                .map(ProductImage::getImageUrl)
                .collect(Collectors.toList());

        List<AdminAttributeDTO> attributes = product.getAttributes()
                .stream()
                .map(this::toAdminAttributeDTO)
                .collect(Collectors.toList());

        return new ProductAdminDTO(
                product.getId(),
                product.getName(),
                product.getDescription(),
                product.getPrice(),
                product.getStock(),
                product.getActive(),
                product.getBrand(),
                categoryId,
                categoryName,
                product.getRating(),
                product.getImageUrl(),    // primary / base image
                imageUrls,                // additional images
                attributes,
                product.getCreatedAt(),
                product.getUpdatedAt()
        );
    }

    private AdminAttributeDTO toAdminAttributeDTO(ProductAttribute attr) {
        AttributeKey key = attr.getAttributeKey();
        Long keyId = key != null ? key.getId() : null;
        String keyName = key != null ? key.getKeyName() : null;

        // Prefer valueText; fall back to valueNumber as string
        String value = StringUtils.hasText(attr.getValueText())
                ? attr.getValueText()
                : (attr.getValueNumber() != null ? String.valueOf(attr.getValueNumber()) : null);

        return new AdminAttributeDTO(keyId, keyName, value);
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
        if ("stock".equalsIgnoreCase(sortBy)) {
            return "stock";
        }
        // Default: createdAt (for newest first)
        return "createdAt";
    }
}

