package com.example.gdms.group;

import jakarta.persistence.*;
import lombok.Data;

import java.time.LocalDateTime;

@Entity
@Table(name = "review_assignment")
@Data
public class ReviewAssignment {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private Long reviewerId;
    private Long studentId;
    private Long topicId;
    private String type; // CROSS or ADVISOR
    private String status; // PENDING/DONE
    @Lob
    private String comment;
    private Double score;
    private LocalDateTime createdAt = LocalDateTime.now();
}

