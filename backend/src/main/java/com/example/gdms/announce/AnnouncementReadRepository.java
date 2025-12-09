package com.example.gdms.announce;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface AnnouncementReadRepository extends JpaRepository<AnnouncementRead, Long> {
    boolean existsByAnnouncementIdAndUserId(Long announcementId, Long userId);
    long countByAnnouncementId(Long announcementId);
    
    @Modifying
    @Query("DELETE FROM AnnouncementRead r WHERE r.announcementId = :announcementId")
    void deleteByAnnouncementId(@Param("announcementId") Long announcementId);
}

