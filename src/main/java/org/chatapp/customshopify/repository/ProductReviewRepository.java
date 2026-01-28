package org.chatapp.customshopify.repository;

import org.chatapp.customshopify.dto.response.ReviewStatsResponse;
import org.chatapp.customshopify.entity.ProductReview;
import org.chatapp.customshopify.enums.ReviewStatus;
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
                        "AND ((:statuses) IS NULL OR r.status IN (:statuses)) " +
                        "AND (:onlyParents IS NULL OR (:onlyParents = true AND r.replyTo IS NULL))")
        Long countFiltered(@Param("shop") String shop,
                        @Param("productId") String productId,
                        @Param("statuses") Collection<ReviewStatus> statuses,
                        @Param("onlyParents") Boolean onlyParents);

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
                        "AND r.replyTo IS NULL " +
                        "AND r.status IN :statuses")

        ReviewStatsResponse getReviewStats(
                        @Param("shop") String shop,
                        @Param("productId") String productId,
                        @Param("statuses") Collection<ReviewStatus> statuses);

        @Query("SELECT AVG(r.rating) FROM ProductReview r WHERE r.shop = :shop " +
                        "AND (:productId IS NULL OR r.productId = :productId) " +
                        "AND ((:statuses) IS NULL OR r.status IN (:statuses)) " +
                        "AND (:onlyParents IS NULL OR (:onlyParents = true AND r.replyTo IS NULL))")
        Double getAverageRatingFiltered(@Param("shop") String shop,
                        @Param("productId") String productId,
                        @Param("statuses") Collection<ReviewStatus> statuses,
                        @Param("onlyParents") Boolean onlyParents);

        Optional<ProductReview> getProductReviewById(Long id);

        List<ProductReview> findTop10ByShopOrderByCreatedAtDesc(String shop);

        List<ProductReview> getProductReviewByReplyTo(Long id);

        @Modifying
        @Query("""
                            UPDATE ProductReview r
                            SET r.status = :status
                            WHERE r.replyTo = :id
                              AND r.status <> 'ARCHIVED'
                        """)
        void updateReply(@Param("id") Long id,
                        @Param("status") ReviewStatus status);

        @Modifying
        @Query("""
                            UPDATE ProductReview r
                            SET r.isRead = true
                            WHERE r.id IN :reviews
                        """)
        void updateReadStatus(@Param("reviews") List<Long> reviews);

        List<ProductReview> getProductReviewByReplyToAndIsReadFalse(Long id);

        List<ProductReview> getProductReviewByReplyToAndIsReadTrue(Long id);

        Long countByReplyToAndIsReadFalse(Long id);
}
