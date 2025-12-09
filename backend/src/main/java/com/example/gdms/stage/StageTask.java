package com.example.gdms.stage;

import jakarta.persistence.*;
import lombok.Data;

import java.time.LocalDateTime;

@Entity
@Table(name = "stage_task")
@Data
public class StageTask {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private Long stageId;
    private Long studentId;
    private Long topicId;
    @Enumerated(EnumType.STRING)
    private TaskStatus status = TaskStatus.PENDING;
    @Lob
    private String content;
    private LocalDateTime updatedAt = LocalDateTime.now();

    public enum TaskStatus {PENDING, SUBMITTED, APPROVED, REJECTED}
}

