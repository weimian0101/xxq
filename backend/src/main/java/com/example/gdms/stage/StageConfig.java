package com.example.gdms.stage;

import jakarta.persistence.*;
import lombok.Data;

import java.time.LocalDateTime;

@Entity
@Table(name = "stage_config")
@Data
public class StageConfig {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private String name; // 开题/中期/答辩前
    private Integer orderIndex;
    private Boolean active = true;
    private LocalDateTime startAt;
    private LocalDateTime endAt;
}

