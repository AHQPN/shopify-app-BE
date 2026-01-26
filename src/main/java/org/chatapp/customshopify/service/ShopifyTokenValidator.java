package org.chatapp.customshopify.service;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.chatapp.customshopify.config.ShopifyConfig;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class ShopifyTokenValidator {
    
    private final ShopifyConfig shopifyConfig;
    private final RestTemplate restTemplate = new RestTemplate();
    
    /**
     * Validate session token (JWT) from App Bridge
     */
    public Claims validateSessionToken(String token) {
        try {
            // Decode JWT without verification first to get shop
            String[] parts = token.split("\\.");
            if (parts.length != 3) {
                log.error("Invalid JWT format");
                return null;
            }
            
            String payload = new String(Base64.getUrlDecoder().decode(parts[1]));
            log.debug("JWT payload: {}", payload);
            
            // Parse with verification using API secret (JJWT 0.12+ API)
            javax.crypto.SecretKey key = io.jsonwebtoken.security.Keys.hmacShaKeyFor(
                shopifyConfig.getApiSecret().getBytes(java.nio.charset.StandardCharsets.UTF_8)
            );
            
            Claims claims = Jwts.parser()
                    .clockSkewSeconds(60) // Allow 60 seconds skew for local dev time difference
                    .verifyWith(key)
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();
            
            log.info("✅ Session token validated for shop: {}", claims.get("dest"));
            return claims;
            
        } catch (Exception e) {
            log.error("❌ Session token validation failed: {}", e.getMessage());
            return null;
        }
    }
    
    /**
     * Validate access token by calling Shopify API
     */
    public boolean validateAccessToken(String shop, String accessToken) {
        try {
            log.info("Validating access token for shop: {}", shop);
            
            // Call a simple Shopify API endpoint to verify token
            String url = String.format("https://%s/admin/api/%s/shop.json", 
                    shop, shopifyConfig.getApiVersion());
            
            HttpHeaders headers = new HttpHeaders();
            headers.set("X-Shopify-Access-Token", accessToken);
            HttpEntity<Void> entity = new HttpEntity<>(headers);
            
            ResponseEntity<Map> response = restTemplate.exchange(
                    url, HttpMethod.GET, entity, Map.class);
            
            boolean valid = response.getStatusCode() == HttpStatus.OK;
            
            if (valid) {
                log.info("✅ Access token is valid for shop: {}", shop);
            } else {
                log.warn("❌ Access token validation failed: {}", response.getStatusCode());
            }
            
            return valid;
            
        } catch (Exception e) {
            log.error("❌ Access token validation error for {}: {}", shop, e.getMessage());
            return false;
        }
    }
    
    /**
     * Validate HMAC from Shopify request
     */
    public boolean validateHmac(Map<String, String> params, String hmac) {
        try {
            // Remove hmac from params
            params.remove("hmac");
            params.remove("signature");
            
            // Build query string (sorted)
            StringBuilder query = new StringBuilder();
            params.entrySet().stream()
                    .sorted(Map.Entry.comparingByKey())
                    .forEach(entry -> {
                        if (query.length() > 0) query.append("&");
                        query.append(entry.getKey()).append("=").append(entry.getValue());
                    });
            
            // Calculate HMAC
            Mac mac = Mac.getInstance("HmacSHA256");
            SecretKeySpec secretKey = new SecretKeySpec(
                    shopifyConfig.getApiSecret().getBytes(StandardCharsets.UTF_8),
                    "HmacSHA256");
            mac.init(secretKey);
            
            byte[] hmacBytes = mac.doFinal(query.toString().getBytes(StandardCharsets.UTF_8));
            String calculatedHmac = bytesToHex(hmacBytes);
            
            return calculatedHmac.equals(hmac);
            
        } catch (Exception e) {
            log.error("HMAC validation error: {}", e.getMessage());
            return false;
        }
    }
    
    private String bytesToHex(byte[] bytes) {
        StringBuilder result = new StringBuilder();
        for (byte b : bytes) {
            result.append(String.format("%02x", b));
        }
        return result.toString();
    }
}
