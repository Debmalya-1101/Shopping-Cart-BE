package com.demoproject.shoppingcart.service;

import com.demoproject.shoppingcart.dto.CategoryDTO;
import com.demoproject.shoppingcart.dto.CreateCategoryRequest;

import java.util.List;

public interface AdminCategoryService {

    /** Returns all categories, each with their attribute keys. */
    List<CategoryDTO> getAllCategories();

    /** Returns a single category with its attribute keys. */
    CategoryDTO getCategoryById(Long id);

    /** Creates a new category. Fails if the name already exists. */
    CategoryDTO createCategory(CreateCategoryRequest request);

    /** Updates the name of an existing category. Fails if the new name already exists. */
    CategoryDTO updateCategory(Long id, CreateCategoryRequest request);

    /**
     * Deletes a category permanently.
     * Fails if there are any products associated with this category.
     */
    void deleteCategory(Long id);
}
