package com.example.gdms.group;

import com.example.gdms.common.ApiResponse;
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
@RequestMapping("/api/defense")
public class DefenseController {
    private final DefenseService defenseService;

    public DefenseController(DefenseService defenseService) {
        this.defenseService = defenseService;
    }

    @GetMapping("/groups")
    public ApiResponse<List<DefenseGroup>> groups() {
        return ApiResponse.ok(defenseService.listGroups());
    }

    @GetMapping("/groups/page")
    @PreAuthorize("hasAnyRole('ADMIN','TEACHER')")
    public ApiResponse<Page<DefenseGroup>> groupsPage(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String type) {
        // 安全修复：限制分页大小，防止DoS攻击
        if (size > 100) {
            size = 100;
        }
        if (size < 1) {
            size = 10;
        }
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.ASC, "name"));
        return ApiResponse.ok(defenseService.findGroups(pageable, keyword, type));
    }

    @PostMapping("/groups")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<DefenseGroup> create(@RequestBody DefenseGroup g) {
        return ApiResponse.ok(defenseService.createGroup(g));
    }

    @GetMapping("/groups/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','TEACHER')")
    public ApiResponse<DefenseGroup> getGroup(@PathVariable Long id) {
        return ApiResponse.ok(defenseService.getGroupById(id));
    }

    @PutMapping("/groups/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<DefenseGroup> updateGroup(@PathVariable Long id, @RequestBody DefenseGroup g) {
        return ApiResponse.ok(defenseService.updateGroup(id, g));
    }

    @DeleteMapping("/groups/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<?> deleteGroup(@PathVariable Long id) {
        defenseService.deleteGroup(id);
        return ApiResponse.ok();
    }

    @PostMapping("/groups/auto")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<List<GroupMember>> autoAssign(@RequestBody Map<String, Object> body, Principal principal) {
        // 安全修复：确保principal不为null
        if (principal == null) {
            throw new SecurityException("需要登录才能执行自动分配");
        }
        
        String type = (String) body.getOrDefault("type", "FINAL");
        int capacity = 8;
        if (body.get("capacity") != null) {
            try {
                capacity = Integer.parseInt(body.get("capacity").toString());
            } catch (NumberFormatException e) {
                throw new IllegalArgumentException("无效的容量参数");
            }
        }
        return ApiResponse.ok(defenseService.autoAssign(type, capacity));
    }

    @GetMapping("/groups/{groupId}/members")
    public ApiResponse<List<GroupMember>> members(@PathVariable Long groupId) {
        return ApiResponse.ok(defenseService.members(groupId));
    }

    @GetMapping("/groups/{groupId}/detail")
    @PreAuthorize("hasAnyRole('ADMIN','TEACHER')")
    public ApiResponse<Map<String, Object>> getGroupDetail(@PathVariable Long groupId) {
        return ApiResponse.ok(defenseService.getGroupDetail(groupId));
    }

    @GetMapping("/groups/{groupId}/members/detail")
    @PreAuthorize("hasAnyRole('ADMIN','TEACHER')")
    public ApiResponse<List<Map<String, Object>>> getMembersWithDetails(@PathVariable Long groupId) {
        return ApiResponse.ok(defenseService.getMembersWithDetails(groupId));
    }

    @PostMapping("/reviews")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<ReviewAssignment> assignReview(@RequestBody Map<String, Object> body, Principal principal) {
        // 安全修复：确保principal不为null
        if (principal == null) {
            throw new SecurityException("需要登录才能分配评阅任务");
        }
        
        Long reviewerId = null;
        Long studentId = null;
        Long topicId = null;
        
        try {
            if (body.get("reviewerId") != null) {
                reviewerId = Long.parseLong(body.get("reviewerId").toString());
            }
            if (body.get("studentId") != null) {
                studentId = Long.parseLong(body.get("studentId").toString());
            }
            if (body.get("topicId") != null) {
                topicId = Long.parseLong(body.get("topicId").toString());
            }
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("无效的ID参数");
        }
        
        String type = (String) body.getOrDefault("type", "CROSS");
        return ApiResponse.ok(defenseService.assignReview(reviewerId, studentId, topicId, type));
    }

    @PostMapping("/reviews/{id}/complete")
    @PreAuthorize("hasAnyRole('ADMIN','TEACHER')")
    public ApiResponse<ReviewAssignment> completeReview(@PathVariable Long id, @RequestBody Map<String, String> body, Principal principal) {
        // 安全修复：确保principal不为null
        if (principal == null) {
            throw new SecurityException("需要登录才能完成评阅");
        }
        
        String comment = body.getOrDefault("comment", "");
        Double score = null;
        if (body.get("score") != null && !body.get("score").toString().trim().isEmpty()) {
            try {
                score = Double.parseDouble(body.get("score").toString());
            } catch (NumberFormatException e) {
                throw new IllegalArgumentException("无效的分数格式");
            }
        }
        
        Long currentId = currentUserId(principal);
        if (currentId == null) {
            throw new SecurityException("无法识别用户身份");
        }
        
        return ApiResponse.ok(defenseService.completeReview(id, comment, score, currentId, isAdmin(principal)));
    }

    @PostMapping("/scores")
    @PreAuthorize("hasAnyRole('ADMIN','TEACHER')")
    public ApiResponse<DefenseScore> score(@RequestBody Map<String, Object> body, Principal principal) {
        // 安全修复：确保principal不为null
        if (principal == null) {
            throw new SecurityException("需要登录才能记录分数");
        }
        
        Long groupId = null;
        Long studentId = null;
        Double score = null;
        
        try {
            if (body.get("groupId") != null) {
                groupId = Long.parseLong(body.get("groupId").toString());
            }
            if (body.get("studentId") != null) {
                studentId = Long.parseLong(body.get("studentId").toString());
            }
            if (body.get("score") != null) {
                score = Double.parseDouble(body.get("score").toString());
            }
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("无效的参数格式");
        }
        
        String comment = (String) body.getOrDefault("comment", "");
        return ApiResponse.ok(defenseService.recordScore(groupId, studentId, score, comment));
    }

    @GetMapping("/scores/{groupId}")
    public ApiResponse<List<DefenseScore>> groupScores(@PathVariable Long groupId) {
        return ApiResponse.ok(defenseService.scoresByGroup(groupId));
    }

    @GetMapping("/grades")
    @PreAuthorize("hasAnyRole('ADMIN','TEACHER')")
    public ApiResponse<List<Map<String, Object>>> grades() {
        return ApiResponse.ok(defenseService.gradeSummary());
    }

    @PostMapping("/groups/{groupId}/members")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<GroupMember> addMember(@PathVariable Long groupId, @RequestBody Map<String, Object> body, Principal principal) {
        // 安全修复：确保principal不为null
        if (principal == null) {
            throw new SecurityException("需要登录才能添加成员");
        }
        
        Long studentId = null;
        Long topicId = null;
        
        try {
            if (body.get("studentId") != null) {
                studentId = Long.parseLong(body.get("studentId").toString());
            }
            if (body.get("topicId") != null) {
                topicId = Long.parseLong(body.get("topicId").toString());
            }
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("无效的ID参数");
        }
        
        return ApiResponse.ok(defenseService.addMember(groupId, studentId, topicId));
    }

    @DeleteMapping("/groups/{groupId}/members/{memberId}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> removeMember(@PathVariable Long groupId, @PathVariable Long memberId) {
        defenseService.removeMember(memberId);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/reviews/auto-cross")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<List<ReviewAssignment>> autoCrossReview(Principal principal) {
        // 安全修复：确保principal不为null
        if (principal == null) {
            throw new SecurityException("需要登录才能执行自动生成交叉评阅");
        }
        return ApiResponse.ok(defenseService.autoCrossReview());
    }

    @GetMapping("/reviews")
    @PreAuthorize("hasAnyRole('ADMIN','TEACHER')")
    public ApiResponse<List<ReviewAssignment>> reviewerTasks(@RequestParam("reviewerId") Long reviewerId,
                                                  @RequestParam(value = "status", required = false) String status,
                                                  @RequestParam(value = "type", required = false) String type,
                                                  Principal principal) {
        if (!isAdmin(principal)) {
            Long currentId = currentUserId(principal);
            if (currentId == null || !currentId.equals(reviewerId)) {
                throw new SecurityException("无权查看他人评阅任务");
            }
        }
        return ApiResponse.ok(defenseService.reviewerTasks(reviewerId, status, type));
    }

    @GetMapping("/reviews/page")
    @PreAuthorize("hasAnyRole('ADMIN','TEACHER')")
    public ApiResponse<Page<ReviewAssignment>> reviewerTasksPage(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam("reviewerId") Long reviewerId,
            @RequestParam(value = "status", required = false) String status,
            @RequestParam(value = "type", required = false) String type,
            Principal principal) {
        // 安全修复：确保principal不为null
        if (principal == null) {
            throw new SecurityException("需要登录才能查看评阅任务");
        }
        
        // 权限检查：非管理员只能查看自己的评阅任务
        if (!isAdmin(principal)) {
            Long currentId = currentUserId(principal);
            if (currentId == null || !currentId.equals(reviewerId)) {
                throw new SecurityException("无权查看他人评阅任务");
            }
        }
        
        // 安全修复：限制分页大小，防止DoS攻击
        if (size > 100) {
            size = 100;
        }
        if (size < 1) {
            size = 10;
        }
        
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        return ApiResponse.ok(defenseService.findReviewTasks(pageable, reviewerId, status, type));
    }

    private Long currentUserId(Principal principal) {
        if (principal == null) return null;
        return defenseService.findUserId(principal.getName());
    }

    private boolean isAdmin(Principal principal) {
        return defenseService.isAdmin(principal);
    }

    @GetMapping("/my-group")
    @PreAuthorize("hasRole('STUDENT')")
    public ApiResponse<GroupMember> getMyGroup(Principal principal) {
        Long studentId = currentUserId(principal);
        if (studentId == null) {
            throw new IllegalArgumentException("无法识别学生身份");
        }
        return ApiResponse.ok(defenseService.getStudentGroup(studentId).orElse(null));
    }

    @GetMapping("/my-scores")
    @PreAuthorize("hasRole('STUDENT')")
    public ApiResponse<List<DefenseScore>> getMyScores(Principal principal) {
        Long studentId = currentUserId(principal);
        if (studentId == null) {
            throw new IllegalArgumentException("无法识别学生身份");
        }
        return ApiResponse.ok(defenseService.getStudentScores(studentId));
    }
}

