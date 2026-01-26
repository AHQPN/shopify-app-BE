package org.chatapp.customshopify.controller;

import org.chatapp.customshopify.dto.response.ApiResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api")
public class HealthController {
    
    @GetMapping("/health")
    public ResponseEntity<ApiResponse<Map<String, Object>>> health() {
        Map<String, Object> response = new HashMap<>();
        response.put("status", "UP");
        response.put("service", "Shopify App Backend");
        response.put("timestamp", LocalDateTime.now());
        response.put("message", "Spring Boot backend is running!");
        
        return ResponseEntity.ok(ApiResponse.<Map<String, Object>>builder()
                .data(response)
                .build());
    }
    
    @GetMapping
    public ResponseEntity<ApiResponse<Map<String, Object>>> root() {
        Map<String, Object> response = new HashMap<>();
        response.put("message", "Shopify App Backend API");
        response.put("version", "1.0.0");
        response.put("endpoints", Map.of(
            "health", "/api/health",
            "auth", "/api/auth?shop=yourshop.myshopify.com",
            "webhooks", "/webhooks/app/*"
        ));
        
        return ResponseEntity.ok(ApiResponse.<Map<String, Object>>builder()
                .data(response)
                .build());
    }
}
