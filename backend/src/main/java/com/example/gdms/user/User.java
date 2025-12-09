package com.example.gdms.user;

import jakarta.persistence.*;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.Set;

@Entity
@Table(name = "users")
@Data
public class User {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, nullable = false)
    private String username;

    @Column(nullable = false)
    private String password;

    private String fullName;

    private String role; // ADMIN/TEACHER/STUDENT/STAFF

    private Long orgId;

    private Boolean enabled = true;

    private LocalDateTime createdAt = LocalDateTime.now();

    private String phone;
    private String signatureUrl;
}

