package com.example.gdms.stage;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class StageService {
    private final StageConfigRepository configRepository;
    private final StageTaskRepository taskRepository;
    private final StageReviewRepository reviewRepository;

    public StageService(StageConfigRepository configRepository, StageTaskRepository taskRepository, StageReviewRepository reviewRepository) {
        this.configRepository = configRepository;
        this.taskRepository = taskRepository;
        this.reviewRepository = reviewRepository;
    }

    public List<StageConfig> stages() {
        return configRepository.findAllByActiveTrueOrderByOrderIndexAsc();
    }

    public List<StageConfig> stagesAll() {
        return configRepository.findAll();
    }

    public StageConfig createStage(StageConfig cfg) {
        return configRepository.save(cfg);
    }

    public StageConfig updateStage(Long id, StageConfig body) {
        StageConfig cfg = configRepository.findById(id).orElseThrow();
        if (body.getName() != null) cfg.setName(body.getName());
        if (body.getOrderIndex() != null) cfg.setOrderIndex(body.getOrderIndex());
        if (body.getActive() != null) cfg.setActive(body.getActive());
        if (body.getStartAt() != null) cfg.setStartAt(body.getStartAt());
        if (body.getEndAt() != null) cfg.setEndAt(body.getEndAt());
        return configRepository.save(cfg);
    }

    public void deleteStage(Long id) {
        configRepository.deleteById(id);
    }

    public StageTask submitTask(Long stageId, Long studentId, Long topicId, String content) {
        StageConfig stage = configRepository.findById(stageId).orElseThrow();
        if (Boolean.FALSE.equals(stage.getActive())) {
            throw new IllegalStateException("当前阶段未开启");
        }
        LocalDateTime now = LocalDateTime.now();
        if (stage.getStartAt() != null && now.isBefore(stage.getStartAt())) {
            throw new IllegalStateException("阶段尚未开始");
        }
        if (stage.getEndAt() != null && now.isAfter(stage.getEndAt())) {
            throw new IllegalStateException("阶段已结束");
        }
        if (studentId == null) {
            throw new IllegalArgumentException("缺少学生标识");
        }
        StageConfig prev = (stage.getOrderIndex() == null) ? null :
                configRepository.findTopByOrderIndexLessThanOrderByOrderIndexDesc(stage.getOrderIndex())
                        .orElse(null);
        if (prev != null && studentId != null) {
            taskRepository.findTopByStudentIdAndStageIdOrderByIdDesc(studentId, prev.getId())
                    .filter(t -> t.getStatus() == StageTask.TaskStatus.APPROVED)
                    .orElseThrow(() -> new IllegalStateException("请先完成上一阶段"));
        }

        StageTask task = new StageTask();
        task.setStageId(stageId);
        task.setStudentId(studentId);
        task.setTopicId(topicId);
        task.setContent(content);
        task.setStatus(StageTask.TaskStatus.SUBMITTED);
        return taskRepository.save(task);
    }

    public List<StageTask> listTasks(Long studentId) {
        if (studentId == null) {
            return taskRepository.findAll();
        }
        return taskRepository.findByStudentId(studentId);
    }

    public StageTask getTask(Long taskId) {
        return taskRepository.findById(taskId).orElseThrow();
    }

    public List<StageReview> getTaskReviews(Long taskId) {
        return reviewRepository.findByTaskId(taskId);
    }

    @Transactional
    public StageTask review(Long taskId, Long reviewerId, StageTask.TaskStatus decision, String comment) {
        StageTask task = taskRepository.findById(taskId).orElseThrow();
        task.setStatus(decision);
        task.setUpdatedAt(LocalDateTime.now());
        StageReview r = new StageReview();
        r.setTaskId(taskId);
        r.setReviewerId(reviewerId);
        r.setDecision(decision);
        r.setComment(comment);
        reviewRepository.save(r);
        return taskRepository.save(task);
    }
}

