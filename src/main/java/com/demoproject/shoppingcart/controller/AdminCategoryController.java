package com.demoproject.shoppingcart.controller;

import com.demoproject.shoppingcart.dto.CategoryDTO;
import com.demoproject.shoppingcart.dto.CreateCategoryRequest;
import com.demoproject.shoppingcart.service.AdminCategoryService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/admin/categories")
@CrossOrigin(origins = "*")
public class AdminCategoryController {

    private final AdminCategoryService adminCategoryService;

    public AdminCategoryController(AdminCategoryService adminCategoryService) {
        this.adminCategoryService = adminCategoryService;
    }

    /**
     * List all categories with their attribute keys.
     * GET /api/admin/categories
     */
    @GetMapping
    public ResponseEntity<List<CategoryDTO>> getAllCategories() {
        return ResponseEntity.ok(adminCategoryService.getAllCategories());
    }

    /**
     * Get a single category (with its attribute keys).
     * GET /api/admin/categories/{id}
     */
    @GetMapping("/{id}")
    public ResponseEntity<CategoryDTO> getCategoryById(@PathVariable Long id) {
        return ResponseEntity.ok(adminCategoryService.getCategoryById(id));
    }

    /**
     * Create a new category.
     * POST /api/admin/categories
     */
    @PostMapping
    public ResponseEntity<CategoryDTO> createCategory(@Valid @RequestBody CreateCategoryRequest request) {
        CategoryDTO created = adminCategoryService.createCategory(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    /**
     * Update an existing category's name.
     * PUT /api/admin/categories/{id}
     */
    @PutMapping("/{id}")
    public ResponseEntity<CategoryDTO> updateCategory(
            @PathVariable Long id,
            @Valid @RequestBody CreateCategoryRequest request) {
        return ResponseEntity.ok(adminCategoryService.updateCategory(id, request));
    }

    /**
     * Delete a category permanently.
     * Fails with 400 if any products are still linked to this category.
     * DELETE /api/admin/categories/{id}
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<String> deleteCategory(@PathVariable Long id) {
        adminCategoryService.deleteCategory(id);
        return ResponseEntity.ok("Category deleted successfully");
    }
}
