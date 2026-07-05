package com.demoproject.shoppingcart.controller;

import com.demoproject.shoppingcart.dto.PageResponse;
import com.demoproject.shoppingcart.dto.ProductAdminDTO;
import com.demoproject.shoppingcart.dto.CreateProductRequest;
import com.demoproject.shoppingcart.dto.UpdateProductRequest;
import com.demoproject.shoppingcart.dto.UpdateStockRequest;
import com.demoproject.shoppingcart.dto.UpdateProductStatusRequest;
import com.demoproject.shoppingcart.service.AdminProductService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/admin/products")

public class AdminProductController {

    private final AdminProductService adminProductService;

    public AdminProductController(AdminProductService adminProductService) {
        this.adminProductService = adminProductService;
    }

    /**
     * Create a new product
     * POST /api/admin/products
     */
    @PostMapping
    public ResponseEntity<ProductAdminDTO> createProduct(
            @Valid @RequestBody CreateProductRequest request) {
        ProductAdminDTO product = adminProductService.createProduct(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(product);
    }

    /**
     * Update an existing product
     * PUT /api/admin/products/{id}
     */
    @PutMapping("/{id}")
    public ResponseEntity<ProductAdminDTO> updateProduct(
            @PathVariable Long id,
            @Valid @RequestBody UpdateProductRequest request) {
        ProductAdminDTO product = adminProductService.updateProduct(id, request);
        return ResponseEntity.ok(product);
    }

    /**
     * Get product by ID (admin view)
     * GET /api/admin/products/{id}
     */
    @GetMapping("/{id}")
    public ResponseEntity<ProductAdminDTO> getProduct(@PathVariable Long id) {
        ProductAdminDTO product = adminProductService.getProductById(id);
        return ResponseEntity.ok(product);
    }

    /**
     * Get all products with filtering, pagination and sorting
     * GET /api/admin/products?page=0&size=10&category=Electronics&brand=Samsung&active=true&search=phone&sortBy=price&order=asc
     */
    @GetMapping
    public ResponseEntity<PageResponse<ProductAdminDTO>> getAllProducts(
            @RequestParam(required = false) String category,
            @RequestParam(required = false) String brand,
            @RequestParam(required = false) Boolean active,
            @RequestParam(required = false) String search,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "desc") String order,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {

        PageResponse<ProductAdminDTO> response = adminProductService.getAllProducts(
                category, brand, active, search, sortBy, order, page, size
        );
        return ResponseEntity.ok(response);
    }

    /**
     * Update product stock only
     * PATCH /api/admin/products/{id}/stock
     * @deprecated Use POST /api/admin/inventory/product/{id}/adjust instead
     */
    @Deprecated
    @PatchMapping("/{id}/stock")
    public ResponseEntity<String> updateStock(
            @PathVariable Long id,
            @Valid @RequestBody UpdateStockRequest request) {
        adminProductService.updateStock(id, request);
        return ResponseEntity.ok("Stock updated successfully");
    }

    /**
     * Update product status (active/inactive)
     * PATCH /api/admin/products/{id}/status
     */
    @PatchMapping("/{id}/status")
    public ResponseEntity<String> updateProductStatus(
            @PathVariable Long id,
            @Valid @RequestBody UpdateProductStatusRequest request) {
        adminProductService.updateProductStatus(id, request);
        return ResponseEntity.ok("Product status updated successfully");
    }

    /**
     * Soft delete a product
     * DELETE /api/admin/products/{id}
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<String> deleteProduct(@PathVariable Long id) {
        adminProductService.deleteProduct(id);
        return ResponseEntity.ok("Product deleted successfully");
    }
}



