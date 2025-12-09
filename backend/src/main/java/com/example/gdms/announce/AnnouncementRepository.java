package com.example.gdms.announce;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.List;

public interface AnnouncementRepository extends JpaRepository<Announcement, Long>, JpaSpecificationExecutor<Announcement> {
    List<Announcement> findByStatus(String status);
    List<Announcement> findByStatusOrderByCreatedAtDesc(String status);
    long countByStatus(String status);
}

