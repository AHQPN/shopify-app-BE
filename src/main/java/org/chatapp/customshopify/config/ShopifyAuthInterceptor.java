package org.chatapp.customshopify.config;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.chatapp.customshopify.entity.ShopifySession;
import org.chatapp.customshopify.repository.ShopifySessionRepository;
import org.chatapp.customshopify.service.ShopifyAuthService;
import org.chatapp.customshopify.service.ShopifyTokenValidator;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import java.util.Arrays;
import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j
public class ShopifyAuthInterceptor implements HandlerInterceptor {
    
    private final ShopifySessionRepository sessionRepository;
    private final ShopifyTokenValidator tokenValidator;
    private final ShopifyAuthService authService;
    
    // Endpoints không cần check token
    private static final List<String> EXCLUDED_PATHS = Arrays.asList(
        "/api/auth",
        "/api/auth/callback",
        "/api/health",
        "/api/test",
        "/api/files",
        "/h2-console"
    );
    
    @Override
    public boolean preHandle(HttpServletRequest request, 
                            HttpServletResponse response, 
                            Object handler) throws Exception {
        
        String path = request.getRequestURI();
        String method = request.getMethod();
        
        // Skip OPTIONS requests (CORS preflight)
        if ("OPTIONS".equalsIgnoreCase(method)) {
            return true;
        }
        
        // Skip excluded paths
        if (EXCLUDED_PATHS.stream().anyMatch(path::startsWith)) {
            return true;
        }
        
        log.debug("========== REQUEST VALIDATION ==========");
        log.debug("Path: {}", path);
        log.debug("Method: {}", method);
        
        // Try to get session token from Authorization header (App Bridge)
        String authHeader = request.getHeader("Authorization");
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            String sessionToken = authHeader.substring(7);
            log.debug("Found session token in Authorization header");
            
            // Validate session token (JWT)
            var claims = tokenValidator.validateSessionToken(sessionToken);
            if (claims != null) {
                String shop = claims.get("dest", String.class);
                if (shop != null) {
                    shop = shop.replace("https://", "").replace("/admin", "");
                }
                log.info("✅ Session token validated for shop: {}", shop);
                
                // Always set shop attribute if JWT is valid
                request.setAttribute("shop", shop);
                
                // Get or create access token for this shop
                var sessions = sessionRepository.findByShop(shop);
                if (!sessions.isEmpty()) {
                    ShopifySession session = sessions.get(0);
                    request.setAttribute("accessToken", session.getAccessToken());
                } else {
                    log.info("🔄 No access token in DB - initiating Token Exchange...");
                    try {
                        // Use Token Exchange to get Access Token from Session Token
                        ShopifySession newSession = authService.exchangeSessionTokenForAccessToken(shop, sessionToken);
                        request.setAttribute("accessToken", newSession.getAccessToken());
                        log.info("✅ Token Exchange successful - Access Token obtained!");
                    } catch (Exception e) {
                        log.error("❌ Token Exchange failed: {}", e.getMessage());
                        // Continue without access token - controller will handle
                    }
                }
                
                log.debug("✅ Request validated via session token");
                log.debug("========================================");
                return true;
            } else {
                log.error("❌ Invalid session token");
            }
        }

        // Fallback: Check shop parameter for backward compatibility
        String shop = request.getParameter("shop");
        
        if (shop == null || shop.isEmpty()) {
            log.warn(" No shop parameter in request: {}", path);
            response.setStatus(400);
            response.setContentType("application/json");
            response.getWriter().write("{\"error\": \"Missing shop parameter\"}");
            return false;
        }
        
        log.debug("Shop: {}", shop);
        
        // Check if session exists
        var sessions = sessionRepository.findByShop(shop);
        
        if (sessions.isEmpty()) {
            log.warn("❌ No token found for shop: {}. Need OAuth...", shop);
            
            // Return JSON response with auth URL (redirect won't work for AJAX)
            String authUrl = String.format(
                "%s://%s/api/auth?shop=%s",
                request.getScheme(),
                request.getHeader("Host"),
                shop
            );
            
            log.info("→ Client should redirect to: {}", authUrl);
            
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.setContentType("application/json");
            response.getWriter().write(String.format(
                "{\"error\": \"No access token found\", \"authRequired\": true, \"authUrl\": \"%s\", \"shop\": \"%s\"}",
                authUrl, shop
            ));
            return false;
        }
        
        ShopifySession session = sessions.get(0);
        log.debug("✅ Session found in DB");
        
        // Validate access token với Shopify API
        log.debug("Validating access token...");
        boolean isValid = tokenValidator.validateAccessToken(shop, session.getAccessToken());
        
        if (!isValid) {
            log.error("❌ Access token is invalid or expired for shop: {}", shop);
            log.info("🔄 Deleting old session and requiring re-auth...");
            
            // Delete old invalid session
            sessionRepository.delete(session);
            log.info("✅ Old session deleted");
            
            // Return JSON response for re-auth
            String authUrl = String.format(
                "%s://%s/api/auth?shop=%s",
                request.getScheme(),
                request.getHeader("Host"),
                shop
            );
            
            log.info("→ Client should redirect to: {}", authUrl);
            
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.setContentType("application/json");
            response.getWriter().write(String.format(
                "{\"error\": \"Access token expired\", \"authRequired\": true, \"authUrl\": \"%s\", \"shop\": \"%s\"}",
                authUrl, shop
            ));
            return false;
        }
        
        log.debug("✅ Access token is valid");
        log.debug("✅ Request validated for shop: {}", shop);
        log.debug("========================================");
        
        // Add token to request attributes for controller use
        request.setAttribute("accessToken", session.getAccessToken());
        request.setAttribute("shop", shop);
        
        return true;
    }
}
