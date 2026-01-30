package org.chatapp.customshopify.client;

import lombok.RequiredArgsConstructor;
import org.chatapp.customshopify.config.ShopifyConfig;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.web.reactive.function.client.ExchangeStrategies;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.netty.http.client.HttpClient;

import java.time.Duration;

/**
 * Configuration for WebClient beans used to communicate with Shopify APIs.
 * Provides configured WebClient instances with proper timeouts and buffer
 * sizes.
 */
@Configuration
@RequiredArgsConstructor
public class ShopifyClientConfig {

    private final ShopifyConfig shopifyConfig;

    private static final int RESPONSE_TIMEOUT_SECONDS = 30;
    private static final int MAX_MEMORY_SIZE = 16 * 1024 * 1024; // 16MB


    @Bean
    public WebClient shopifyWebClient() {
        HttpClient httpClient = HttpClient.create()
                .responseTimeout(Duration.ofSeconds(RESPONSE_TIMEOUT_SECONDS));

        ExchangeStrategies strategies = ExchangeStrategies.builder()
                .codecs(configurer -> configurer
                        .defaultCodecs()
                        .maxInMemorySize(MAX_MEMORY_SIZE))
                .build();

        return WebClient.builder()
                .clientConnector(new ReactorClientHttpConnector(httpClient))
                .exchangeStrategies(strategies)
                .defaultHeader("Content-Type", "application/json")
                .build();
    }


    public String buildGraphQLUrl(String shop) {
        return String.format(shopifyConfig.getGraphqlUrl(), shop, shopifyConfig.getApiVersion());
    }


    public String buildOAuthTokenUrl(String shop) {
        return String.format(shopifyConfig.getOauthTokenUrl(), shop);
    }
}
