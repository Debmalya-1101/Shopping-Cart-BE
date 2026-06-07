package com.demoproject.shoppingcart.service.impl;

import com.demoproject.shoppingcart.dto.AdminAttributeKeyDTO;
import com.demoproject.shoppingcart.dto.CreateAttributeKeyRequest;
import com.demoproject.shoppingcart.exception.ResourceNotFoundException;
import com.demoproject.shoppingcart.model.AttributeKey;
import com.demoproject.shoppingcart.model.Category;
import com.demoproject.shoppingcart.repository.AttributeKeyRepository;
import com.demoproject.shoppingcart.repository.CategoryRepository;
import com.demoproject.shoppingcart.service.AdminAttributeKeyService;
import jakarta.transaction.Transactional;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
@Transactional
public class AdminAttributeKeyServiceImpl implements AdminAttributeKeyService {

    private final AttributeKeyRepository attributeKeyRepository;
    private final CategoryRepository categoryRepository;

    public AdminAttributeKeyServiceImpl(AttributeKeyRepository attributeKeyRepository,
                                        CategoryRepository categoryRepository) {
        this.attributeKeyRepository = attributeKeyRepository;
        this.categoryRepository = categoryRepository;
    }

    @Override
    public List<AdminAttributeKeyDTO> getAllAttributeKeys(Long categoryId) {
        List<AttributeKey> keys = (categoryId != null)
                ? attributeKeyRepository.findByCategoryId(categoryId)
                : attributeKeyRepository.findAll();

        return keys.stream()
                .map(this::toDTO)
                .collect(Collectors.toList());
    }

    @Override
    public AdminAttributeKeyDTO getAttributeKeyById(Long id) {
        AttributeKey key = attributeKeyRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("AttributeKey", "id", id));
        return toDTO(key);
    }

    @Override
    public AdminAttributeKeyDTO createAttributeKey(CreateAttributeKeyRequest request) {
        Category category = categoryRepository.findById(request.getCategoryId())
                .orElseThrow(() -> new ResourceNotFoundException("Category", "id", request.getCategoryId()));

        AttributeKey key = new AttributeKey();
        key.setKeyName(request.getKeyName().trim());
        key.setType(request.getType());
        key.setCategory(category);

        AttributeKey saved = attributeKeyRepository.save(key);
        return toDTO(saved);
    }

    @Override
    public AdminAttributeKeyDTO updateAttributeKey(Long id, CreateAttributeKeyRequest request) {
        AttributeKey key = attributeKeyRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("AttributeKey", "id", id));

        Category category = categoryRepository.findById(request.getCategoryId())
                .orElseThrow(() -> new ResourceNotFoundException("Category", "id", request.getCategoryId()));

        key.setKeyName(request.getKeyName().trim());
        key.setType(request.getType());
        key.setCategory(category);

        AttributeKey saved = attributeKeyRepository.save(key);
        return toDTO(saved);
    }

    @Override
    public void deleteAttributeKey(Long id) {
        AttributeKey key = attributeKeyRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("AttributeKey", "id", id));

        if (attributeKeyRepository.isUsedByProducts(id)) {
            throw new RuntimeException(
                    "Cannot delete attribute key '" + key.getKeyName() + "' because it is currently " +
                    "used by one or more products. Remove it from those products first.");
        }

        attributeKeyRepository.delete(key);
    }

    // ==================== Mapper ====================

    private AdminAttributeKeyDTO toDTO(AttributeKey key) {
        return new AdminAttributeKeyDTO(
                key.getId(),
                key.getKeyName(),
                key.getType() != null ? key.getType().name() : null,
                key.getCategory() != null ? key.getCategory().getId() : null,
                key.getCategory() != null ? key.getCategory().getName() : null
        );
    }
}
