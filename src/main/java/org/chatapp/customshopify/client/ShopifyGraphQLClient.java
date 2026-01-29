package org.chatapp.customshopify.client;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.chatapp.customshopify.dto.request.GraphQLRequest;
import org.chatapp.customshopify.exception.AppException;
import org.chatapp.customshopify.exception.ErrorCode;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.core.publisher.Mono;

/**
 * Client for Shopify GraphQL Admin API calls.
 * Provides a centralized, type-safe interface for executing GraphQL queries and
 * mutations.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class ShopifyGraphQLClient {

    private final WebClient shopifyWebClient;
    private final ShopifyClientConfig clientConfig;

    /**
     * Execute a GraphQL query/mutation and return the response mapped to the
     * specified type.
     *
     * @param shop         The Shopify store domain (e.g., "example.myshopify.com")
     * @param accessToken  The access token for authentication
     * @param query        The GraphQL query or mutation string
     * @param responseType The class to map the response to
     * @return The mapped response object
     */
    public <T> T execute(String shop, String accessToken, String query, Class<T> responseType) {
        String url = clientConfig.buildGraphQLUrl(shop);

        log.debug("Executing GraphQL request to shop: {}", shop);

        try {
            T response = shopifyWebClient.post()
                    .uri(url)
                    .header("X-Shopify-Access-Token", accessToken)
                    .contentType(MediaType.APPLICATION_JSON)
                    .bodyValue(new GraphQLRequest(query))
                    .retrieve()
                    .bodyToMono(responseType)
                    .block();

            log.debug("GraphQL request successful for shop: {}", shop);
            return response;

        } catch (WebClientResponseException e) {
            log.error("Shopify API error for shop {}: {} - {}", shop, e.getStatusCode(), e.getResponseBodyAsString());
            throw new AppException(ErrorCode.SHOPIFY_API_ERROR);
        } catch (Exception e) {
            log.error("Error executing GraphQL request for shop: {}", shop, e);
            throw new AppException(ErrorCode.SHOPIFY_API_ERROR);
        }
    }

    /**
     * Execute a GraphQL query/mutation asynchronously, returning a Mono.
     * Use this for non-blocking operations.
     *
     * @param shop         The Shopify store domain
     * @param accessToken  The access token for authentication
     * @param query        The GraphQL query or mutation string
     * @param responseType The class to map the response to
     * @return A Mono containing the mapped response
     */
    public <T> Mono<T> executeAsync(String shop, String accessToken, String query, Class<T> responseType) {
        String url = clientConfig.buildGraphQLUrl(shop);

        return shopifyWebClient.post()
                .uri(url)
                .header("X-Shopify-Access-Token", accessToken)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(new GraphQLRequest(query))
                .retrieve()
                .bodyToMono(responseType)
                .doOnSuccess(r -> log.debug("Async GraphQL request successful for shop: {}", shop))
                .doOnError(e -> log.error("Async GraphQL error for shop: {}", shop, e));
    }

    /**
     * Execute a mutation without expecting a complex response (fire-and-forget with
     * logging).
     *
     * @param shop        The Shopify store domain
     * @param accessToken The access token for authentication
     * @param mutation    The GraphQL mutation string
     * @return true if successful, false otherwise
     */
    public boolean executeMutation(String shop, String accessToken, String mutation) {
        String url = clientConfig.buildGraphQLUrl(shop);

        try {
            String response = shopifyWebClient.post()
                    .uri(url)
                    .header("X-Shopify-Access-Token", accessToken)
                    .contentType(MediaType.APPLICATION_JSON)
                    .bodyValue(new GraphQLRequest(mutation))
                    .retrieve()
                    .bodyToMono(String.class)
                    .block();

            // Basic check for errors in response
            if (response != null && response.contains("\"errors\"")) {
                log.warn("GraphQL mutation returned errors for shop {}: {}", shop, response);
                return false;
            }

            log.debug("Mutation executed successfully for shop: {}", shop);
            return true;

        } catch (Exception e) {
            log.error("Error executing mutation for shop: {}", shop, e);
            return false;
        }
    }
}
