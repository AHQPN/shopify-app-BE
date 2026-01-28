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
import org.springframework.data.domain.Pageable;
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
        log.info("Creating review for product {} in shop {}", request.getProductId(), shop);

        if (request.getReplyTo() == null) {
            if (request.getRating() == null || request.getRating() < 1 || request.getRating() > 5) {
                throw new AppException(ErrorCode.INVALID_REQUEST);
            }
        } else {
            // Handle Reply Logic: Update Parent Counters
            ProductReview parent = reviewRepository.getProductReviewById(request.getReplyTo())
                    .orElseThrow(() -> new AppException(ErrorCode.INVALID_REQUEST));

            int currentReplyNum = parent.getReplyNum();
            parent.setReplyNum(currentReplyNum + 1);

            int currentUnread = parent.getUnreadReplyCount();
            parent.setUnreadReplyCount(currentUnread + 1);

            reviewRepository.save(parent);
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
            String productName, Pageable pageable) {
        // NOTE: 'status' param here is from Controller (true=PUBLISHED,
        // false=HIDDEN/ARCHIVED?).
        // But Controller logic is:
        // Admin (Auth Header): pass status=null (get all) or true/false filter.
        // Storefront (No Auth): pass status=true (only published).

        List<ReviewStatus> statusList = new ArrayList<>();
        Boolean onlyParents = null;

        // If status is TRUE -> PUBLISHED
        // If status is FALSE -> HIDDEN? (Usually explicit filter)
        // If status is NULL -> "Admin Query" behavior defined by user requirements:
        // "Admin: parent reviews, no reply_to, status != ARCHIVED"

        // However, the Controller calls this.
        // Let's adapt based on the 'status' boolean flag semantics which usually meant
        // "Published?"

        if (Boolean.TRUE.equals(status)) {
            // Storefront behavior: Only PUBLISHED
            statusList.add(ReviewStatus.PUBLISHED);
            // User Requirement: "storefront will load review with status publish"
            // usually storefront lists parents
            // but let's leave onlyParents as NULL if not strictly required,
            // OR if user said "load review" (implies main list), usually parents.
            // But let's checking the requirement: "query get review on admin, only get
            // parent reviews... status different from archived"
            // "storefront will load review with status publish" (didn't prioritize replyTo,
            // but implied main list).
            // Let's assume Storefront also wants parents for the main list, but let's keep
            // it safe.
            // Actually, if we filter by PUBLISHED, replies might be published too.
        } else {
            // Admin behavior (or explicit status=false filter?)
            // User Requirement: "Admin: only get parent reviews, no reply_to, status !=
            // ARCHIVED"
            // Logic: If status is NULL (getting all for admin table), apply the
            // exclusionary logic.
            if (status == null) {
                statusList.add(ReviewStatus.PUBLISHED);
                statusList.add(ReviewStatus.HIDDEN); // != ARCHIVED
                onlyParents = true; // "only get parent reviews"
            } else {
                // Explicit status=false filter in Admin UI?
                // If Admin selects "Hidden", they want HIDDEN.
                statusList.add(org.chatapp.customshopify.enums.ReviewStatus.HIDDEN);
                // Should we enforce parents only here too? Probably yes for the main table.
                onlyParents = true;
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
                onlyParents,
                isRead,
                productName);

        return reviewRepository.findAll(spec, pageable);

    }

    public ReviewStatsResponse getReviewStats(String shop, String productId, Boolean status) {
        // Stats should follow similar logic?
        // Usually stats are for "All non-archived" or "Published" depending on context.

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

    @Transactional
    public void updateReviewStatus(UpdateReviewStatusRequest request) {
        ProductReview productReview = reviewRepository.getProductReviewById(request.getId())
                .orElseThrow(() -> new AppException(ErrorCode.INVALID_REQUEST));
        // Status in Request is now ReviewStatus Enum
        ReviewStatus status = ReviewStatus.valueOf(request.getStatus().name());
        productReview.setStatus(status);
        if (productReview.getReplyNum() > 0)
            reviewRepository.updateReply(productReview.getId(), status);
        productReview.setHideReason(request.getHideReason());
        reviewRepository.save(productReview);
    }

    public List<ProductReview> getRepliesByReview(Long id, Boolean isRead) {
        if (isRead == null) {
            return reviewRepository.getProductReviewByReplyTo(id);
        }
        if (isRead) {
            return reviewRepository.getProductReviewByReplyToAndIsReadTrue(id);
        }
        return reviewRepository.getProductReviewByReplyToAndIsReadFalse(id);
    }

    @Transactional
    public void setReadReview(List<Long> reviews) {
        reviewRepository.updateReadStatus(reviews);

    }

    @Transactional
    public void updateUnreadReplyCount(List<Long> reviews) {
        reviewRepository.updateReadStatus(reviews);
        Long productReviewId = reviewRepository.getProductReviewById(reviews.get(0)).orElseThrow().getReplyTo();
        ProductReview review = reviewRepository.getProductReviewById(productReviewId).orElseThrow();
        review.setUnreadReplyCount(review.getUnreadReplyCount() - reviews.size());
        reviewRepository.save(review);
    }
}
