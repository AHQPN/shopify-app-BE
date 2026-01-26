package org.chatapp.customshopify.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.chatapp.customshopify.dto.request.UpdateSettingsRequest;
import org.chatapp.customshopify.dto.response.BatchCalculationResult;
import org.chatapp.customshopify.dto.response.SettingsResponse;
import org.chatapp.customshopify.dto.response.UpdateSettingsResponse;
import org.chatapp.customshopify.entity.AppSettings;
import org.chatapp.customshopify.exception.AppException;
import org.chatapp.customshopify.exception.ErrorCode;
import org.chatapp.customshopify.repository.AppSettingsRepository;
import org.springframework.stereotype.Service;

import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class SettingsService {

    private final AppSettingsRepository settingsRepository;
    private final ProductService productService;

    public SettingsResponse getSettings(String shop) {
        log.info("Getting settings for shop: {}", shop);
        
        AppSettings settings = settingsRepository.findByShop(shop)
                .orElse(new AppSettings(shop));
        
        return new SettingsResponse(shop, settings.getDiscountFeatureEnabled());
    }

    public UpdateSettingsResponse updateSettings(String shop, String accessToken, UpdateSettingsRequest request) {
        log.info("Updating settings for shop: {}", shop);
        
        Boolean enabled = request.getDiscountFeatureEnabled();
        
        AppSettings settings = settingsRepository.findByShop(shop)
                .orElse(new AppSettings(shop));
        
        settings.setDiscountFeatureEnabled(enabled);
        settingsRepository.save(settings);
        
        // If enabled, calculate discounts for all products
        // If disabled, clear all discounts (set to 0)
        BatchCalculationResult calculationResult = null;
        String warning = null;
        
        try {
            if (Boolean.TRUE.equals(enabled)) {
                // Determine accessToken if not provided? Controller provides it.
                if (accessToken == null || accessToken.isEmpty()) {
                     throw new AppException(ErrorCode.UNAUTHENTICATED);
                }
                calculationResult = productService.calculateAllDiscounts(shop, accessToken);
            } else {
                // Change: Clear discounts if feature is disabled
                if (accessToken != null && !accessToken.isEmpty()) {
                    calculationResult = productService.clearAllDiscounts(shop, accessToken);
                }
            }
        } catch (Exception e) {
            log.error("Error processing discounts", e);
            warning = "Settings saved but error processing products: " + e.getMessage();
        }
        
        return UpdateSettingsResponse.builder()
                .success(true)
                .discountFeatureEnabled(enabled)
                .calculationResult(calculationResult != null ? calculationResult : BatchCalculationResult.builder().build())
                .warning(warning)
                .build();
    }
}
