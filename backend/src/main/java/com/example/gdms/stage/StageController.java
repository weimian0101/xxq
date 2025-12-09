package com.example.gdms.stage;

import com.example.gdms.user.User;
import com.example.gdms.user.UserRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/stages")
public class StageController {
    private final StageService stageService;
    private final UserRepository userRepository;

    public StageController(StageService stageService, UserRepository userRepository) {
        this.stageService = stageService;
        this.userRepository = userRepository;
    }

    @GetMapping
    public List<StageConfig> listStages() {
        return stageService.stages();
    }

    @GetMapping("/all")
    @PreAuthorize("hasRole('ADMIN')")
    public List<StageConfig> listAll() {
        return stageService.stagesAll();
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public StageConfig create(@RequestBody StageConfig cfg) {
        return stageService.createStage(cfg);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public StageConfig update(@PathVariable Long id, @RequestBody StageConfig cfg) {
        return stageService.updateStage(id, cfg);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> delete(@PathVariable Long id) {
        stageService.deleteStage(id);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/{id}/tasks")
    @PreAuthorize("hasRole('STUDENT')")
    public StageTask submitTask(@PathVariable Long id, @RequestBody Map<String, Object> body, Principal principal) {
        Long studentId = null;
        if (principal != null) {
            studentId = userRepository.findByUsername(principal.getName())
                    .map(User::getId)
                    .orElse(null);
        }
        Long topicId = body.get("topicId") == null ? null : Long.parseLong(body.get("topicId").toString());
        String content = (String) body.getOrDefault("content", "");
        return stageService.submitTask(id, studentId, topicId, content);
    }

    @GetMapping("/tasks")
    public List<StageTask> tasks(@RequestParam(value = "studentId", required = false) Long studentId) {
        return stageService.listTasks(studentId);
    }

    @GetMapping("/tasks/{taskId}")
    public StageTask getTask(@PathVariable Long taskId) {
        return stageService.getTask(taskId);
    }

    @GetMapping("/tasks/{taskId}/reviews")
    public List<StageReview> getTaskReviews(@PathVariable Long taskId) {
        return stageService.getTaskReviews(taskId);
    }

    @PostMapping("/tasks/{taskId}/review")
    @PreAuthorize("hasAnyRole('ADMIN','TEACHER')")
    public ResponseEntity<?> review(@PathVariable Long taskId, @RequestBody Map<String, String> body, Principal principal) {
        String decisionStr = body.getOrDefault("decision", "APPROVED");
        String comment = body.getOrDefault("comment", "");
        StageTask.TaskStatus decision = StageTask.TaskStatus.valueOf(decisionStr);
        Long reviewerId = null;
        if (principal != null) {
            reviewerId = userRepository.findByUsername(principal.getName())
                    .map(User::getId)
                    .orElse(null);
        }
        return ResponseEntity.ok(stageService.review(taskId, reviewerId, decision, comment));
    }
}

