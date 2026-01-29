package org.chatapp.customshopify.specification;

import jakarta.persistence.criteria.Predicate;
import org.chatapp.customshopify.entity.ProductReview;
import org.chatapp.customshopify.enums.ReviewStatus;
import org.springframework.data.jpa.domain.Specification;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public class ProductReviewSpecification {
    public static Specification<ProductReview> filter(
            String shop,
            String productId,
            Integer rating,
            Collection<ReviewStatus> statuses,
            Boolean isRead,
            String productName) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            // bắt buộc
            predicates.add(cb.equal(root.get("shop"), shop));

            if (productId != null) {
                predicates.add(cb.equal(root.get("productId"), productId));
            }

            if (rating != null) {
                predicates.add(cb.equal(root.get("rating"), rating));
            }

            if (statuses != null && !statuses.isEmpty()) {
                predicates.add(root.get("status").in(statuses));
            }

            if (isRead != null) {
                predicates.add(cb.equal(root.get("isRead"), isRead));
            }

            if (productName != null && !productName.isBlank()) {
                predicates.add(
                        cb.like(
                                cb.lower(root.get("productName")),
                                "%" + productName.toLowerCase() + "%"));
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }
}
