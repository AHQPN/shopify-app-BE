package org.chatapp.customshopify.specification;

import jakarta.persistence.criteria.JoinType;
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
            String productName,
            Boolean showHiddenMedia) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

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
                if (isRead) {
                    // Trường hợp read = true: (Status là PUBLISHED) HOẶC (hideReason là IS NOT
                    // NULL)
                    predicates.add(cb.or(
                            cb.equal(root.get("status"), ReviewStatus.PUBLISHED),
                            cb.isNotNull(root.get("hideReason"))));
                } else {
                    predicates.add(cb.notEqual(root.get("status"), ReviewStatus.PUBLISHED));
                    predicates.add(cb.isNull(root.get("hideReason")));
                }
            }

            if (productName != null && !productName.isBlank()) {
                predicates.add(
                        cb.like(
                                cb.lower(root.get("productName")),
                                "%" + productName.toLowerCase() + "%"));
            }

            if (Boolean.FALSE.equals(showHiddenMedia)) {
                // If this is a data fetch query (not a count query), we use FETCH to avoid N+1.
                // We no longer add a predicate here to filter the review entity itself based on
                // media,
                // because that would hide reviews where ALL media are hidden.
                // Media filtering will be handled in the Service layer.
                if (query.getResultType() != Long.class && query.getResultType() != long.class) {
                    root.fetch("media", JoinType.LEFT);
                }
                query.distinct(true);
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }
}
