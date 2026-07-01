package com.demoproject.shoppingcart.specification;

import com.demoproject.shoppingcart.model.InventoryTransaction;
import com.demoproject.shoppingcart.model.InventoryTransactionType;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class InventoryTransactionSpecifications {

    public static Specification<InventoryTransaction> buildSpecification(Long inventoryId,
                                                                         InventoryTransactionType transactionType,
                                                                         String referenceType,
                                                                         String referenceId,
                                                                         LocalDateTime startDate,
                                                                         LocalDateTime endDate) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            if (inventoryId != null) {
                predicates.add(cb.equal(root.get("inventory").get("id"), inventoryId));
            }

            if (transactionType != null) {
                predicates.add(cb.equal(root.get("transactionType"), transactionType));
            }

            if (StringUtils.hasText(referenceType)) {
                predicates.add(cb.equal(root.get("referenceType"), referenceType));
            }

            if (StringUtils.hasText(referenceId)) {
                predicates.add(cb.equal(root.get("referenceId"), referenceId));
            }

            if (startDate != null) {
                predicates.add(cb.greaterThanOrEqualTo(root.get("createdAt"), startDate));
            }

            if (endDate != null) {
                predicates.add(cb.lessThanOrEqualTo(root.get("createdAt"), endDate));
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }
}
