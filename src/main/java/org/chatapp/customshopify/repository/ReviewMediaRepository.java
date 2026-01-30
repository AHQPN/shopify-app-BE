package org.chatapp.customshopify.repository;

import org.chatapp.customshopify.entity.ReviewMedia;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface ReviewMediaRepository extends JpaRepository<ReviewMedia, Long> {
    @Modifying
    @Query("UPDATE ReviewMedia r SET r.isHidden = :isHidden WHERE r.id = :id")
    void updateStatus(@Param("id") Long id, @Param("isHidden") Boolean status);
}
