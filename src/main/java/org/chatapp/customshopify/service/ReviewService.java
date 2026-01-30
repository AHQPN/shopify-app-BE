package org.chatapp.customshopify.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.chatapp.customshopify.client.CaptchaClient;
import org.chatapp.customshopify.dto.request.CreateReviewRequest;
import org.chatapp.customshopify.dto.request.UpdateReviewStatusRequest;
import org.chatapp.customshopify.dto.response.ReviewStatsResponse;
import org.chatapp.customshopify.entity.ProductReview;
import org.chatapp.customshopify.entity.ReviewMedia;
import org.chatapp.customshopify.enums.ReviewStatus;
import org.chatapp.customshopify.exception.AppException;
import org.chatapp.customshopify.exception.ErrorCode;
import org.chatapp.customshopify.repository.ProductReviewRepository;
import org.chatapp.customshopify.repository.ReviewMediaRepository;
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
    private final ReviewMediaRepository reviewMediaRepository;
    private final ProductReviewRepository reviewRepository;
    private final CaptchaClient captchaClient;

    @Transactional
    public ProductReview createReview(String shop, CreateReviewRequest request) {
        log.info("Creating/Updating review for product {} in shop {}", request.getProductId(), shop);

        // Verify reCAPTCHA
        if (!captchaClient.verify(request.getCaptchaToken())) {
            throw new AppException(ErrorCode.INVALID_REQUEST);
        }

        if (request.getReplyTo() != null) {

            // Handle Direct Reply: Update parent review's reply field
            ProductReview parent = reviewRepository.getProductReviewById(request.getReplyTo())
                    .orElseThrow(() -> new AppException(ErrorCode.INVALID_REQUEST));

            parent.setReply(request.getComment());

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
                .status(ReviewStatus.HIDDEN)
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
                productName,
                isAdmin);

        Page<ProductReview> reviewPage = reviewRepository.findAllSorted(spec, page, size, isAdmin);
        if (Boolean.TRUE.equals(status))
            reviewPage.forEach(review -> {
                if (ReviewStatus.PUBLISHED.equals(review.getStatus())
                        && Boolean.TRUE.equals(review.getIsAnonymous())) {

                    review.setCustomerName(transformToAnonymous(review.getCustomerName()));

                }

                // Filter hidden media for storefront
                if (review.getMedia() != null) {
                    review.setMedia(review.getMedia().stream()
                            .filter(m -> !Boolean.TRUE.equals(m.getIsHidden()))
                            .collect(java.util.stream.Collectors.toList()));
                }
            });
        return reviewPage;

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
                .unModeratedCount(p.getUnModeratedCount())
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
    public void togglePin(Long id, Boolean isPinned) {
        reviewRepository.updatePinnedStatus(id, isPinned);
    }

    private String transformToAnonymous(String customerName) {
        String[] parts = customerName.trim().split("\\s+");
        String name = parts[parts.length - 1];

        if (name.length() < 2) {
            return "Anonymous";
        }
        int maskName = name.length() / 2;
        int index = (name.length() - maskName) / 2;

        StringBuilder sb = new StringBuilder(name);
        for (int i = index; i <= index + maskName; i++)
            sb.setCharAt(i, '*');

        return sb.toString();

    }
    @Transactional
    public void setMediaStatus(long id,boolean status) {

        reviewMediaRepository.updateStatus(id,status);
    }
}
