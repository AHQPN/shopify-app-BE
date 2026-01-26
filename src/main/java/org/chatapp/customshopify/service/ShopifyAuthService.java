package org.chatapp.customshopify.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.chatapp.customshopify.config.ShopifyConfig;
import org.chatapp.customshopify.dto.request.CodeExchangeRequest;
import org.chatapp.customshopify.dto.request.TokenExchangeRequest;
import org.chatapp.customshopify.dto.response.AuthInitResponse;
import org.chatapp.customshopify.dto.response.SessionResponse;
import org.chatapp.customshopify.dto.response.TokenExchangeResponse;
import org.chatapp.customshopify.entity.ShopifySession;
import org.chatapp.customshopify.exception.AppException;
import org.chatapp.customshopify.exception.ErrorCode;
import org.chatapp.customshopify.repository.ShopifySessionRepository;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.*;

@Service
@Slf4j
@RequiredArgsConstructor
public class ShopifyAuthService {
    
    private final ShopifyConfig shopifyConfig;
    private final ShopifySessionRepository sessionRepository;
    private final RestTemplate restTemplate = new RestTemplate();

    public AuthInitResponse initiateAuth(String shop) {
        // Validate shop domain
        if (!shop.endsWith(".myshopify.com")) {
            log.error("Invalid shop domain: {}", shop);
            throw new AppException(ErrorCode.INVALID_REQUEST);
        }

        String redirectUri = "http://localhost:8080/api/auth/callback";
        String authUrl = generateAuthUrl(shop, redirectUri);
        
        return AuthInitResponse.builder()
                .authUrl(authUrl)
                .shop(shop)
                .build();
    }

    public SessionResponse getSessionResponse(String shop) {
        return getSession(shop)
                .map(session -> SessionResponse.builder()
                        .shop(session.getShop())
                        .scope(session.getScope())
                        .isOnline(session.getIsOnline())
                        .build())
                .orElseThrow(() -> new AppException(ErrorCode.SHOP_NOT_FOUND));
    }

    
    /**
     * Generate OAuth authorization URL
     */
    public String generateAuthUrl(String shop, String redirectUri) {
        String state = generateState();
        String scopes = shopifyConfig.getScopes();
        String apiKey = shopifyConfig.getApiKey();
        
        // Use configured endpoint
        return String.format(shopifyConfig.getOauthAuthorizeUrl(), shop) +
               String.format("?client_id=%s&scope=%s&redirect_uri=%s&state=%s", apiKey, scopes, redirectUri, state);
    }
    
