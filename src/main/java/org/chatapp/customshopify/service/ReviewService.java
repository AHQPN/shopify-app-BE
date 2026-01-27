package org.chatapp.customshopify.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.chatapp.customshopify.dto.request.CreateReviewRequest;
import org.chatapp.customshopify.dto.request.UpdateReviewStatusRequest;
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

        if (request.getReplyTo() == null) {
            if (request.getRating() == null || request.getRating() < 1 || request.getRating() > 5) {
                throw new AppException(ErrorCode.INVALID_REQUEST);
            }
        }

        ProductReview review = ProductReview.builder()
                .shop(shop)
                .productId(request.getProductId())
                .productName(request.getProductName())
                .customerId(request.getCustomerId())
                .customerName(request.getCustomerName())
                .avatarUrl(request.getAvatarUrl())
                .comment(request.getComment())
                .rating(request.getRating())
                .replyTo(request.getReplyTo())
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
        review.setStatus(false); // Default to unpublished
        return reviewRepository.save(review);
    }

    public Page<ProductReview> getReviews(String shop, String productId, Integer rating, Boolean status,
            Pageable pageable) {
        return reviewRepository.findFiltered(shop, productId, rating, status, pageable);
    }

    public ReviewStats getReviewStats(String shop, String productId, Boolean status) {
        Double avg = reviewRepository.getAverageRatingFiltered(shop, productId, status);

        return ReviewStats.builder()
                .totalReviews(reviewRepository.countFiltered(shop, productId, status))
                .averageRating(avg != null ? Math.round(avg * 10.0) / 10.0 : 0.0)
                .oneStar(reviewRepository.countFilteredWithRating(shop, productId, 1, status))
                .twoStars(reviewRepository.countFilteredWithRating(shop, productId, 2, status))
                .threeStars(reviewRepository.countFilteredWithRating(shop, productId, 3, status))
                .fourStars(reviewRepository.countFilteredWithRating(shop, productId, 4, status))
                .fiveStars(reviewRepository.countFilteredWithRating(shop, productId, 5, status))
                .build();
    }

    @Transactional
    public void updateReviewStatus(UpdateReviewStatusRequest request) {
        ProductReview productReview = reviewRepository.getProductReviewById(request.getId())
                .orElseThrow(() -> new AppException(ErrorCode.INVALID_REQUEST));
        productReview.setStatus(request.getStatus());
        productReview.setHideReason(request.getHideReason());
        reviewRepository.save(productReview);
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
