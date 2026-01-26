package org.chatapp.customshopify.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "shopify_sessions")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ShopifySession {
    
    @Id
    private String id;
    
    @Column(nullable = false)
    private String shop;
    
    @Column(nullable = false)
    private String state;
    
    @Column(nullable = false)
    private Boolean isOnline = false;
    
    @Column(length = 4000)
    private String scope;
    
    private LocalDateTime expires;
    
    @Column(nullable = false)
    private String accessToken;
    
    private Long userId;
    
    private String firstName;
    
    private String lastName;
    
    private String email;
    
    private Boolean accountOwner = false;
    
    private String locale;
    
    private Boolean collaborator = false;
    
    private Boolean emailVerified = false;
    
    private String refreshToken;
    
    private LocalDateTime refreshTokenExpires;
    
    @Column(updatable = false)
    private LocalDateTime createdAt = LocalDateTime.now();
    
    private LocalDateTime updatedAt = LocalDateTime.now();
    
    @PreUpdate
    public void preUpdate() {
        this.updatedAt = LocalDateTime.now();
    }
}
