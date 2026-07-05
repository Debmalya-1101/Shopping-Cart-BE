package com.demoproject.shoppingcart.controller;

import com.demoproject.shoppingcart.dto.PageResponse;
import com.demoproject.shoppingcart.dto.ProductDetailDTO;
import com.demoproject.shoppingcart.dto.ProductListDTO;
import com.demoproject.shoppingcart.service.ProductService;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/products")
// adjust later if needed
public class ProductController {

    private final ProductService productService;

    public ProductController(ProductService productService) {
        this.productService = productService;
    }

    // GET /api/products?page=0&size=12&category=...&brand=...&search=...&sortBy=price&order=asc
    @GetMapping
    public PageResponse<ProductListDTO> getProducts(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "12") int size,
            @RequestParam(required = false) String category,
            @RequestParam(required = false) String brand,
            @RequestParam(required = false) String search,
            @RequestParam(required = false) Long minPrice,
            @RequestParam(required = false) Long maxPrice,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "desc") String order
    ) {
        return productService.getAllProducts(
                page, size, category, brand, search, minPrice, maxPrice, sortBy, order
        );
    }


    // GET /api/products/{id}
    @GetMapping("/{id}")
    public ProductDetailDTO getProduct(@PathVariable Long id) {
        return productService.getProductById(id);
    }

    // GET /api/products/categories
    @GetMapping("/categories")
    public java.util.List<String> getCatalogCategories() {
        return productService.getDistinctCategoryNames();
    }

    // GET /api/products/brands
    @GetMapping("/brands")
    public java.util.List<String> getCatalogBrands() {
        return productService.getDistinctBrands();
    }
}
