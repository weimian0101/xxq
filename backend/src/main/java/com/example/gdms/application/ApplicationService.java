package com.example.gdms.application;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import jakarta.persistence.criteria.Predicate;
import java.util.ArrayList;
import java.util.List;

@Service
public class ApplicationService {
    private final ApplicationRepository applicationRepository;
    private final ApplicationLogRepository logRepository;

    public ApplicationService(ApplicationRepository applicationRepository, ApplicationLogRepository logRepository) {
        this.applicationRepository = applicationRepository;
        this.logRepository = logRepository;
    }

    public Application create(Application application) {
        Application saved = applicationRepository.save(application);
        ApplicationLog log = new ApplicationLog();
        log.setApplicationId(saved.getId());
        log.setActorId(saved.getStudentId());
        log.setAction("SUBMITTED");
        logRepository.save(log);
        return saved;
    }

    public List<Application> list(Long studentId, ApplicationType type, Application.ApplicationStatus status) {
        List<Application> base;
        if (studentId != null && status != null) {
            base = applicationRepository.findByStudentIdAndStatus(studentId, status);
        } else if (studentId != null) {
            base = applicationRepository.findByStudentId(studentId);
        } else if (type != null && status != null) {
            base = applicationRepository.findByType(type).stream()
                    .filter(a -> a.getStatus() == status)
                    .toList();
        } else if (type != null) {
            base = applicationRepository.findByType(type);
        } else if (status != null) {
            base = applicationRepository.findByStatus(status);
        } else {
            base = applicationRepository.findAll();
        }
        return base;
    }

    public Page<Application> findApplications(Pageable pageable, String keyword, ApplicationType type, 
                                              Application.ApplicationStatus status, Long studentId) {
        Specification<Application> spec = (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            
            if (keyword != null && !keyword.trim().isEmpty()) {
                String pattern = "%" + keyword.trim() + "%";
                predicates.add(cb.like(root.get("payload"), pattern));
            }
            
            if (type != null) {
                predicates.add(cb.equal(root.get("type"), type));
            }
            
            if (status != null) {
                predicates.add(cb.equal(root.get("status"), status));
            }
            
            if (studentId != null) {
                predicates.add(cb.equal(root.get("studentId"), studentId));
            }
            
            return cb.and(predicates.toArray(new Predicate[0]));
        };
        
        return applicationRepository.findAll(spec, pageable);
    }

    public Application getById(Long id) {
        return applicationRepository.findById(id).orElseThrow();
    }

    @Transactional
    public Application review(Long id, Long actorId, Application.ApplicationStatus decision, String comment) {
        Application a = applicationRepository.findById(id).orElseThrow();
        a.setStatus(decision);
        ApplicationLog log = new ApplicationLog();
        log.setApplicationId(id);
        log.setActorId(actorId);
        log.setAction(decision.name());
        log.setComment(comment);
        logRepository.save(log);
        return applicationRepository.save(a);
    }

    public List<ApplicationLog> getLogs(Long applicationId) {
        return logRepository.findByApplicationId(applicationId);
    }

    @Transactional
    public Application withdraw(Long id, Long studentId) {
        Application a = applicationRepository.findById(id).orElseThrow();
        if (studentId != null && !a.getStudentId().equals(studentId)) {
            throw new IllegalStateException("只能撤回自己的申请");
        }
        if (a.getStatus() != Application.ApplicationStatus.SUBMITTED) {
            throw new IllegalStateException("只能撤回待审批的申请");
        }
        a.setStatus(Application.ApplicationStatus.REJECTED);
        ApplicationLog log = new ApplicationLog();
        log.setApplicationId(id);
        log.setActorId(studentId);
        log.setAction("WITHDRAWN");
        log.setComment("学生撤回");
        logRepository.save(log);
        return applicationRepository.save(a);
    }

    @Transactional
    public Application resubmit(Long id, Long studentId, String payload) {
        Application a = applicationRepository.findById(id).orElseThrow();
        if (studentId != null && !a.getStudentId().equals(studentId)) {
            throw new IllegalStateException("只能重新提交自己的申请");
        }
        if (a.getStatus() != Application.ApplicationStatus.REJECTED) {
            throw new IllegalStateException("只能重新提交被驳回的申请");
        }
        a.setStatus(Application.ApplicationStatus.SUBMITTED);
        if (payload != null && !payload.trim().isEmpty()) {
            a.setPayload(payload);
        }
        ApplicationLog log = new ApplicationLog();
        log.setApplicationId(id);
        log.setActorId(studentId);
        log.setAction("RESUBMITTED");
        log.setComment("学生重新提交");
        logRepository.save(log);
        return applicationRepository.save(a);
    }
}

