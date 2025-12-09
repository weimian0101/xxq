package com.example.gdms.stage;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface StageReviewRepository extends JpaRepository<StageReview, Long> {
    List<StageReview> findByTaskId(Long taskId);
}

