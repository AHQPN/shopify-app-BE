package org.chatapp.customshopify.client;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.Map;

@Component
@Slf4j
public class CaptchaClient {

    private final WebClient webClient;
    private final String secretKey;
    private static final String GOOGLE_VERIFY_URL = "https://www.google.com/recaptcha/api/siteverify";

    public CaptchaClient(WebClient shopifyWebClient, @Value("${google.recaptcha.secret:}") String secretKey) {
        this.webClient = shopifyWebClient;
        this.secretKey = secretKey;
    }

    /**
     * Verifies the reCAPTCHA token with Google.
     * 
     * @param response the token from the client
     * @return true if valid, false otherwise
     */
    public boolean verify(String response) {
        if (response == null || response.isEmpty()) {
            log.warn("reCAPTCHA response is empty");
            return false;
        }

        if (secretKey == null || secretKey.isEmpty()) {
            log.warn("reCAPTCHA secret key is not configured. Skipping verification.");
            return true; // Skip if not configured
        }

        MultiValueMap<String, String> formData = new LinkedMultiValueMap<>();
        formData.add("secret", secretKey);
        formData.add("response", response);

        try {
            Map<String, Object> result = webClient.post()
                    .uri(GOOGLE_VERIFY_URL)
                    .body(BodyInserters.fromFormData(formData))
                    .retrieve()
                    .bodyToMono(new ParameterizedTypeReference<Map<String, Object>>() {
                    })
                    .block();

            if (result != null && Boolean.TRUE.equals(result.get("success"))) {
                log.info("reCAPTCHA verification successful");
                return true;
            } else {
                log.warn("reCAPTCHA verification failed: {}", result);
                return false;
            }
        } catch (Exception e) {
            log.error("Error during reCAPTCHA verification", e);
            return false;
        }
    }
}
