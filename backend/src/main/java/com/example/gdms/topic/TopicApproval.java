package com.example.gdms.topic;

import jakarta.persistence.*;
import lombok.Data;

import java.time.LocalDateTime;

@Entity
@Table(name = "topic_approvals")
@Data
public class TopicApproval {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private Long topicId;
    private Long reviewerId;
    @Enumerated(EnumType.STRING)
    private TopicStatus decision;
    private String comment;
    private LocalDateTime createdAt = LocalDateTime.now();
}

