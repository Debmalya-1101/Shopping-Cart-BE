package com.demoproject.shoppingcart.specification;

import com.demoproject.shoppingcart.model.Product;
import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.JoinType;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.util.StringUtils;

public class AdminProductSpecifications {

    public static Specification<Product> hasCategory(String categoryName) {
        return (root, query, cb) -> {
            if (!StringUtils.hasText(categoryName)) {
                return cb.conjunction();
            }
            Join<Object, Object> categoryJoin = root.join("category", JoinType.LEFT);
            return cb.equal(cb.lower(categoryJoin.get("name")), categoryName.toLowerCase());
        };
    }

    public static Specification<Product> hasBrand(String brand) {
        return (root, query, cb) -> {
            if (!StringUtils.hasText(brand)) {
                return cb.conjunction();
            }
            return cb.equal(cb.lower(root.get("brand")), brand.toLowerCase());
        };
    }

    public static Specification<Product> nameContains(String search) {
        return (root, query, cb) -> {
            if (!StringUtils.hasText(search)) {
                return cb.conjunction();
            }
            String pattern = "%" + search.toLowerCase() + "%";
            return cb.like(cb.lower(root.get("name")), pattern);
        };
    }

    public static Specification<Product> isActive(Boolean active) {
        return (root, query, cb) -> {
            if (active == null) {
                return cb.conjunction();
            }
            return cb.equal(root.get("active"), active);
        };
    }

    public static Specification<Product> buildSpecification(String category,
                                                            String brand,
                                                            String search,
                                                            Boolean active) {
        return Specification.allOf(
                hasCategory(category),
                hasBrand(brand),
                nameContains(search),
                isActive(active)
        );
    }
}

