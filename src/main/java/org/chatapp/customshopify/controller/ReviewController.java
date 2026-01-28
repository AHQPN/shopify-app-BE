package org.chatapp.customshopify.controller;

import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.chatapp.customshopify.dto.request.CreateReviewRequest;
import org.chatapp.customshopify.dto.request.UpdateReviewStatusRequest;
import org.chatapp.customshopify.dto.response.ApiResponse;
import org.chatapp.customshopify.dto.response.ReviewStatsResponse;
import org.chatapp.customshopify.entity.ProductReview;
import org.chatapp.customshopify.exception.AppException;
import org.chatapp.customshopify.exception.ErrorCode;
import org.chatapp.customshopify.service.ReviewService;
import org.chatapp.customshopify.enums.HideReason;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/reviews")
@RequiredArgsConstructor
@Slf4j
public class ReviewController {

    private final ReviewService reviewService;

    @PostMapping
    public ResponseEntity<ApiResponse<ProductReview>> createReview(
            HttpServletRequest httpServletRequest,
            @RequestBody CreateReviewRequest request) {
        String shop = getShop(httpServletRequest);
        return ResponseEntity.ok(ApiResponse.<ProductReview>builder()
                .data(reviewService.createReview(shop, request))
                .build());
    }

    @GetMapping
    public ResponseEntity<ApiResponse<Page<ProductReview>>> getReviews(
            HttpServletRequest httpServletRequest,
            @RequestParam(required = false) String productId,
            @RequestParam(required = false) Integer rating,
            @RequestParam(required = false) Boolean status,
            @RequestParam(required = false) Boolean isRead,
            @RequestParam(required = false) String productName,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {

        String shop = getShop(httpServletRequest);

        // Determine if we should enforce Published status
        // Admin App uses Bearer token. Storefront uses shop param only.
        String authHeader = httpServletRequest.getHeader("Authorization");
        Boolean finalStatus = status;
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            finalStatus = true; // Storefront only sees published
        }

        Sort sort = Sort.by("createdAt").descending();
        Page<ProductReview> reviews = reviewService.getReviews(shop, productId, rating, finalStatus, isRead,
                productName,
                PageRequest.of(page, size, sort));

        return ResponseEntity.ok(ApiResponse.<Page<ProductReview>>builder()
                .data(reviews)
                .build());
    }

    @GetMapping("/stats")
    public ResponseEntity<ApiResponse<ReviewStatsResponse>> getStats(
            HttpServletRequest httpServletRequest,
            @RequestParam(required = false) String productId,
            @RequestParam(required = false) Boolean status) {
        String shop = getShop(httpServletRequest);

        String authHeader = httpServletRequest.getHeader("Authorization");
        Boolean finalStatus = status;
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            finalStatus = true; // Storefront stats only for published
        }

        return ResponseEntity.ok(ApiResponse.<ReviewStatsResponse>builder()
                .data(reviewService.getReviewStats(shop, productId, finalStatus))
                .build());
    }

    @GetMapping("/hide-reasons")
    public ResponseEntity<ApiResponse<List<Map<String, String>>>> getHideReasons() {
        List<Map<String, String>> reasons = Arrays.stream(HideReason.values())
                .map(reason -> Map.of(
                        "value", reason.name(),
                        "label", reason.getDisplayValue()))
                .collect(Collectors.toList());

        return ResponseEntity.ok(ApiResponse.<List<Map<String, String>>>builder()
                .data(reasons)
                .build());
    }

    @PutMapping
    public ResponseEntity<ApiResponse> setReviewStatus(@RequestBody UpdateReviewStatusRequest request) {
        reviewService.updateReviewStatus(request);
        return ResponseEntity.ok().body(ApiResponse.<Void>builder().message("Update successfully").build());
    }
    @PutMapping("/read-reply")
    public ResponseEntity<ApiResponse> updateUnreadReplyCount(@RequestBody List<Long> reviews) {
        reviewService.updateUnreadReplyCount(reviews);
        return ResponseEntity.ok().body(ApiResponse.<Void>builder().message("Update successfully").build());
    }
    @GetMapping("/{id}/replies")
    public ResponseEntity<?> getRepliesByReview(@PathVariable Long id, @RequestParam(required = false) Boolean isRead) {
        List<ProductReview> reviews = reviewService.getRepliesByReview(id, isRead);
        return ResponseEntity.ok().body(ApiResponse.builder().data(reviews).build());
    }

    @PutMapping("/read")
    public ResponseEntity<?> setReadReview(@RequestBody List<Long> reviews) {
        reviewService.setReadReview(reviews);
        return ResponseEntity.ok().body(ApiResponse.builder().message("Set read successfully"));
    }



    private String getShop(HttpServletRequest request) {
        String shop = (String) request.getAttribute("shop");
        if (shop == null) {
            shop = request.getParameter("shop");
        }
        if (shop == null || shop.isEmpty()) {
            throw new AppException(ErrorCode.SHOP_NOT_FOUND);
        }
        return shop;
    }
}
