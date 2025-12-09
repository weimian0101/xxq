package com.example.gdms.topic;

import jakarta.persistence.*;
import lombok.Data;

import java.time.LocalDateTime;

@Entity
@Table(name = "student_selections")
@Data
public class StudentSelection {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private Long studentId;
    private Long topicId;
    @Enumerated(EnumType.STRING)
    private SelectionStatus status = SelectionStatus.SELECTED;
    private LocalDateTime createdAt = LocalDateTime.now();

    public enum SelectionStatus {
        SELECTED, LOCKED, CANCELLED
    }
}

