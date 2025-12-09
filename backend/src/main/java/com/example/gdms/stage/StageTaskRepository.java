package com.example.gdms.stage;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface StageTaskRepository extends JpaRepository<StageTask, Long> {
    List<StageTask> findByStudentId(Long studentId);
    List<StageTask> findByStageId(Long stageId);
    Optional<StageTask> findTopByStudentIdAndStageIdOrderByIdDesc(Long studentId, Long stageId);
}

