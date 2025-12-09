package com.example.gdms.application;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.List;

public interface ApplicationRepository extends JpaRepository<Application, Long>, JpaSpecificationExecutor<Application> {
    List<Application> findByStudentId(Long studentId);
    List<Application> findByType(ApplicationType type);
    List<Application> findByStatus(Application.ApplicationStatus status);
    List<Application> findByStudentIdAndStatus(Long studentId, Application.ApplicationStatus status);
}

