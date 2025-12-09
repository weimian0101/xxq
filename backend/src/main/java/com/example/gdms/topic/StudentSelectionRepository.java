package com.example.gdms.topic;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface StudentSelectionRepository extends JpaRepository<StudentSelection, Long> {
    List<StudentSelection> findByTopicId(Long topicId);
    List<StudentSelection> findByStudentId(Long studentId);
    Optional<StudentSelection> findByStudentIdAndStatus(Long studentId, StudentSelection.SelectionStatus status);
    boolean existsByStudentIdAndStatus(Long studentId, StudentSelection.SelectionStatus status);
}

