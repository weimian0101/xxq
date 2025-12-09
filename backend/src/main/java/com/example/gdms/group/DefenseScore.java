package com.example.gdms.group;

import jakarta.persistence.*;
import lombok.Data;

@Entity
@Table(name = "defense_score")
@Data
public class DefenseScore {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private Long groupId;
    private Long studentId;
    private Double score;
    private String comment;
}

