package com.example.gdms.application;

import com.example.gdms.common.ApiResponse;
import com.example.gdms.user.User;
import com.example.gdms.user.UserRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/applications")
public class ApplicationController {
    private final ApplicationService applicationService;
    private final UserRepository userRepository;

    public ApplicationController(ApplicationService applicationService, UserRepository userRepository) {
        this.applicationService = applicationService;
        this.userRepository = userRepository;
    }

    @PostMapping("/{type}")
    @PreAuthorize("hasRole('STUDENT')")
    public ApiResponse<Application> create(@PathVariable String type, @RequestBody Map<String, Object> body, Principal principal) {
        Application a = new Application();
        a.setType(ApplicationType.valueOf(type.toUpperCase()));
        Long studentId = body.get("studentId") == null ? null : Long.parseLong(body.get("studentId").toString());
        if (studentId == null && principal != null) {
            studentId = userRepository.findByUsername(principal.getName())
                    .map(User::getId).orElse(null);
        }
        a.setStudentId(studentId);
        a.setTopicId(body.get("topicId") == null ? null : Long.parseLong(body.get("topicId").toString()));
        String payload = (String) body.getOrDefault("payload", "");
        if (!StringUtils.hasText(payload)) {
            throw new IllegalArgumentException("申请内容不能为空");
        }
        a.setPayload(payload);
        return ApiResponse.ok(applicationService.create(a));
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN','TEACHER','STUDENT')")
    public ApiResponse<List<Application>> list(@RequestParam(value = "studentId", required = false) Long studentId,
                                  @RequestParam(value = "type", required = false) String type,
                                  @RequestParam(value = "status", required = false) String status,
                                  Principal principal) {
        if (studentId == null && principal != null) {
            studentId = userRepository.findByUsername(principal.getName())
                    .filter(u -> "STUDENT".equalsIgnoreCase(u.getRole()))
                    .map(User::getId).orElse(null);
        }
        ApplicationType at = type == null ? null : ApplicationType.valueOf(type.toUpperCase());
        Application.ApplicationStatus st = status == null ? null : Application.ApplicationStatus.valueOf(status.toUpperCase());
        return ApiResponse.ok(applicationService.list(studentId, at, st));
    }

    @GetMapping("/page")
    @PreAuthorize("hasAnyRole('ADMIN','TEACHER','STUDENT')")
    public ApiResponse<Page<Application>> listPage(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String type,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) Long studentId,
            Principal principal) {
        // 学生只能查看自己的申请
        if (principal != null) {
            User user = userRepository.findByUsername(principal.getName()).orElse(null);
            if (user != null && "STUDENT".equalsIgnoreCase(user.getRole())) {
                studentId = user.getId();
            }
        }
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        ApplicationType appType = null;
        if (type != null && !type.trim().isEmpty()) {
            try {
                appType = ApplicationType.valueOf(type.toUpperCase());
            } catch (IllegalArgumentException e) {
                // 忽略无效的类型值
            }
        }
        Application.ApplicationStatus appStatus = null;
        if (status != null && !status.trim().isEmpty()) {
            try {
                appStatus = Application.ApplicationStatus.valueOf(status.toUpperCase());
            } catch (IllegalArgumentException e) {
                // 忽略无效的状态值
            }
        }
        return ApiResponse.ok(applicationService.findApplications(pageable, keyword, appType, appStatus, studentId));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','TEACHER','STUDENT')")
    public ApiResponse<Application> get(@PathVariable Long id, Principal principal) {
        Application app = applicationService.getById(id);
        // 学生只能查看自己的申请
        if (principal != null) {
            User user = userRepository.findByUsername(principal.getName()).orElse(null);
            if (user != null && "STUDENT".equalsIgnoreCase(user.getRole()) && !user.getId().equals(app.getStudentId())) {
                throw new SecurityException("无权查看他人申请");
            }
        }
        return ApiResponse.ok(app);
    }

    @PostMapping("/{id}/review")
    @PreAuthorize("hasAnyRole('ADMIN','TEACHER')")
    public ApiResponse<Application> review(@PathVariable Long id, @RequestBody Map<String, String> body, Principal principal) {
        String decisionStr = body.getOrDefault("decision", "APPROVED");
        String comment = body.getOrDefault("comment", "");
        if (comment == null || comment.trim().isEmpty()) {
            throw new IllegalArgumentException("审批意见不能为空");
        }
        Application.ApplicationStatus decision = Application.ApplicationStatus.valueOf(decisionStr);
        Long actorId = null;
        if (principal != null) {
            actorId = userRepository.findByUsername(principal.getName()).map(User::getId).orElse(null);
        }
        return ApiResponse.ok(applicationService.review(id, actorId, decision, comment));
    }

    @GetMapping("/{id}/logs")
    public ApiResponse<List<ApplicationLog>> getLogs(@PathVariable Long id) {
        return ApiResponse.ok(applicationService.getLogs(id));
    }

    @PostMapping("/{id}/withdraw")
    @PreAuthorize("hasRole('STUDENT')")
    public ApiResponse<Application> withdraw(@PathVariable Long id, Principal principal) {
        Long studentId = null;
        if (principal != null) {
            studentId = userRepository.findByUsername(principal.getName()).map(User::getId).orElse(null);
        }
        return ApiResponse.ok(applicationService.withdraw(id, studentId));
    }

    @PostMapping("/{id}/resubmit")
    @PreAuthorize("hasRole('STUDENT')")
    public ApiResponse<Application> resubmit(@PathVariable Long id, @RequestBody Map<String, String> body, Principal principal) {
        Long studentId = null;
        if (principal != null) {
            studentId = userRepository.findByUsername(principal.getName()).map(User::getId).orElse(null);
        }
        String payload = body.getOrDefault("payload", "");
        if (payload == null || payload.trim().isEmpty()) {
            throw new IllegalArgumentException("申请内容不能为空");
        }
        return ApiResponse.ok(applicationService.resubmit(id, studentId, payload));
    }
}

