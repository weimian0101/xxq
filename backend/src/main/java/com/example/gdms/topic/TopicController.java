package com.example.gdms.topic;

import com.example.gdms.common.ApiResponse;
import com.example.gdms.user.User;
import com.example.gdms.user.UserRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/topics")
public class TopicController {
    private final TopicService topicService;
    private final UserRepository userRepository;

    public TopicController(TopicService topicService, UserRepository userRepository) {
        this.topicService = topicService;
        this.userRepository = userRepository;
    }

    @GetMapping
    public ApiResponse<List<Topic>> list() {
        return ApiResponse.ok(topicService.list());
    }

    @GetMapping("/page")
    public ApiResponse<Page<Topic>> listPage(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) Long creatorId) {
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        TopicStatus topicStatus = null;
        if (status != null && !status.trim().isEmpty()) {
            try {
                topicStatus = TopicStatus.valueOf(status.toUpperCase());
            } catch (IllegalArgumentException e) {
                // 忽略无效的状态值
            }
        }
        return ApiResponse.ok(topicService.findTopics(pageable, keyword, topicStatus, creatorId));
    }

    @GetMapping("/{id}")
    public ApiResponse<Topic> get(@PathVariable Long id) {
        return ApiResponse.ok(topicService.getById(id));
    }

    @GetMapping("/{id}/approvals")
    public List<TopicApproval> approvals(@PathVariable Long id) {
        return topicService.approvals(id);
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN','TEACHER')")
    public ApiResponse<Topic> create(@RequestBody Topic t, Principal principal) {
        if (principal != null) {
            t.setCreatorId(userRepository.findByUsername(principal.getName())
                    .map(User::getId)
                    .orElse(null));
        }
        return ApiResponse.ok(topicService.create(t));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','TEACHER')")
    public ApiResponse<Topic> update(@PathVariable Long id, @RequestBody Topic t, Principal principal) {
        Topic existing = topicService.getById(id);
        // 检查权限：只有创建者或管理员可以编辑
        if (principal != null) {
            Long currentUserId = userRepository.findByUsername(principal.getName())
                    .map(User::getId)
                    .orElse(null);
            boolean isAdmin = userRepository.findByUsername(principal.getName())
                    .map(u -> "ADMIN".equals(u.getRole()))
                    .orElse(false);
            if (!isAdmin && (existing.getCreatorId() == null || !existing.getCreatorId().equals(currentUserId))) {
                throw new IllegalStateException("只能编辑自己创建的课题");
            }
        }
        // 已提交或已审批的课题不能编辑
        if (existing.getStatus() != TopicStatus.DRAFT) {
            throw new IllegalStateException("只能编辑草稿状态的课题");
        }
        return ApiResponse.ok(topicService.update(id, t));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','TEACHER')")
    public ApiResponse<?> delete(@PathVariable Long id, Principal principal) {
        Topic existing = topicService.getById(id);
        // 检查权限：只有创建者或管理员可以删除
        if (principal != null) {
            Long currentUserId = userRepository.findByUsername(principal.getName())
                    .map(User::getId)
                    .orElse(null);
            boolean isAdmin = userRepository.findByUsername(principal.getName())
                    .map(u -> "ADMIN".equals(u.getRole()))
                    .orElse(false);
            if (!isAdmin && (existing.getCreatorId() == null || !existing.getCreatorId().equals(currentUserId))) {
                throw new IllegalStateException("只能删除自己创建的课题");
            }
        }
        topicService.delete(id);
        return ApiResponse.ok();
    }

    @PostMapping("/{id}/submit")
    @PreAuthorize("hasAnyRole('ADMIN','TEACHER')")
    public ApiResponse<Topic> submit(@PathVariable Long id) {
        return ApiResponse.ok(topicService.submit(id));
    }

    @PostMapping("/{id}/approve")
    @PreAuthorize("hasAnyRole('ADMIN','TEACHER')")
    public ApiResponse<Topic> approve(@PathVariable Long id, @RequestBody Map<String, String> body, Principal principal) {
        String decision = body.getOrDefault("decision", "APPROVED");
        String comment = body.getOrDefault("comment", "");
        if (comment == null || comment.trim().isEmpty()) {
            throw new IllegalArgumentException("审批意见不能为空");
        }
        Long reviewerId = null;
        if (principal != null) {
            reviewerId = userRepository.findByUsername(principal.getName())
                    .map(User::getId)
                    .orElse(null);
        }
        return ApiResponse.ok(topicService.approve(id, reviewerId, TopicStatus.valueOf(decision), comment));
    }

    @PostMapping("/{id}/select")
    @PreAuthorize("hasRole('STUDENT')")
    public ResponseEntity<?> select(@PathVariable Long id, @RequestBody Map<String, Long> body, Principal principal) {
        Long studentId = null;
        if (principal != null) {
            studentId = userRepository.findByUsername(principal.getName())
                    .map(User::getId)
                    .orElse(null);
        }
        if (studentId == null) {
            return ResponseEntity.badRequest().body("无法识别学生身份");
        }
        return ResponseEntity.ok(topicService.select(id, studentId));
    }

    @PostMapping("/selections/{selectionId}/lock")
    @PreAuthorize("hasAnyRole('ADMIN','TEACHER')")
    public ResponseEntity<?> lockSelection(@PathVariable Long selectionId) {
        return ResponseEntity.ok(topicService.lockSelection(selectionId));
    }

    @PostMapping("/selections/{selectionId}/cancel")
    @PreAuthorize("hasAnyRole('ADMIN','STUDENT')")
    public ResponseEntity<?> cancelSelection(@PathVariable Long selectionId, Principal principal) {
        Long studentId = null;
        if (principal != null) {
            studentId = userRepository.findByUsername(principal.getName())
                    .map(User::getId)
                    .orElse(null);
        }
        return ResponseEntity.ok(topicService.cancelSelection(selectionId, studentId));
    }

    @GetMapping("/{id}/selections")
    public ResponseEntity<?> getSelections(@PathVariable Long id) {
        return ResponseEntity.ok(topicService.getSelections(id));
    }

    @GetMapping("/my-selection")
    @PreAuthorize("hasRole('STUDENT')")
    public ApiResponse<StudentSelection> getMySelection(Principal principal) {
        Long studentId = null;
        if (principal != null) {
            studentId = userRepository.findByUsername(principal.getName())
                    .map(User::getId)
                    .orElse(null);
        }
        if (studentId == null) {
            throw new IllegalArgumentException("无法识别学生身份");
        }
        return ApiResponse.ok(topicService.getActiveSelection(studentId).orElse(null));
    }

    @GetMapping("/selections/my")
    @PreAuthorize("hasRole('STUDENT')")
    public ApiResponse<List<StudentSelection>> getMySelections(Principal principal) {
        Long studentId = null;
        if (principal != null) {
            studentId = userRepository.findByUsername(principal.getName())
                    .map(User::getId)
                    .orElse(null);
        }
        if (studentId == null) {
            throw new IllegalArgumentException("无法识别学生身份");
        }
        return ApiResponse.ok(topicService.getStudentSelections(studentId));
    }

    @GetMapping("/my-students")
    @PreAuthorize("hasAnyRole('TEACHER','ADMIN')")
    public ApiResponse<List<Map<String, Object>>> getMyStudents(Principal principal) {
        Long teacherId = null;
        if (principal != null) {
            teacherId = userRepository.findByUsername(principal.getName())
                    .map(User::getId)
                    .orElse(null);
        }
        if (teacherId == null) {
            throw new IllegalArgumentException("无法识别教师身份");
        }
        return ApiResponse.ok(topicService.getTeacherStudents(teacherId));
    }
}

