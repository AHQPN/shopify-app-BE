package org.chatapp.customshopify.client;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.chatapp.customshopify.dto.request.CodeExchangeRequest;
import org.chatapp.customshopify.dto.request.TokenExchangeRequest;
import org.chatapp.customshopify.dto.response.TokenExchangeResponse;
import org.chatapp.customshopify.exception.AppException;
import org.chatapp.customshopify.exception.ErrorCode;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;

/**
 * Client for Shopify OAuth and REST API calls.
 * Handles token exchange and authentication-related operations.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class ShopifyRestClient {

    private final WebClient shopifyWebClient;
    private final ShopifyClientConfig clientConfig;

    /**
     * Exchange an authorization code for an access token.
     *
     * @param shop    The Shopify store domain
     * @param request The code exchange request containing client credentials and
     *                code
     * @return TokenExchangeResponse with access token and scope
     */
    public TokenExchangeResponse exchangeCode(String shop, CodeExchangeRequest request) {
        String url = clientConfig.buildOAuthTokenUrl(shop);

        log.info("Exchanging authorization code for shop: {}", shop);

        try {
            TokenExchangeResponse response = shopifyWebClient.post()
                    .uri(url)
                    .contentType(MediaType.APPLICATION_JSON)
                    .bodyValue(request)
                    .retrieve()
                    .bodyToMono(TokenExchangeResponse.class)
                    .block();

            if (response == null || response.getAccessToken() == null) {
                log.error("Empty response from token exchange for shop: {}", shop);
                throw new AppException(ErrorCode.SHOPIFY_API_ERROR);
            }

            log.info("✅ Code exchange successful for shop: {}", shop);
            return response;

        } catch (WebClientResponseException e) {
            log.error("Token exchange failed for shop {}: {} - {}", shop, e.getStatusCode(),
                    e.getResponseBodyAsString());
            throw new AppException(ErrorCode.SHOPIFY_API_ERROR);
        } catch (AppException e) {
            throw e;
        } catch (Exception e) {
            log.error("Error during code exchange for shop: {}", shop, e);
            throw new AppException(ErrorCode.SHOPIFY_API_ERROR);
        }
    }

    /**
     * Exchange a session token (JWT) for an offline access token.
     * This is the modern App Bridge v4 approach.
     *
     * @param shop    The Shopify store domain
     * @param request The token exchange request
     * @return TokenExchangeResponse with access token and scope
     */
    public TokenExchangeResponse exchangeSessionToken(String shop, TokenExchangeRequest request) {
        String url = clientConfig.buildOAuthTokenUrl(shop);

        log.info("Exchanging session token for shop: {}", shop);

        try {
            TokenExchangeResponse response = shopifyWebClient.post()
                    .uri(url)
                    .contentType(MediaType.APPLICATION_JSON)
                    .bodyValue(request)
                    .retrieve()
                    .bodyToMono(TokenExchangeResponse.class)
                    .block();

            if (response == null || response.getAccessToken() == null) {
                log.error("Empty response from session token exchange for shop: {}", shop);
                throw new AppException(ErrorCode.TOKEN_EXCHANGE_FAILED);
            }

            log.info("✅ Session token exchange successful for shop: {}", shop);
            return response;

        } catch (WebClientResponseException e) {
            log.error("Session token exchange failed for shop {}: {} - {}", shop, e.getStatusCode(),
                    e.getResponseBodyAsString());
            throw new AppException(ErrorCode.TOKEN_EXCHANGE_FAILED);
        } catch (AppException e) {
            throw e;
        } catch (Exception e) {
            log.error("Error during session token exchange for shop: {}", shop, e);
            throw new AppException(ErrorCode.TOKEN_EXCHANGE_FAILED);
        }
    }
}
