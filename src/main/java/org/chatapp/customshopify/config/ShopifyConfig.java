package org.chatapp.customshopify.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConfigurationProperties(prefix = "shopify")
@Data
public class ShopifyConfig {
    
    private Api api = new Api();
    private App app = new App();
    private String scopes;
    
    @Data
    public static class Api {
        private String key;
        private String secret;
        private String version;
        private Endpoints endpoints = new Endpoints();
        
        @Data
        public static class Endpoints {
            private String graphql;
            private String oauthAuthorize;
            private String oauthToken;
        }
    }
    
    @Data
    public static class App {
        private String url;
    }
    
    public String getApiKey() {
        return api.getKey();
    }
    
    public String getApiSecret() {
        return api.getSecret();
    }
    
    public String getApiVersion() {
        return api.getVersion();
    }
    
    public String getAppUrl() {
        return app.getUrl();
    }

    public String getGraphqlUrl() {
        return api.getEndpoints().getGraphql();
    }

    public String getOauthAuthorizeUrl() {
        return api.getEndpoints().getOauthAuthorize();
    }

    public String getOauthTokenUrl() {
        return api.getEndpoints().getOauthToken();
    }
}
