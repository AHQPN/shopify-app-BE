package org.chatapp.customshopify.repository;

import org.chatapp.customshopify.entity.ProductReview;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ProductReviewRepository extends JpaRepository<ProductReview, Long> {

    @Query("SELECT r FROM ProductReview r WHERE r.shop = :shop " +
            "AND (:productId IS NULL OR r.productId = :productId) " +
            "AND (:rating IS NULL OR r.rating = :rating) " +
            "AND (:status IS NULL OR r.status = :status)")
    Page<ProductReview> findFiltered(
            @Param("shop") String shop,
            @Param("productId") String productId,
            @Param("rating") Integer rating,
            @Param("status") Boolean status,
            Pageable pageable);

    @Query("SELECT COUNT(r) FROM ProductReview r WHERE r.shop = :shop " +
            "AND (:productId IS NULL OR r.productId = :productId) " +
            "AND (:status IS NULL OR r.status = :status)")
    Long countFiltered(@Param("shop") String shop,
            @Param("productId") String productId,
            @Param("status") Boolean status);

    @Query("SELECT COUNT(r) FROM ProductReview r WHERE r.shop = :shop " +
            "AND (:productId IS NULL OR r.productId = :productId) " +
            "AND r.rating = :rating " +
            "AND (:status IS NULL OR r.status = :status)")
    Long countFilteredWithRating(@Param("shop") String shop,
            @Param("productId") String productId,
            @Param("rating") Integer rating,
            @Param("status") Boolean status);

    @Query("SELECT AVG(r.rating) FROM ProductReview r WHERE r.shop = :shop " +
            "AND (:productId IS NULL OR r.productId = :productId) " +
            "AND (:status IS NULL OR r.status = :status)")
    Double getAverageRatingFiltered(@Param("shop") String shop,
            @Param("productId") String productId,
            @Param("status") Boolean status);

    Optional<ProductReview> getProductReviewById(Long id);

    List<ProductReview> findTop10ByShopOrderByCreatedAtDesc(String shop);
}
