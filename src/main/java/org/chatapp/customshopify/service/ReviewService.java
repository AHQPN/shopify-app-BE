package org.chatapp.customshopify.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.chatapp.customshopify.dto.request.CreateReviewRequest;
import org.chatapp.customshopify.entity.ProductReview;
import org.chatapp.customshopify.entity.ReviewMedia;
import org.chatapp.customshopify.exception.AppException;
import org.chatapp.customshopify.exception.ErrorCode;
import org.chatapp.customshopify.repository.ProductReviewRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class ReviewService {

    private final ProductReviewRepository reviewRepository;

    @Transactional
    public ProductReview createReview(String shop, CreateReviewRequest request) {
        log.info("Creating review for product {} in shop {}", request.getProductId(), shop);

        if (request.getRating() == null || request.getRating() < 1 || request.getRating() > 5) {
            throw new AppException(ErrorCode.INVALID_REQUEST);
        }

        ProductReview review = ProductReview.builder()
                .shop(shop)
                .productId(request.getProductId())
                .customerId(request.getCustomerId())
                .customerName(request.getCustomerName())
                .avatarUrl(request.getAvatarUrl())
                .comment(request.getComment())
                .rating(request.getRating())
                .build();

        List<ReviewMedia> mediaList = new ArrayList<>();
        if (request.getMedia() != null) {
            for (CreateReviewRequest.MediaItem item : request.getMedia()) {
                mediaList.add(ReviewMedia.builder()
                        .review(review)
                        .mediaUrl(item.getUrl())
                        .mediaType(item.getType())
                        .fileSize(item.getFileSize())
                        .build());
            }
        }
        review.setMedia(mediaList);

        return reviewRepository.save(review);
    }

    public Page<ProductReview> getReviews(String shop, String productId, Integer rating, Pageable pageable) {
        if (productId != null && !productId.isEmpty()) {
            if (rating != null) {
                return reviewRepository.findByShopAndProductIdAndRating(shop, productId, rating, pageable);
            }
            return reviewRepository.findByShopAndProductId(shop, productId, pageable);
        } else {
            if (rating != null) {
                return reviewRepository.findByShopAndRating(shop, rating, pageable);
            }
            return reviewRepository.findByShop(shop, pageable);
        }
    }

    public ReviewStats getReviewStats(String shop) {
        Double avg = reviewRepository.getAverageRatingByShop(shop);
        
        return ReviewStats.builder()
                .totalReviews(reviewRepository.countByShop(shop))
                .averageRating(avg != null ? Math.round(avg * 10.0) / 10.0 : 0.0)
                .oneStar(reviewRepository.countByShopAndRating(shop, 1))
                .twoStars(reviewRepository.countByShopAndRating(shop, 2))
                .threeStars(reviewRepository.countByShopAndRating(shop, 3))
                .fourStars(reviewRepository.countByShopAndRating(shop, 4))
                .fiveStars(reviewRepository.countByShopAndRating(shop, 5))
                .build();
    }
    
    @lombok.Data
    @lombok.Builder
    public static class ReviewStats {
        private Long totalReviews;
        private Double averageRating;
        private Long oneStar;
        private Long twoStars;
        private Long threeStars;
        private Long fourStars;
        private Long fiveStars;
    }
}
