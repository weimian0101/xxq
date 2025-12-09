package com.example.gdms.group;

import jakarta.persistence.*;
import lombok.Data;

import java.time.LocalDateTime;

@Entity
@Table(name = "defense_group")
@Data
public class DefenseGroup {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private String name;
    private String type; // OPENING / FINAL
    private Integer capacity = 8;
    private LocalDateTime createdAt = LocalDateTime.now();
}

