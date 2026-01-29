package org.chatapp.customshopify.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.chatapp.customshopify.dto.request.CreateReviewRequest;
import org.chatapp.customshopify.dto.request.UpdateReviewStatusRequest;
import org.chatapp.customshopify.dto.response.ReviewStatsResponse;
import org.chatapp.customshopify.entity.ProductReview;
import org.chatapp.customshopify.entity.ReviewMedia;
import org.chatapp.customshopify.enums.ReviewStatus;
import org.chatapp.customshopify.exception.AppException;
import org.chatapp.customshopify.exception.ErrorCode;
import org.chatapp.customshopify.repository.ProductReviewRepository;
import org.chatapp.customshopify.specification.ProductReviewSpecification;
import org.springframework.data.domain.Page;
import org.springframework.data.jpa.domain.Specification;
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
        log.info("Creating/Updating review for product {} in shop {}", request.getProductId(), shop);

        if (request.getReplyTo() != null) {
            // Handle Direct Reply: Update parent review's reply field
            ProductReview parent = reviewRepository.getProductReviewById(request.getReplyTo())
                    .orElseThrow(() -> new AppException(ErrorCode.INVALID_REQUEST));

            parent.setReply(request.getComment());
            // Optionally reset status or mark as read? For now just save reply.
            return reviewRepository.save(parent);
        }

        // New Review submission
        if (request.getRating() == null || request.getRating() < 1 || request.getRating() > 5) {
            throw new AppException(ErrorCode.INVALID_REQUEST);
        }

        ProductReview review = ProductReview.builder()
                .shop(shop)
                .productId(request.getProductId())
                .productName(request.getProductName())
                .customerId(request.getCustomerId())
                .customerName(request.getCustomerName())
                .comment(request.getComment())
                .rating(request.getRating())
                .isAnonymous(request.getIsAnonymous() != null ? request.getIsAnonymous() : false)
                .status(ReviewStatus.HIDDEN) // Default HIDDEN
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

    public Page<ProductReview> getReviews(String shop, String productId, Integer rating, Boolean status, Boolean isRead,
            String productName, int page, int size, boolean isAdmin) {
        // NOTE: 'status' param here is from Controller (true=PUBLISHED,
        // false=HIDDEN/ARCHIVED?).
        // But Controller logic is:
        // Admin (Auth Header): pass status=null (get all) or true/false filter.
        // Storefront (No Auth): pass status=true (only published).

        List<ReviewStatus> statusList = new ArrayList<>();
        if (Boolean.TRUE.equals(status)) {
            // Storefront behavior: Only PUBLISHED
            statusList.add(ReviewStatus.PUBLISHED);
        } else {
            // Admin behavior
            if (status == null) {
                statusList.add(ReviewStatus.PUBLISHED);
                statusList.add(ReviewStatus.HIDDEN); // != ARCHIVED
            } else {
                statusList.add(ReviewStatus.HIDDEN);
            }
        }
        if (productName != null && productName.isBlank()) {
            productName = null;
        }

        Specification<ProductReview> spec = ProductReviewSpecification.filter(
                shop,
                productId,
                rating,
                statusList,
                isRead,
                productName);

        return reviewRepository.findAllSorted(spec, page, size, isAdmin);
    }

    public ReviewStatsResponse getReviewStats(String shop, String productId, Boolean status) {

        List<ReviewStatus> statusList = new ArrayList<>();
        // Stats follow specific admin/storefront logic:

        if (Boolean.TRUE.equals(status)) {
            statusList.add(ReviewStatus.PUBLISHED);
        } else {
            // Admin stats: Not Archived
            statusList.add(ReviewStatus.PUBLISHED);
            statusList.add(ReviewStatus.HIDDEN);
        }

        ReviewStatsResponse p = reviewRepository.getReviewStats(shop, productId, statusList);

        if (p == null || p.getTotalReviews() == 0) {
            return null;
        }
        return ReviewStatsResponse.builder()
                .totalReviews(p.getTotalReviews())
                .averageRating(
                        p.getAverageRating() != null
                                ? Math.round(p.getAverageRating() * 10.0) / 10.0
                                : 0.0)
                .oneStar(p.getOneStar())
                .twoStars(p.getTwoStars())
                .threeStars(p.getThreeStars())
                .fourStars(p.getFourStars())
                .fiveStars(p.getFiveStars())
                .unReadReview(p.getUnReadReview())
                .build();
    }

    public ProductReview getReview(Long id) {
        return reviewRepository.getProductReviewById(id)
                .orElseThrow(() -> new AppException(ErrorCode.INVALID_REQUEST));
    }

    @Transactional
    public void updateReviewStatus(UpdateReviewStatusRequest request) {
        ProductReview productReview = reviewRepository.getProductReviewById(request.getId())
                .orElseThrow(() -> new AppException(ErrorCode.INVALID_REQUEST));
        // Status in Request is now ReviewStatus Enum
        ReviewStatus status = ReviewStatus.valueOf(request.getStatus().name());
        if (productReview.getStatus() != null)
            productReview.setStatus(status);

        productReview.setHideReason(request.getHideReason());
        reviewRepository.save(productReview);
    }

    @Transactional
    public void setReadReview(List<Long> reviews, Boolean isRead) {
        reviewRepository.updateReadStatus(reviews, isRead);
    }

    @Transactional
    public void togglePin(Long id, Boolean isPinned) {
        reviewRepository.updatePinnedStatus(id, isPinned);
    }
}
