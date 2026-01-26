package org.chatapp.customshopify.repository;

import org.chatapp.customshopify.entity.ProductReview;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ProductReviewRepository extends JpaRepository<ProductReview, Long> {
    
    // Find reviews by shop and product
    Page<ProductReview> findByShopAndProductId(String shop, String productId, Pageable pageable);
    
    // Find all reviews by shop
    Page<ProductReview> findByShop(String shop, Pageable pageable);
    
    // Find recent reviews for a shop
    List<ProductReview> findTop10ByShopOrderByCreatedAtDesc(String shop);
    
    // Find reviews by shop and rating
    Page<ProductReview> findByShopAndRating(String shop, Integer rating, Pageable pageable);

    // Find reviews by shop, product, and rating
    Page<ProductReview> findByShopAndProductIdAndRating(String shop, String productId, Integer rating, Pageable pageable);

    // Stats
    Long countByShop(String shop);
    Long countByShopAndRating(String shop, Integer rating);

    @org.springframework.data.jpa.repository.Query("SELECT AVG(r.rating) FROM ProductReview r WHERE r.shop = :shop")
    Double getAverageRatingByShop(@org.springframework.data.repository.query.Param("shop") String shop);
}