    /**
     * Exchange authorization code for access token
     */
    public ShopifySession exchangeCodeForToken(String shop, String code) {
        log.info("========== EXCHANGE CODE FOR TOKEN ==========");
        log.info("Shop: {}", shop);
        log.info("Code (first 10): {}...", code.substring(0, Math.min(10, code.length())));

        try {
            // Call Shopify API to exchange code for access token
            String url = String.format(shopifyConfig.getOauthTokenUrl(), shop);
            log.info("Calling Shopify API: {}", url);

            org.springframework.web.client.RestTemplate restTemplate = new org.springframework.web.client.RestTemplate();
            org.springframework.http.HttpHeaders headers = new org.springframework.http.HttpHeaders();
            headers.setContentType(org.springframework.http.MediaType.APPLICATION_JSON);

            CodeExchangeRequest requestBody = CodeExchangeRequest.builder()
                    .clientId(shopifyConfig.getApiKey())
                    .clientSecret(shopifyConfig.getApiSecret())
                    .code(code)
                    .build();

            log.info("Request body: client_id={}, code={}...",
                    shopifyConfig.getApiKey(), code.substring(0, Math.min(10, code.length())));

            org.springframework.http.HttpEntity<CodeExchangeRequest> request =
                    new org.springframework.http.HttpEntity<>(requestBody, headers);

            log.info("Sending POST request to Shopify...");
            log.info("Sending POST request to Shopify...");
            org.springframework.http.ResponseEntity<TokenExchangeResponse> response =
                    restTemplate.postForEntity(url, request, TokenExchangeResponse.class);

            log.info("Response status: {}", response.getStatusCode());

            TokenExchangeResponse responseBody = response.getBody();
            if (responseBody == null) {
                throw new AppException(ErrorCode.SHOPIFY_API_ERROR);
            }
            
            String accessToken = responseBody.getAccessToken();
            String scope = responseBody.getScope();

            log.info("✅ Successfully obtained access token!");
            log.info("Token (first 15): {}...", accessToken.substring(0, Math.min(15, accessToken.length())));
            log.info("Scope: {}", scope);

            // Check if session already exists
            log.info("Checking for existing session...");
            var existingSessions = sessionRepository.findByShop(shop);

            ShopifySession session;
            if (!existingSessions.isEmpty()) {
                // Update existing session
                session = existingSessions.get(0);
                log.info("Updating existing session ID: {}", session.getId());
                session.setAccessToken(accessToken);
                session.setScope(scope != null ? scope : shopifyConfig.getScopes());
                session.setState("active");
                session.setUpdatedAt(java.time.LocalDateTime.now());
            } else {
                // Create new session
                log.info("Creating new session...");
                session = new ShopifySession();
                session.setId(UUID.randomUUID().toString());
                session.setShop(shop);
                session.setState("active");
                session.setIsOnline(false);
                session.setAccessToken(accessToken);
                session.setScope(scope != null ? scope : shopifyConfig.getScopes());
                session.setCreatedAt(java.time.LocalDateTime.now());
                session.setUpdatedAt(java.time.LocalDateTime.now());
            }

            ShopifySession savedSession = sessionRepository.save(session);
            log.info("✅ Session saved to database!");
            log.info("Session ID: {}", savedSession.getId());
            log.info("========== EXCHANGE COMPLETED ==========");

            return savedSession;

        } catch (AppException e) {
            throw e;
        } catch (Exception e) {
            log.error("❌ Error exchanging code for token: {}", e.getMessage(), e);
            throw new AppException(ErrorCode.SHOPIFY_API_ERROR);
        }
    }
    
    /**
     * Exchange Session Token (JWT) for Offline Access Token using Token Exchange
     * This is the modern approach (App Bridge v4) - no redirect required!
     */
    public ShopifySession exchangeSessionTokenForAccessToken(String shop, String sessionToken) {
        log.info("========== TOKEN EXCHANGE (Session -> Access) ==========");
        log.info("Shop: {}", shop);
        
        try {
            String url = String.format(shopifyConfig.getOauthTokenUrl(), shop);
            
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            
            TokenExchangeRequest requestBody = TokenExchangeRequest.builder()
                    .clientId(shopifyConfig.getApiKey())
                    .clientSecret(shopifyConfig.getApiSecret())
                    .grantType("urn:ietf:params:oauth:grant-type:token-exchange")
                    .subjectToken(sessionToken)
                    .subjectTokenType("urn:ietf:params:oauth:token-type:id_token")
                    .requestedTokenType("urn:shopify:params:oauth:token-type:offline-access-token")
                    .build();
            
            HttpEntity<TokenExchangeRequest> request = new HttpEntity<>(requestBody, headers);
            
            log.info("Calling Token Exchange API...");
            ResponseEntity<TokenExchangeResponse> response = restTemplate.postForEntity(url, request, TokenExchangeResponse.class);
            
            log.info("Response status: {}", response.getStatusCode());
            
            TokenExchangeResponse responseBody = response.getBody();
            if (responseBody == null) {
                throw new AppException(ErrorCode.TOKEN_EXCHANGE_FAILED);
            }
            
            String accessToken = responseBody.getAccessToken();
            String scope = responseBody.getScope();
            
            log.info("✅ Token Exchange successful!");
            log.info("Access Token (first 15): {}...", accessToken.substring(0, Math.min(15, accessToken.length())));
            
            // Save to database
            var existingSessions = sessionRepository.findByShop(shop);
            ShopifySession session;
            
            if (!existingSessions.isEmpty()) {
                session = existingSessions.get(0);
                session.setAccessToken(accessToken);
                session.setScope(scope != null ? scope : shopifyConfig.getScopes());
                session.setUpdatedAt(java.time.LocalDateTime.now());
            } else {
                session = new ShopifySession();
                session.setId(UUID.randomUUID().toString());
                session.setShop(shop);
                session.setState("active");
                session.setIsOnline(false);
                session.setAccessToken(accessToken);
                session.setScope(scope != null ? scope : shopifyConfig.getScopes());
                session.setCreatedAt(java.time.LocalDateTime.now());
                session.setUpdatedAt(java.time.LocalDateTime.now());
            }
            
            ShopifySession savedSession = sessionRepository.save(session);
            log.info("✅ Session saved! ID: {}", savedSession.getId());
            log.info("========== TOKEN EXCHANGE COMPLETED ==========");
            
            return savedSession;
            
        } catch (AppException e) {
            throw e;
        } catch (Exception e) {
            log.error("❌ Token Exchange failed: {}", e.getMessage(), e);
            throw new AppException(ErrorCode.TOKEN_EXCHANGE_FAILED);
        }
    }
    
