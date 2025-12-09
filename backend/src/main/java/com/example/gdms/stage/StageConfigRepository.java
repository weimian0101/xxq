package com.example.gdms.stage;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface StageConfigRepository extends JpaRepository<StageConfig, Long> {
    List<StageConfig> findAllByActiveTrueOrderByOrderIndexAsc();
    Optional<StageConfig> findTopByOrderIndexLessThanOrderByOrderIndexDesc(Integer orderIndex);
}

