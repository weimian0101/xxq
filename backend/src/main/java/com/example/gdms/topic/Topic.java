package com.example.gdms.topic;

import jakarta.persistence.*;
import lombok.Data;

import java.time.LocalDateTime;

@Entity
@Table(name = "topics")
@Data
public class Topic {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private String title;
    private String description;
    private Long creatorId;
    private Integer capacity = 1;
    @Enumerated(EnumType.STRING)
    private TopicStatus status = TopicStatus.DRAFT;
    private LocalDateTime createdAt = LocalDateTime.now();
}

