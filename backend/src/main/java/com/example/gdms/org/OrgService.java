package com.example.gdms.org;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import jakarta.persistence.criteria.Predicate;
import java.util.ArrayList;
import java.util.List;

@Service
public class OrgService {
    private final OrgRepository orgRepository;

    public OrgService(OrgRepository orgRepository) {
        this.orgRepository = orgRepository;
    }

    public Page<Org> findOrgs(Pageable pageable, String keyword, String type, Long parentId) {
        Specification<Org> spec = (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            
            if (keyword != null && !keyword.trim().isEmpty()) {
                String pattern = "%" + keyword.trim() + "%";
                predicates.add(cb.like(root.get("name"), pattern));
            }
            
            if (type != null && !type.trim().isEmpty()) {
                predicates.add(cb.equal(root.get("type"), type));
            }
            
            if (parentId != null) {
                predicates.add(cb.equal(root.get("parentId"), parentId));
            }
            
            return cb.and(predicates.toArray(new Predicate[0]));
        };
        
        return orgRepository.findAll(spec, pageable);
    }

    public List<Org> getTree() {
        // 返回所有组织，让前端构建树形结构
        return orgRepository.findAll();
    }

    @Transactional
    public void deleteOrg(Long id) {
        if (orgRepository.existsByParentId(id)) {
            throw new IllegalArgumentException("该组织下存在子组织，无法删除");
        }
        orgRepository.deleteById(id);
    }

    @Transactional
    public void batchDeleteOrgs(List<Long> ids) {
        if (ids == null || ids.isEmpty()) {
            throw new IllegalArgumentException("组织ID列表不能为空");
        }
        for (Long id : ids) {
            if (orgRepository.existsByParentId(id)) {
                throw new IllegalArgumentException("组织ID " + id + " 下存在子组织，无法删除");
            }
        }
        orgRepository.deleteAllById(ids);
    }
}

