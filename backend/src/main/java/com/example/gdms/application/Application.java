package com.example.gdms.application;

import jakarta.persistence.*;
import lombok.Data;

import java.time.LocalDateTime;

@Entity
@Table(name = "applications")
@Data
public class Application {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Enumerated(EnumType.STRING)
    private ApplicationType type;
    private Long studentId;
    private Long topicId;
    @Enumerated(EnumType.STRING)
    private ApplicationStatus status = ApplicationStatus.SUBMITTED;
    @Lob
    private String payload;
    private LocalDateTime createdAt = LocalDateTime.now();

    public enum ApplicationStatus {SUBMITTED, APPROVED, REJECTED}
}

