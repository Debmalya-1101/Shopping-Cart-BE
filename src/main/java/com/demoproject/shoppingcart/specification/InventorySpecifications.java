package com.demoproject.shoppingcart.specification;

import com.demoproject.shoppingcart.model.Inventory;
import com.demoproject.shoppingcart.model.Product;
import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.JoinType;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.List;

public class InventorySpecifications {

    public static Specification<Inventory> buildSpecification(Long productId, String productName, Boolean lowStock, Boolean outOfStock) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            Join<Inventory, Product> productJoin = root.join("product", JoinType.INNER);

            if (productId != null) {
                predicates.add(cb.equal(productJoin.get("id"), productId));
            }

            if (StringUtils.hasText(productName)) {
                predicates.add(cb.like(cb.lower(productJoin.get("name")), "%" + productName.toLowerCase() + "%"));
            }

            if (Boolean.TRUE.equals(outOfStock)) {
                predicates.add(cb.equal(root.get("availableQuantity"), 0));
            } else if (Boolean.TRUE.equals(lowStock)) {
                predicates.add(cb.lessThanOrEqualTo(root.get("availableQuantity"), root.get("reorderLevel")));
                // Also could mean available > 0, but technically 0 is also lowStock.
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }
}
