package com.example.gdms.announce;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import jakarta.persistence.criteria.Predicate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Service
public class AnnouncementService {
    private final AnnouncementRepository repository;
    private final AnnouncementReadRepository readRepository;

    public AnnouncementService(AnnouncementRepository repository, AnnouncementReadRepository readRepository) {
        this.repository = repository;
        this.readRepository = readRepository;
    }

    public Announcement create(Announcement a) {
        // 安全修复：增强输入验证，防止DoS攻击
        if (a.getTitle() == null || a.getTitle().trim().isEmpty()) {
            throw new IllegalArgumentException("公告标题不能为空");
        }
        String title = a.getTitle().trim();
        if (title.length() > 200) {
            throw new IllegalArgumentException("公告标题长度不能超过200个字符");
        }
        a.setTitle(title);
        
        if (a.getContent() == null || a.getContent().trim().isEmpty()) {
            throw new IllegalArgumentException("公告内容不能为空");
        }
        String content = a.getContent().trim();
        if (content.length() > 10000) {
            throw new IllegalArgumentException("公告内容长度不能超过10000个字符");
        }
        a.setContent(content);
        
        if (a.getStatus() == null) {
            a.setStatus("DRAFT");
        }
        // 安全修复：验证状态值，防止非法状态
        if (!"DRAFT".equals(a.getStatus()) && !"PUBLISHED".equals(a.getStatus())) {
            throw new IllegalArgumentException("无效的公告状态");
        }
        
        return repository.save(a);
    }

    public Announcement update(Long id, Announcement a, Announcement existing) {
        // 已发布的公告不允许修改状态
        if ("PUBLISHED".equals(existing.getStatus()) && a.getStatus() != null && !"PUBLISHED".equals(a.getStatus())) {
            throw new IllegalStateException("已发布的公告不能修改状态");
        }
        
        // 安全修复：增强输入验证，防止DoS攻击
        if (a.getTitle() != null && !a.getTitle().trim().isEmpty()) {
            String title = a.getTitle().trim();
            if (title.length() > 200) {
                throw new IllegalArgumentException("公告标题长度不能超过200个字符");
            }
            existing.setTitle(title);
        }
        if (a.getContent() != null) {
            String content = a.getContent().trim();
            if (content.length() > 10000) {
                throw new IllegalArgumentException("公告内容长度不能超过10000个字符");
            }
            existing.setContent(content);
        }
        // 只有草稿状态才能修改状态
        if (a.getStatus() != null && !"PUBLISHED".equals(existing.getStatus())) {
            // 安全修复：验证状态值，防止非法状态
            if (!"DRAFT".equals(a.getStatus()) && !"PUBLISHED".equals(a.getStatus())) {
                throw new IllegalArgumentException("无效的公告状态");
            }
            existing.setStatus(a.getStatus());
        }
        return repository.save(existing);
    }

    @Transactional
    public void delete(Long id) {
        // 删除公告时同时删除阅读记录
        readRepository.deleteByAnnouncementId(id);
        repository.deleteById(id);
    }

    public Announcement getById(Long id) {
        // 安全修复：改进异常处理，防止信息泄露
        if (id == null || id <= 0) {
            throw new IllegalArgumentException("无效的公告ID");
        }
        return repository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("公告不存在"));
    }

    public Page<Announcement> findAnnouncements(Pageable pageable, String keyword, String status, Long createdBy) {
        Specification<Announcement> spec = (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            
            if (keyword != null && !keyword.trim().isEmpty()) {
                String pattern = "%" + keyword.trim() + "%";
                Predicate titlePred = cb.like(root.get("title"), pattern);
                Predicate contentPred = cb.like(root.get("content"), pattern);
                predicates.add(cb.or(titlePred, contentPred));
            }
            
            if (status != null && !status.trim().isEmpty()) {
                predicates.add(cb.equal(root.get("status"), status));
            }
            
            if (createdBy != null) {
                predicates.add(cb.equal(root.get("createdBy"), createdBy));
            }
            
            return cb.and(predicates.toArray(new Predicate[0]));
        };
        
        return repository.findAll(spec, pageable);
    }

    public List<Announcement> list() {
        return repository.findAll();
    }

    public List<Announcement> listPublished() {
        return repository.findByStatusOrderByCreatedAtDesc("PUBLISHED");
    }

    public List<Announcement> listDrafts() {
        return repository.findByStatusOrderByCreatedAtDesc("DRAFT");
    }

    @Transactional
    public Announcement publish(Long id) {
        Announcement a = repository.findById(id).orElseThrow();
        a.setStatus("PUBLISHED");
        a.setPublishedAt(LocalDateTime.now());
        return repository.save(a);
    }

    @Transactional
    public AnnouncementRead markRead(Long annId, Long userId) {
        if (readRepository.existsByAnnouncementIdAndUserId(annId, userId)) {
            return null;
        }
        AnnouncementRead r = new AnnouncementRead();
        r.setAnnouncementId(annId);
        r.setUserId(userId);
        return readRepository.save(r);
    }

    public Map<String, Object> readStats(Long annId) {
        long totalRead = readRepository.countByAnnouncementId(annId);
        Announcement ann = repository.findById(annId).orElse(null);
        return Map.of(
                "announcementId", annId,
                "totalRead", totalRead,
                "title", ann != null ? ann.getTitle() : ""
        );
    }

    // 管理员功能：批量删除公告
    @Transactional
    public void batchDelete(List<Long> ids) {
        if (ids == null || ids.isEmpty()) {
            throw new IllegalArgumentException("请选择要删除的公告");
        }
        for (Long id : ids) {
            if (id != null && id > 0) {
                delete(id);
            }
        }
    }

    // 管理员功能：批量发布公告
    @Transactional
    public int batchPublish(List<Long> ids) {
        if (ids == null || ids.isEmpty()) {
            throw new IllegalArgumentException("请选择要发布的公告");
        }
        int count = 0;
        for (Long id : ids) {
            if (id != null && id > 0) {
                try {
                    Announcement ann = repository.findById(id).orElse(null);
                    if (ann != null && !"PUBLISHED".equals(ann.getStatus())) {
                        publish(id);
                        count++;
                    }
                } catch (Exception e) {
                    // 忽略单个失败，继续处理其他
                }
            }
        }
        return count;
    }

    // 管理员功能：获取公告统计信息
    public Map<String, Object> getStatistics() {
        long totalCount = repository.count();
        long publishedCount = repository.countByStatus("PUBLISHED");
        long draftCount = repository.countByStatus("DRAFT");
        long totalReadCount = readRepository.count();
        
        return Map.of(
                "totalCount", totalCount,
                "publishedCount", publishedCount,
                "draftCount", draftCount,
                "totalReadCount", totalReadCount
        );
    }
}