    /**
     * Validate HMAC signature from Shopify
     */
    public boolean validateHmac(Map<String, String> params, String hmac) {
        try {
            // Remove hmac and signature from params
            Map<String, String> filteredParams = new TreeMap<>(params);
            filteredParams.remove("hmac");
            filteredParams.remove("signature");
            
            // Build query string
            StringBuilder queryString = new StringBuilder();
            filteredParams.forEach((key, value) -> {
                if (queryString.length() > 0) {
                    queryString.append("&");
                }
                queryString.append(key).append("=").append(value);
            });
            
            // Calculate HMAC
            String calculatedHmac = calculateHmac(queryString.toString());
            return calculatedHmac.equals(hmac);
            
        } catch (Exception e) {
            log.error("Error validating HMAC", e);
            return false;
        }
    }
    
    /**
     * Verify webhook signature
     */
    public boolean verifyWebhookSignature(String data, String hmacHeader) {
        try {
            String calculatedHmac = calculateHmacBase64(data);
            return calculatedHmac.equals(hmacHeader);
        } catch (Exception e) {
            log.error("Error verifying webhook signature", e);
            return false;
        }
    }
    
    /**
     * Get session by shop
     */
    public Optional<ShopifySession> getSession(String shop) {
        return sessionRepository.findByShopAndIsOnline(shop, false);
    }
    
    /**
     * Delete sessions for a shop
     */
    @Transactional
    public void deleteSessionsByShop(String shop) {
        sessionRepository.deleteByShop(shop);
    }
    
    // Helper methods
    
    private String generateState() {
        return UUID.randomUUID().toString();
    }
    
    private String calculateHmac(String data) throws NoSuchAlgorithmException, InvalidKeyException {
        Mac mac = Mac.getInstance("HmacSHA256");
        SecretKeySpec secretKey = new SecretKeySpec(
            shopifyConfig.getApiSecret().getBytes(StandardCharsets.UTF_8),
            "HmacSHA256"
        );
        mac.init(secretKey);
        byte[] hmacBytes = mac.doFinal(data.getBytes(StandardCharsets.UTF_8));
        return bytesToHex(hmacBytes);
    }
    
    private String calculateHmacBase64(String data) throws NoSuchAlgorithmException, InvalidKeyException {
        Mac mac = Mac.getInstance("HmacSHA256");
        SecretKeySpec secretKey = new SecretKeySpec(
            shopifyConfig.getApiSecret().getBytes(StandardCharsets.UTF_8),
            "HmacSHA256"
        );
        mac.init(secretKey);
        byte[] hmacBytes = mac.doFinal(data.getBytes(StandardCharsets.UTF_8));
        return Base64.getEncoder().encodeToString(hmacBytes);
    }
    
    private String bytesToHex(byte[] bytes) {
        StringBuilder result = new StringBuilder();
        for (byte b : bytes) {
            result.append(String.format("%02x", b));
        }
        return result.toString();
    }
}
