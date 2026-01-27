package org.chatapp.customshopify.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "app_settings")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class AppSettings {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "shop", unique = true, nullable = false)
    private String shop;

    @Column(name = "discount_feature_enabled")
    private Boolean discountFeatureEnabled = false;

    @Column(nullable = false)
    private Boolean isAutoPublish;

    public AppSettings(String shop) {
        this.shop = shop;
        this.discountFeatureEnabled = false;
        this.isAutoPublish = false;
    }
}
