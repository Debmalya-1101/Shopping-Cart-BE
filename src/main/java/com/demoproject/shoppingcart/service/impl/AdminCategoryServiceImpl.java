package com.demoproject.shoppingcart.service.impl;

import com.demoproject.shoppingcart.dto.AdminAttributeKeyDTO;
import com.demoproject.shoppingcart.dto.CategoryDTO;
import com.demoproject.shoppingcart.dto.CreateCategoryRequest;
import com.demoproject.shoppingcart.exception.ResourceNotFoundException;
import com.demoproject.shoppingcart.model.AttributeKey;
import com.demoproject.shoppingcart.model.Category;
import com.demoproject.shoppingcart.repository.CategoryRepository;
import com.demoproject.shoppingcart.service.AdminCategoryService;
import jakarta.transaction.Transactional;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
@Transactional
public class AdminCategoryServiceImpl implements AdminCategoryService {

    private final CategoryRepository categoryRepository;

    public AdminCategoryServiceImpl(CategoryRepository categoryRepository) {
        this.categoryRepository = categoryRepository;
    }

    @Override
    public List<CategoryDTO> getAllCategories() {
        return categoryRepository.findAll()
                .stream()
                .map(this::toCategoryDTO)
                .collect(Collectors.toList());
    }

    @Override
    public CategoryDTO getCategoryById(Long id) {
        Category category = categoryRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Category", "id", id));
        return toCategoryDTO(category);
    }

    @Override
    public CategoryDTO createCategory(CreateCategoryRequest request) {
        // Reject duplicate name
        categoryRepository.findByName(request.getName().trim()).ifPresent(existing -> {
            throw new RuntimeException("Category with name '" + request.getName() + "' already exists");
        });

        Category category = new Category();
        category.setName(request.getName().trim());

        Category saved = categoryRepository.save(category);
        return toCategoryDTO(saved);
    }

    @Override
    public CategoryDTO updateCategory(Long id, CreateCategoryRequest request) {
        Category category = categoryRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Category", "id", id));

        // Reject duplicate name (ignore current record)
        categoryRepository.findByName(request.getName().trim())
                .filter(existing -> !existing.getId().equals(id))
                .ifPresent(existing -> {
                    throw new RuntimeException("Category with name '" + request.getName() + "' already exists");
                });

        category.setName(request.getName().trim());
        Category saved = categoryRepository.save(category);
        return toCategoryDTO(saved);
    }

    @Override
    public void deleteCategory(Long id) {
        Category category = categoryRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Category", "id", id));

        if (categoryRepository.hasProducts(id)) {
            throw new RuntimeException(
                    "Cannot delete category '" + category.getName() + "' because it has products linked to it. " +
                    "Please reassign or delete those products first.");
        }

        categoryRepository.delete(category);
    }

    // ==================== Mapper ====================

    private CategoryDTO toCategoryDTO(Category category) {
        List<AdminAttributeKeyDTO> keys = category.getAttributeKeys() == null
                ? List.of()
                : category.getAttributeKeys()
                        .stream()
                        .map(this::toAttributeKeyDTO)
                        .collect(Collectors.toList());

        return new CategoryDTO(category.getId(), category.getName(), keys);
    }

    private AdminAttributeKeyDTO toAttributeKeyDTO(AttributeKey key) {
        return new AdminAttributeKeyDTO(
                key.getId(),
                key.getKeyName(),
                key.getType() != null ? key.getType().name() : null,
                key.getCategory() != null ? key.getCategory().getId() : null,
                key.getCategory() != null ? key.getCategory().getName() : null
        );
    }
}
