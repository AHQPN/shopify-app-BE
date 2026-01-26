package org.chatapp.customshopify.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.chatapp.customshopify.entity.AppSettings;
import org.chatapp.customshopify.repository.AppSettingsRepository;
import org.chatapp.customshopify.service.ProductService;
import org.chatapp.customshopify.service.ShopifyAuthService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/webhooks")
@RequiredArgsConstructor
@Slf4j
public class WebhookController {
    
    private final ShopifyAuthService authService;
    private final ProductService productService;
    private final AppSettingsRepository settingsRepository;
    private final ObjectMapper objectMapper = new ObjectMapper();
    
    /**
     * Handle app uninstalled webhook
     * POST /webhooks/app/uninstalled
     */
    @PostMapping("/app/uninstalled")
    public ResponseEntity<?> handleAppUninstalled(
            @RequestBody String payload,
            @RequestHeader("X-Shopify-Hmac-SHA256") String hmac,
            @RequestHeader("X-Shopify-Shop-Domain") String shop
    ) {
        log.info("Received app/uninstalled webhook for shop: {}", shop);
        
        // Verify webhook signature
        if (!authService.verifyWebhookSignature(payload, hmac)) {
            log.error("Invalid webhook signature for shop: {}", shop);
            return ResponseEntity.badRequest().body(Map.of("error", "Invalid signature"));
        }
        
        // Delete all sessions for this shop
        try {
            authService.deleteSessionsByShop(shop);
            log.info("Deleted sessions for uninstalled shop: {}", shop);
            return ResponseEntity.ok().build();
        } catch (Exception e) {
            log.error("Error handling app uninstall webhook", e);
            return ResponseEntity.internalServerError().build();
        }
    }
    
    /**
     * Handle scopes update webhook
     * POST /webhooks/app/scopes_update
     */
    @PostMapping("/app/scopes_update")
    public ResponseEntity<?> handleScopesUpdate(
            @RequestBody String payload,
            @RequestHeader("X-Shopify-Hmac-SHA256") String hmac,
            @RequestHeader("X-Shopify-Shop-Domain") String shop
    ) {
        log.info("Received app/scopes_update webhook for shop: {}", shop);
        
        // Verify webhook signature
        if (!authService.verifyWebhookSignature(payload, hmac)) {
            log.error("Invalid webhook signature for shop: {}", shop);
            return ResponseEntity.badRequest().body(Map.of("error", "Invalid signature"));
        }
        
        // Handle scopes update
        log.info("Scopes updated for shop: {}", shop);
        
        return ResponseEntity.ok().build();
    }

    /**
     * Handle products/update webhook
     * POST /webhooks/products/update
     */
    @PostMapping("/products/update")
    public ResponseEntity<?> handleProductUpdate(
            @RequestBody String payload,
            @RequestHeader("X-Shopify-Hmac-SHA256") String hmac,
            @RequestHeader("X-Shopify-Shop-Domain") String shop
    ) {
        // Verify webhook signature
        if (!authService.verifyWebhookSignature(payload, hmac)) {
            log.error("Invalid webhook signature for shop: {}", shop);
            return ResponseEntity.badRequest().body(Map.of("error", "Invalid signature"));
        }

        try {
            // Check if feature is enabled for this shop
            AppSettings settings = settingsRepository.findByShop(shop)
                    .orElse(new AppSettings(shop));
            
            if (!Boolean.TRUE.equals(settings.getDiscountFeatureEnabled())) {
                return ResponseEntity.ok().build(); // Feature disabled, ignore
            }

            JsonNode root = objectMapper.readTree(payload);
            String productId = "gid://shopify/Product/" + root.get("id").asText();
            
            // Get first variant prices
            JsonNode variants = root.get("variants");
            if (variants != null && variants.isArray() && variants.size() > 0) {
                JsonNode firstVariant = variants.get(0);
                String price = firstVariant.get("price").asText();
                String compareAtPrice = firstVariant.hasNonNull("compare_at_price") 
                        ? firstVariant.get("compare_at_price").asText() 
                        : null;

                if (compareAtPrice != null) {
                    productService.handleProductUpdate(shop, productId, price, compareAtPrice);
                }
            }
            
            return ResponseEntity.ok().build();
        } catch (Exception e) {
            log.error("Error handling product update webhook", e);
            return ResponseEntity.internalServerError().build();
        }
    }
}
