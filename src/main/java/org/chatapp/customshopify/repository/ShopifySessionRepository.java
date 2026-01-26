package org.chatapp.customshopify.repository;

import org.chatapp.customshopify.entity.ShopifySession;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ShopifySessionRepository extends JpaRepository<ShopifySession, String> {
    
    List<ShopifySession> findByShop(String shop);
    
    Optional<ShopifySession> findByShopAndIsOnline(String shop, Boolean isOnline);
    
    void deleteByShop(String shop);
}
