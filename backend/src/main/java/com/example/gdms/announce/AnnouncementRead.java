package com.example.gdms.announce;

import jakarta.persistence.*;
import lombok.Data;

import java.time.LocalDateTime;

@Entity
@Table(name = "announcement_reads")
@Data
public class AnnouncementRead {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private Long announcementId;
    private Long userId;
    private LocalDateTime readAt = LocalDateTime.now();
}

