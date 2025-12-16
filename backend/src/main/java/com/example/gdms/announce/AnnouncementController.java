package com.example.gdms.announce;

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
@RequestMapping("/api/announcements")
public class AnnouncementController {
    private final AnnouncementService service;
    private final UserRepository userRepository;

    public AnnouncementController(AnnouncementService service, UserRepository userRepository) {
        this.service = service;
        this.userRepository = userRepository;
    }

    @GetMapping
    public ApiResponse<List<Announcement>> list() {
        return ApiResponse.ok(service.listPublished());
    }

    @GetMapping("/page")
    public ApiResponse<Page<Announcement>> listPage(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String status   ,
            @RequestParam(required = false) Long createdBy,
            Principal principal) {
        // 安全修复：非管理员和教师只能查看已发布的公告，且不能使用createdBy参数
        boolean isAdminOrTeacher = false;
        Long currentUserId = null;
        
        if (principal != null) {
            User user = userRepository.findByUsername(principal.getName()).orElse(null);
            if (user != null) {
                isAdminOrTeacher = "ADMIN".equalsIgnoreCase(user.getRole()) || "TEACHER".equalsIgnoreCase(user.getRole());
                currentUserId = user.getId();
            }
        }
        
        // 非管理员和教师只能查看已发布的公告
        if (!isAdminOrTeacher) {
            status = "PUBLISHED";
            // 安全修复：防止IDOR漏洞，非管理员/教师不能使用createdBy参数查询其他用户的公告
            createdBy = null;
        } else {
            // 安全修复：教师只能查看自己创建的公告（除非是管理员）
            if (currentUserId != null && principal != null) {
                User user = userRepository.findByUsername(principal.getName()).orElse(null);
                if (user != null && !"ADMIN".equalsIgnoreCase(user.getRole())) {
                    createdBy = currentUserId;
                }
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
        return ApiResponse.ok(service.findAnnouncements(pageable, keyword, status, createdBy));
    }

    @GetMapping("/drafts")
    @PreAuthorize("hasAnyRole('ADMIN','TEACHER')")
    public ApiResponse<List<Announcement>> listDrafts() {
        return ApiResponse.ok(service.listDrafts());
    }

    @GetMapping("/published")
    public ApiResponse<List<Announcement>> listPublished() {
        return ApiResponse.ok(service.listPublished());
    }

    @GetMapping("/{id}")
    public ApiResponse<Announcement> get(@PathVariable Long id, Principal principal) {
        // 安全修复：改进异常处理，防止信息泄露
        Announcement announcement;
        try {
            announcement = service.getById(id);
        } catch (Exception e) {
            throw new IllegalArgumentException("公告不存在");
        }
        
        // 非管理员和教师只能查看已发布的公告
        boolean isAdminOrTeacher = false;
        if (principal != null) {
            User user = userRepository.findByUsername(principal.getName()).orElse(null);
            if (user != null) {
                isAdminOrTeacher = "ADMIN".equalsIgnoreCase(user.getRole()) || "TEACHER".equalsIgnoreCase(user.getRole());
            }
        }
        
        if (!isAdminOrTeacher && !"PUBLISHED".equals(announcement.getStatus())) {
            throw new SecurityException("无权查看未发布的公告");
        }
        
        return ApiResponse.ok(announcement);
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN','TEACHER')")
    public ApiResponse<Announcement> create(@RequestBody Announcement a, Principal principal) {
        // 安全修复：确保principal不为null（虽然@PreAuthorize应该保证这一点，但双重检查更安全）
        if (principal == null) {
            throw new SecurityException("需要登录才能创建公告");
        }
        
        // 安全修复：忽略请求体中的createdBy，防止伪造创建者
        User user = userRepository.findByUsername(principal.getName()).orElse(null);
        if (user == null) {
            throw new SecurityException("无法识别用户身份");
        }
        a.setCreatedBy(user.getId());
        
        return ApiResponse.ok(service.create(a));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','TEACHER')")
    public ApiResponse<Announcement> update(@PathVariable Long id, @RequestBody Announcement a, Principal principal) {
        // 安全修复：确保principal不为null
        if (principal == null) {
            throw new SecurityException("需要登录才能修改公告");
        }
        
        Announcement existing;
        try {
            existing = service.getById(id);
        } catch (Exception e) {
            throw new IllegalArgumentException("公告不存在");
        }
        
        // 检查权限：只有创建者或管理员可以编辑
        User currentUser = userRepository.findByUsername(principal.getName()).orElse(null);
        if (currentUser == null) {
            throw new SecurityException("无法识别用户身份");
        }
        Long currentUserId = currentUser.getId();
        boolean isAdmin = "ADMIN".equals(currentUser.getRole());
        
        if (!isAdmin && (existing.getCreatedBy() == null || !existing.getCreatedBy().equals(currentUserId))) {
            throw new SecurityException("只能编辑自己创建的公告");
        }
        
        // 已发布的公告只能由管理员修改，且不能修改为草稿状态
        if ("PUBLISHED".equals(existing.getStatus()) && !isAdmin) {
            throw new IllegalStateException("已发布的公告只能由管理员修改");
        }
        
        // 安全修复：防止修改createdBy字段
        a.setCreatedBy(null);
        
        return ApiResponse.ok(service.update(id, a, existing));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','TEACHER')")
    public ApiResponse<?> delete(@PathVariable Long id, Principal principal) {
        // 安全修复：确保principal不为null
        if (principal == null) {
            throw new SecurityException("需要登录才能删除公告");
        }
        
        Announcement existing;
        try {
            existing = service.getById(id);
        } catch (Exception e) {
            throw new IllegalArgumentException("公告不存在");
        }
        
        // 检查权限：只有创建者或管理员可以删除
        User currentUser = userRepository.findByUsername(principal.getName()).orElse(null);
        if (currentUser == null) {
            throw new SecurityException("无法识别用户身份");
        }
        Long currentUserId = currentUser.getId();
        boolean isAdmin = "ADMIN".equals(currentUser.getRole());
        
        if (!isAdmin && (existing.getCreatedBy() == null || !existing.getCreatedBy().equals(currentUserId))) {
            throw new SecurityException("只能删除自己创建的公告");
        }
        
        service.delete(id);
        return ApiResponse.ok();
    }

    @PostMapping("/{id}/publish")
    @PreAuthorize("hasAnyRole('ADMIN','TEACHER')")
    public ApiResponse<Announcement> publish(@PathVariable Long id, Principal principal) {
        // 安全修复：确保principal不为null
        if (principal == null) {
            throw new SecurityException("需要登录才能发布公告");
        }
        
        Announcement existing;
        try {
            existing = service.getById(id);
        } catch (Exception e) {
            throw new IllegalArgumentException("公告不存在");
        }
        
        // 检查权限：只有创建者或管理员可以发布
        User currentUser = userRepository.findByUsername(principal.getName()).orElse(null);
        if (currentUser == null) {
            throw new SecurityException("无法识别用户身份");
        }
        boolean isAdmin = "ADMIN".equals(currentUser.getRole());
        Long currentUserId = currentUser.getId();
        
        if (!isAdmin && (existing.getCreatedBy() == null || !existing.getCreatedBy().equals(currentUserId))) {
            throw new SecurityException("只能发布自己创建的公告");
        }
        
        return ApiResponse.ok(service.publish(id));
    }

    @GetMapping("/{id}/stats")
    @PreAuthorize("hasAnyRole('ADMIN','TEACHER')")
    public ApiResponse<Map<String, Object>> stats(@PathVariable Long id, Principal principal) {
        // 安全修复：确保principal不为null
        if (principal == null) {
            throw new SecurityException("需要登录才能查看统计");
        }
        
        Announcement existing;
        try {
            existing = service.getById(id);
        } catch (Exception e) {
            throw new IllegalArgumentException("公告不存在");
        }
        
        // 检查权限：只有创建者或管理员可以查看统计
        User currentUser = userRepository.findByUsername(principal.getName()).orElse(null);
        if (currentUser == null) {
            throw new SecurityException("无法识别用户身份");
        }
        boolean isAdmin = "ADMIN".equals(currentUser.getRole());
        Long currentUserId = currentUser.getId();
        
        if (!isAdmin && (existing.getCreatedBy() == null || !existing.getCreatedBy().equals(currentUserId))) {
            throw new SecurityException("只能查看自己创建的公告统计");
        }
        
        return ApiResponse.ok(service.readStats(id));
    }

    @PostMapping("/{id}/read")
    public ApiResponse<AnnouncementRead> markRead(@PathVariable Long id, @RequestBody Map<String, Long> body, Principal principal) {
        // 安全修复：忽略body中的userId，只使用当前登录用户的ID，防止伪造阅读记录
        Long userId = null;
        if (principal != null) {
            userId = userRepository.findByUsername(principal.getName()).map(u -> u.getId()).orElse(null);
        }
        if (userId == null) {
            throw new SecurityException("无法识别用户，请先登录");
        }
        
        // 验证公告是否存在且已发布
        Announcement announcement = service.getById(id);
        if (!"PUBLISHED".equals(announcement.getStatus())) {
            throw new IllegalStateException("只能标记已发布公告为已读");
        }
        
        AnnouncementRead read = service.markRead(id, userId);
        return ApiResponse.ok(read);
    }

    // 管理员功能：批量删除公告
    @DeleteMapping("/batch")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<?> batchDelete(@RequestBody Map<String, List<Long>> body, Principal principal) {
        if (principal == null) {
            throw new SecurityException("需要登录才能执行批量操作");
        }
        List<Long> ids = body.get("ids");
        service.batchDelete(ids);
        return ApiResponse.ok();
    }

    // 管理员功能：批量发布公告
    @PostMapping("/batch/publish")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<Map<String, Object>> batchPublish(@RequestBody Map<String, List<Long>> body, Principal principal) {
        if (principal == null) {
            throw new SecurityException("需要登录才能执行批量操作");
        }
        List<Long> ids = body.get("ids");
        int count = service.batchPublish(ids);
        return ApiResponse.ok(Map.of("successCount", count, "totalCount", ids != null ? ids.size() : 0));
    }

    // 管理员功能：获取公告统计信息
    @GetMapping("/statistics")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<Map<String, Object>> statistics(Principal principal) {
        if (principal == null) {
            throw new SecurityException("需要登录才能查看统计信息");
        }
        return ApiResponse.ok(service.getStatistics());
    }
}

