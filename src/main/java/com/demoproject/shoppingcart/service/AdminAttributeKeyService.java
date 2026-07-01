package com.demoproject.shoppingcart.service;

import com.demoproject.shoppingcart.dto.AdminAttributeKeyDTO;
import com.demoproject.shoppingcart.dto.CreateAttributeKeyRequest;

import java.util.List;

public interface AdminAttributeKeyService {

    /**
     * Returns all attribute keys, optionally filtered by category.
     * Pass null for categoryId to return all keys across all categories.
     */
    List<AdminAttributeKeyDTO> getAllAttributeKeys(Long categoryId);

    /** Returns a single attribute key by ID. */
    AdminAttributeKeyDTO getAttributeKeyById(Long id);

    /** Creates a new attribute key under the specified category. */
    AdminAttributeKeyDTO createAttributeKey(CreateAttributeKeyRequest request);

    /** Updates an existing attribute key's name, type, and/or category. */
    AdminAttributeKeyDTO updateAttributeKey(Long id, CreateAttributeKeyRequest request);

    /**
     * Deletes an attribute key permanently.
     * Fails if any product currently uses this attribute key.
     */
    void deleteAttributeKey(Long id);
}
