package com.example.gdms.group;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface DefenseScoreRepository extends JpaRepository<DefenseScore, Long> {
    List<DefenseScore> findByGroupId(Long groupId);
    List<DefenseScore> findByStudentId(Long studentId);
}

