package com.example.gdms.application;

import jakarta.persistence.*;
import lombok.Data;

import java.time.LocalDateTime;

@Entity
@Table(name = "application_logs")
@Data
public class ApplicationLog {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private Long applicationId;
    private Long actorId;
    private String action;
    private String comment;
    private LocalDateTime createdAt = LocalDateTime.now();
}

