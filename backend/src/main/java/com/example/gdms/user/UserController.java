package com.example.gdms.user;

import com.example.gdms.common.ApiResponse;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/users")
public class UserController {
    private final UserService userService;
    private final UserRepository userRepository;

    public UserController(UserService userService, UserRepository userRepository) {
        this.userService = userService;
        this.userRepository = userRepository;
    }

    @GetMapping
    public ApiResponse<List<User>> list() {
        return ApiResponse.ok(userRepository.findAll());
    }

    @GetMapping("/page")
    @PreAuthorize("hasAnyRole('ADMIN')")
    public ApiResponse<Page<User>> listPage(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String role,
            @RequestParam(required = false) Long orgId,
            @RequestParam(required = false) Boolean enabled) {
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        return ApiResponse.ok(userService.findUsers(pageable, keyword, role, orgId, enabled));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN')")
    public ApiResponse<User> get(@PathVariable Long id) {
        return ApiResponse.ok(userRepository.findById(id).orElseThrow());
    }

    @PostMapping("/import")
    @PreAuthorize("hasAnyRole('ADMIN')")
    public ApiResponse<List<User>> importUsers(@RequestBody List<UserImportRequest> users) {
        return ApiResponse.ok(
                users.stream()
                        .map(u -> userService.createUser(u.username(), u.password(), u.role(), u.fullName()))
                        .toList()
        );
    }

    @PostMapping("/batch-role")
    @PreAuthorize("hasAnyRole('ADMIN')")
    public ApiResponse<?> batchUpdateRole(@RequestBody BatchRoleRequest req) {
        userService.batchUpdateRole(req.userIds(), req.role());
        return ApiResponse.ok();
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN')")
    public ApiResponse<User> create(@RequestBody @Valid UserCreateRequest req) {
        if (userRepository.findByUsername(req.username()).isPresent()) {
            throw new IllegalArgumentException("用户名已存在");
        }
        return ApiResponse.ok(userService.createUser(req.username(), req.password(), req.role(), req.fullName(), req.phone(), req.signatureUrl(), req.orgId(), req.enabled()));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN')")
    public ApiResponse<User> update(@PathVariable Long id, @RequestBody UserUpdateRequest req) {
        return ApiResponse.ok(userService.updateUser(id, req));
    }

    @PostMapping("/{id}/enable")
    @PreAuthorize("hasAnyRole('ADMIN')")
    public ApiResponse<User> enable(@PathVariable Long id, @RequestParam("enabled") boolean enabled) {
        return ApiResponse.ok(userService.setEnabled(id, enabled));
    }

    @PostMapping("/{id}/password")
    @PreAuthorize("hasAnyRole('ADMIN')")
    public ApiResponse<User> resetPassword(@PathVariable Long id, @RequestParam("password") String password) {
        return ApiResponse.ok(userService.resetPassword(id, password));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN')")
    public ApiResponse<?> delete(@PathVariable Long id) {
        userService.deleteUser(id);
        return ApiResponse.ok();
    }

    @DeleteMapping("/batch")
    @PreAuthorize("hasAnyRole('ADMIN')")
    public ApiResponse<?> batchDelete(@RequestBody BatchDeleteRequest req) {
        userService.batchDeleteUsers(req.userIds());
        return ApiResponse.ok();
    }

    @GetMapping("/profile/me")
    public ApiResponse<User> profile(Authentication authentication) {
        return ApiResponse.ok(userService.getByUsername(authentication.getName()));
    }

    @PutMapping("/profile/me")
    public ApiResponse<User> updateProfile(@RequestBody UserUpdateRequest req, Authentication authentication) {
        return ApiResponse.ok(userService.updateProfile(authentication.getName(), req));
    }

    public record UserImportRequest(String username, String password, String role, String fullName) {}

    public record UserCreateRequest(String username, String password, String role, String fullName, String phone, String signatureUrl, Long orgId, Boolean enabled) {}
    public record UserUpdateRequest(String fullName, String phone, String signatureUrl, String role, Long orgId, Boolean enabled) {}
    public record BatchRoleRequest(List<Long> userIds, String role) {}
    public record BatchDeleteRequest(List<Long> userIds) {}
}

