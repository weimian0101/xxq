package com.example.gdms.stage;

import jakarta.persistence.*;
import lombok.Data;

import java.time.LocalDateTime;

@Entity
@Table(name = "stage_review")
@Data
public class StageReview {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private Long taskId;
    private Long reviewerId;
    @Enumerated(EnumType.STRING)
    private StageTask.TaskStatus decision;
    private String comment;
    private LocalDateTime createdAt = LocalDateTime.now();
}

