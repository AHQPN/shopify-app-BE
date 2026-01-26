package org.chatapp.customshopify.dto.request;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class TokenExchangeRequest {
    @JsonProperty("client_id")
    private String clientId;
    
    @JsonProperty("client_secret")
    private String clientSecret;
    
    @JsonProperty("grant_type")
    private String grantType;
    
    @JsonProperty("subject_token")
    private String subjectToken;
    
    @JsonProperty("subject_token_type")
    private String subjectTokenType;
    
    @JsonProperty("requested_token_type")
    private String requestedTokenType;
}
