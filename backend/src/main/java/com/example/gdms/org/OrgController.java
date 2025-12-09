package com.example.gdms.org;

import com.example.gdms.common.ApiResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/orgs")
public class OrgController {
    private final OrgRepository repository;
    private final OrgService orgService;

    public OrgController(OrgRepository repository, OrgService orgService) {
        this.repository = repository;
        this.orgService = orgService;
    }

    @GetMapping
    public ApiResponse<List<Org>> list() {
        return ApiResponse.ok(repository.findAll());
    }

    @GetMapping("/tree")
    public ApiResponse<List<Org>> tree() {
        return ApiResponse.ok(orgService.getTree());
    }

    @GetMapping("/page")
    @PreAuthorize("hasAnyRole('ADMIN')")
    public ApiResponse<Page<Org>> listPage(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String type,
            @RequestParam(required = false) Long parentId) {
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.ASC, "name"));
        return ApiResponse.ok(orgService.findOrgs(pageable, keyword, type, parentId));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN')")
    public ApiResponse<Org> get(@PathVariable Long id) {
        return ApiResponse.ok(repository.findById(id).orElseThrow());
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<Org> create(@RequestBody Org org) {
        if (org.getName() == null || org.getName().trim().isEmpty()) {
            throw new IllegalArgumentException("组织名称不能为空");
        }
        if (repository.findByName(org.getName()).isPresent()) {
            throw new IllegalArgumentException("组织名称已存在");
        }
        // 验证父组织是否存在
        if (org.getParentId() != null && !repository.existsById(org.getParentId())) {
            throw new IllegalArgumentException("父组织不存在");
        }
        // 防止自己作为自己的父组织
        if (org.getId() != null && org.getId().equals(org.getParentId())) {
            throw new IllegalArgumentException("组织不能作为自己的父组织");
        }
        return ApiResponse.ok(repository.save(org));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<Org> update(@PathVariable Long id, @RequestBody Org body) {
        Org o = repository.findById(id).orElseThrow();
        
        // 防止自己作为自己的父组织
        if (body.getParentId() != null && body.getParentId().equals(id)) {
            throw new IllegalArgumentException("组织不能作为自己的父组织");
        }
        
        // 防止循环引用（简单检查：不能将父组织设置为自己的子组织）
        if (body.getParentId() != null) {
            Org parent = repository.findById(body.getParentId()).orElseThrow();
            if (parent.getParentId() != null && parent.getParentId().equals(id)) {
                throw new IllegalArgumentException("不能创建循环的组织层级关系");
            }
        }
        
        if (body.getName() != null && !body.getName().trim().isEmpty() && !body.getName().equals(o.getName())) {
            if (repository.findByName(body.getName()).isPresent()) {
                throw new IllegalArgumentException("组织名称已存在");
            }
            o.setName(body.getName());
        }
        if (body.getParentId() != null) {
            // 验证父组织是否存在
            if (!repository.existsById(body.getParentId())) {
                throw new IllegalArgumentException("父组织不存在");
            }
            o.setParentId(body.getParentId());
        } else if (body.getParentId() == null && o.getParentId() != null) {
            // 允许清除父组织
            o.setParentId(null);
        }
        if (body.getType() != null) o.setType(body.getType());
        return ApiResponse.ok(repository.save(o));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<?> delete(@PathVariable Long id) {
        orgService.deleteOrg(id);
        return ApiResponse.ok();
    }

    @DeleteMapping("/batch")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<?> batchDelete(@RequestBody BatchDeleteRequest req) {
        orgService.batchDeleteOrgs(req.ids());
        return ApiResponse.ok();
    }

    public record BatchDeleteRequest(List<Long> ids) {}
}

