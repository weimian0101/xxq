package com.example.gdms.announce;

import jakarta.persistence.*;
import lombok.Data;

import java.time.LocalDateTime;

@Entity
@Table(name = "announcements")
@Data
public class Announcement {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private String title;
    @Lob
    private String content;
    private Long createdBy;
    private String status = "DRAFT"; // DRAFT / PUBLISHED
    private LocalDateTime createdAt = LocalDateTime.now();
    private LocalDateTime publishedAt;
}

