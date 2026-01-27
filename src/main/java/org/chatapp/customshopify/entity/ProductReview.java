package org.chatapp.customshopify.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.chatapp.customshopify.enums.HideReason;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;
import java.util.List;

@Entity
@Table(name = "product_reviews", indexes = {
    @Index(name = "idx_product_reviews_shop", columnList = "shop"),
    @Index(name = "idx_product_reviews_product_id", columnList = "product_id")
})
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProductReview {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String shop;

    @Column(name = "product_id", nullable = false)
    private String productId;

    @Column(name = "product_name" , nullable = false)
    private String productName;

    @Column(name = "customer_id")
    private String customerId;

    @Column(name = "customer_name")
    private String customerName;

    @Column(name = "avatar_url")
    private String avatarUrl;

    @Column(columnDefinition = "TEXT")
    private String comment;

    @Column(nullable = true)
    private Integer rating; // 1 to 5

    @Column(nullable = false)
    private Boolean status;

    @Enumerated(EnumType.STRING)
    private HideReason  hideReason;

    private Long replyTo;

    @OneToMany(mappedBy = "review", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<ReviewMedia> media;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;
}
