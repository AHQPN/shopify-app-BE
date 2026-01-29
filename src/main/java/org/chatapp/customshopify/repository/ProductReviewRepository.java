package org.chatapp.customshopify.repository;

import org.chatapp.customshopify.dto.response.ReviewStatsResponse;
import org.chatapp.customshopify.entity.ProductReview;
import org.chatapp.customshopify.enums.ReviewStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

@Repository
public interface ProductReviewRepository
                extends JpaRepository<ProductReview, Long>, JpaSpecificationExecutor<ProductReview> {

        // @Query("""
        // SELECT r FROM ProductReview r
        // WHERE r.shop = :shop
        // AND (:productId IS NULL OR r.productId = :productId)
        // AND (:rating IS NULL OR r.rating = :rating)
        // AND (:statuses IS NULL OR r.status IN :statuses)
        // AND (:onlyParents IS NULL
        // OR (:onlyParents = true AND r.replyTo IS NULL))
        // AND (:isRead IS NULL OR r.isRead = :isRead)
        //
        // """)
        // Page<ProductReview> findFiltered(
        // @Param("shop") String shop,
        // @Param("productId") String productId,
        // @Param("rating") Integer rating,
        // @Param("statuses") Collection<ReviewStatus> statuses,
        // @Param("onlyParents") Boolean onlyParents,
        // @Param("isRead") Boolean isRead,
        // @Param("productName") String productName,
        // Pageable pageable
        // );

        @Query("SELECT COUNT(r) FROM ProductReview r WHERE r.shop = :shop " +
                        "AND (:productId IS NULL OR r.productId = :productId) " +
                        "AND ((:statuses) IS NULL OR r.status IN (:statuses)) ")
        Long countFiltered(@Param("shop") String shop,
                        @Param("productId") String productId,
                        @Param("statuses") Collection<ReviewStatus> statuses);

        @Query("SELECT new org.chatapp.customshopify.dto.response.ReviewStatsResponse(" +
                        "COUNT(r), " +
                        "AVG(r.rating), " +
                        "SUM(CASE WHEN r.rating = 1 THEN 1 ELSE 0 END), " +
                        "SUM(CASE WHEN r.rating = 2 THEN 1 ELSE 0 END), " +
                        "SUM(CASE WHEN r.rating = 3 THEN 1 ELSE 0 END), " +
                        "SUM(CASE WHEN r.rating = 4 THEN 1 ELSE 0 END), " +
                        "SUM(CASE WHEN r.rating = 5 THEN 1 ELSE 0 END), " +
                        "SUM(CASE WHEN r.isRead = false OR r.isRead IS NULL THEN 1 ELSE 0 END)" +
                        ") " +
                        "FROM ProductReview r " +
                        "WHERE r.shop = :shop " +
                        "AND (:productId IS NULL OR r.productId = :productId) " +
                        "AND r.status IN :statuses")
        ReviewStatsResponse getReviewStats(
                        @Param("shop") String shop,
                        @Param("productId") String productId,
                        @Param("statuses") Collection<ReviewStatus> statuses);

        @Query("SELECT AVG(r.rating) FROM ProductReview r WHERE r.shop = :shop " +
                        "AND (:productId IS NULL OR r.productId = :productId) " +
                        "AND ((:statuses) IS NULL OR r.status IN (:statuses)) ")
        Double getAverageRatingFiltered(@Param("shop") String shop,
                        @Param("productId") String productId,
                        @Param("statuses") Collection<ReviewStatus> statuses);

        Optional<ProductReview> getProductReviewById(Long id);

        List<ProductReview> findTop10ByShopOrderByCreatedAtDesc(String shop);

        @Modifying
        @Query("UPDATE ProductReview r SET r.isRead = :isRead WHERE r.id IN :ids")
        void updateReadStatus(@Param("ids") List<Long> ids, @Param("isRead") Boolean isRead);

        @Modifying
        @Query("UPDATE ProductReview r SET r.isPinned = :isPinned WHERE r.id = :id")
        void updatePinnedStatus(@Param("id") Long id, @Param("isPinned") Boolean isPinned);

        default Page<ProductReview> findAllSorted(Specification<ProductReview> spec, int page, int size,
                        boolean isAdmin) {
                Sort sort;
                if (isAdmin) {
                        // Admin: Newest first
                        sort = Sort.by(Sort.Order.desc("createdAt"));
                } else {
                        // Storefront: Pinned first, then newest first
                        sort = Sort.by(
                                        Sort.Order.desc("isPinned").nullsLast(),
                                        Sort.Order.desc("createdAt"));
                }
                return findAll(spec, PageRequest.of(page, size, sort));
        }
}
