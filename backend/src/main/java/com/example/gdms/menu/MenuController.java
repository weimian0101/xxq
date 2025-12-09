package com.example.gdms.menu;

import com.example.gdms.common.ApiResponse;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/menus")
public class MenuController {
    private final MenuRepository repository;

    public MenuController(MenuRepository repository) {
        this.repository = repository;
    }

    @GetMapping
    public ApiResponse<List<Menu>> list(@RequestParam("role") String role) {
        return ApiResponse.ok(repository.findByRoleOrderByOrderIndex(role));
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<Menu> create(@RequestBody Menu m) {
        return ApiResponse.ok(repository.save(m));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<Menu> update(@PathVariable Long id, @RequestBody Menu body) {
        Menu m = repository.findById(id).orElseThrow();
        if (body.getName() != null) m.setName(body.getName());
        if (body.getPath() != null) m.setPath(body.getPath());
        if (body.getRole() != null) m.setRole(body.getRole());
        if (body.getOrderIndex() != null) m.setOrderIndex(body.getOrderIndex());
        return ApiResponse.ok(repository.save(m));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<?> delete(@PathVariable Long id) {
        repository.deleteById(id);
        return ApiResponse.ok();
    }

    @PostMapping("/reorder")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<?> reorder(@RequestBody Map<String, List<Long>> body) {
        List<Long> ids = body.get("ids");
        if (ids != null) {
            for (int i = 0; i < ids.size(); i++) {
                Menu m = repository.findById(ids.get(i)).orElse(null);
                if (m != null) {
                    m.setOrderIndex(i + 1);
                    repository.save(m);
                }
            }
        }
        return ApiResponse.ok();
    }
}

