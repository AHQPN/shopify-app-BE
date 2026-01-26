package org.chatapp.customshopify.controller;

import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.chatapp.customshopify.dto.request.UpdateSettingsRequest;
import org.chatapp.customshopify.dto.response.ApiResponse;
import org.chatapp.customshopify.dto.response.SettingsResponse;
import org.chatapp.customshopify.dto.response.UpdateSettingsResponse;
import org.chatapp.customshopify.exception.AppException;
import org.chatapp.customshopify.exception.ErrorCode;
import org.chatapp.customshopify.service.SettingsService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/settings")
@RequiredArgsConstructor
@Slf4j
public class SettingsController {
    
    private final SettingsService settingsService;
    
    /**
     * Get settings for a shop
     * GET /api/settings
     */
    @GetMapping
    public ResponseEntity<ApiResponse<SettingsResponse>> getSettings(HttpServletRequest request) {
        String shop = (String) request.getAttribute("shop");
        
        if (shop == null || shop.isEmpty()) {
            throw new AppException(ErrorCode.UNAUTHENTICATED);
        }
        
        return ResponseEntity.ok(ApiResponse.<SettingsResponse>builder()
                .data(settingsService.getSettings(shop))
                .build());
    }
    
    /**
     * Update settings for a shop
     * POST /api/settings
     */
    @PostMapping
    public ResponseEntity<ApiResponse<UpdateSettingsResponse>> updateSettings(
            HttpServletRequest request,
            @RequestBody UpdateSettingsRequest body
    ) {
        String shop = (String) request.getAttribute("shop");
        String accessToken = (String) request.getAttribute("accessToken");
        
        if (shop == null || shop.isEmpty()) {
            throw new AppException(ErrorCode.UNAUTHENTICATED);
        }
        
        return ResponseEntity.ok(ApiResponse.<UpdateSettingsResponse>builder()
                .data(settingsService.updateSettings(shop, accessToken, body))
                .build());
    }
}

