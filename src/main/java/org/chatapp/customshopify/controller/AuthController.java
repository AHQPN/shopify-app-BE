package org.chatapp.customshopify.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.chatapp.customshopify.dto.response.ApiResponse;
import org.chatapp.customshopify.dto.response.AuthInitResponse;
import org.chatapp.customshopify.dto.response.SessionResponse;
import org.chatapp.customshopify.entity.ShopifySession;
import org.chatapp.customshopify.exception.AppException;
import org.chatapp.customshopify.exception.ErrorCode;
import org.chatapp.customshopify.service.ShopifyAuthService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
@Slf4j
public class AuthController {
    
    private final ShopifyAuthService authService;
    
    /**
     * Initiate OAuth flow
     * GET /api/auth?shop=myshop.myshopify.com
     */
    @GetMapping
    public ResponseEntity<ApiResponse<AuthInitResponse>> initiateAuth(@RequestParam String shop) {
        log.info("============ OAUTH STEP 1: INITIATE AUTH ============");
        return ResponseEntity.ok(ApiResponse.<AuthInitResponse>builder()
                .data(authService.initiateAuth(shop))
                .build());
    }
    
    /**
     * OAuth callback
     * GET /api/auth/callback
     */
    @GetMapping("/callback")
    public ResponseEntity<?> authCallback(
            @RequestParam String code,
            @RequestParam String hmac,
            @RequestParam String shop,
            @RequestParam String state,
            @RequestParam Map<String, String> allParams
    ) {
        log.info("============ OAUTH STEP 2: CALLBACK ============");
        log.info("Callback received for shop: {}", shop);
        
        // Validate HMAC
        log.info("Validating HMAC...");
        if (!authService.validateHmac(allParams, hmac)) {
            log.error("❌ HMAC validation FAILED for shop: {}", shop);
            throw new AppException(ErrorCode.UNAUTHENTICATED);
        }
        log.info("✅ HMAC validation PASSED");
        
        // Exchange code for token
        try {
            log.info("Exchanging code for access token...");
            ShopifySession session = authService.exchangeCodeForToken(shop, code);
            
            log.info("✅ ACCESS TOKEN OBTAINED AND SAVED!");
            
            // Redirect to frontend with success
            String frontendUrl = "http://localhost:3000/?shop=" + shop + "&session=" + session.getId();
            log.info("Redirecting to: {}", frontendUrl);
            log.info("============ STEP 2 COMPLETED ============");
            
            return ResponseEntity.status(302)
                    .header("Location", frontendUrl)
                    .build();
                    
        } catch (Exception e) {
            log.error("❌ Error during auth callback", e);
            throw new AppException(ErrorCode.TOKEN_EXCHANGE_FAILED);
        }
    }
    
    /**
     * Get session info
     * GET /api/auth/session?shop=xxx
     */
    @GetMapping("/session")
    public ResponseEntity<ApiResponse<SessionResponse>> getSession(@RequestParam String shop) {
        return ResponseEntity.ok(ApiResponse.<SessionResponse>builder()
                .data(authService.getSessionResponse(shop))
                .build());
    }
}
