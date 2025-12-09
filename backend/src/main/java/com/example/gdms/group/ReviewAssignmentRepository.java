package com.example.gdms.group;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.List;

public interface ReviewAssignmentRepository extends JpaRepository<ReviewAssignment, Long>, JpaSpecificationExecutor<ReviewAssignment> {
    List<ReviewAssignment> findByReviewerId(Long reviewerId);
    boolean existsByReviewerIdAndStudentIdAndType(Long reviewerId, Long studentId, String type);
}

