package com.example.gdms.org;

import jakarta.persistence.*;
import lombok.Data;

@Entity
@Table(name = "orgs")
@Data
public class Org {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private String name;
    private Long parentId;
    private String type; // COLLEGE / DEPT
}

