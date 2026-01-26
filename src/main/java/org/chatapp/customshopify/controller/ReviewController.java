package org.chatapp.customshopify.controller;

import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.chatapp.customshopify.dto.request.CreateReviewRequest;
import org.chatapp.customshopify.dto.response.ApiResponse;
import org.chatapp.customshopify.entity.ProductReview;
import org.chatapp.customshopify.exception.AppException;
import org.chatapp.customshopify.exception.ErrorCode;
import org.chatapp.customshopify.service.ReviewService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/reviews")
@RequiredArgsConstructor
@Slf4j
public class ReviewController {

    private final ReviewService reviewService;

    @PostMapping
    public ResponseEntity<ApiResponse<ProductReview>> createReview(
            HttpServletRequest httpServletRequest,
            @RequestBody CreateReviewRequest request
    ) {
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
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size
    ) {
        String shop = getShop(httpServletRequest);
        Sort sort = Sort.by("createdAt").descending();
        
        Page<ProductReview> reviews = reviewService.getReviews(shop, productId, rating, PageRequest.of(page, size, sort));

        return ResponseEntity.ok(ApiResponse.<Page<ProductReview>>builder()
                .data(reviews)
                .build());
    }

    @GetMapping("/stats")
    public ResponseEntity<ApiResponse<ReviewService.ReviewStats>> getStats(HttpServletRequest httpServletRequest) {
        String shop = getShop(httpServletRequest);
        return ResponseEntity.ok(ApiResponse.<ReviewService.ReviewStats>builder()
                .data(reviewService.getReviewStats(shop))
                .build());
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
