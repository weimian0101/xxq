package com.example.gdms.topic;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import jakarta.persistence.criteria.Predicate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Service
public class TopicService {
    private final TopicRepository topicRepository;
    private final TopicApprovalRepository approvalRepository;
    private final StudentSelectionRepository selectionRepository;

    public TopicService(TopicRepository topicRepository, TopicApprovalRepository approvalRepository, StudentSelectionRepository selectionRepository) {
        this.topicRepository = topicRepository;
        this.approvalRepository = approvalRepository;
        this.selectionRepository = selectionRepository;
    }

    public List<Topic> list() {
        return topicRepository.findAll();
    }

    public Page<Topic> findTopics(Pageable pageable, String keyword, TopicStatus status, Long creatorId) {
        Specification<Topic> spec = (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            
            if (keyword != null && !keyword.trim().isEmpty()) {
                String pattern = "%" + keyword.trim() + "%";
                Predicate titlePred = cb.like(root.get("title"), pattern);
                Predicate descPred = cb.like(root.get("description"), pattern);
                predicates.add(cb.or(titlePred, descPred));
            }
            
            if (status != null) {
                predicates.add(cb.equal(root.get("status"), status));
            }
            
            if (creatorId != null) {
                predicates.add(cb.equal(root.get("creatorId"), creatorId));
            }
            
            return cb.and(predicates.toArray(new Predicate[0]));
        };
        
        return topicRepository.findAll(spec, pageable);
    }

    public List<TopicApproval> approvals(Long topicId) {
        return approvalRepository.findByTopicId(topicId);
    }

    public Topic create(Topic t) {
        if (t.getTitle() == null || t.getTitle().trim().isEmpty()) {
            throw new IllegalArgumentException("课题标题不能为空");
        }
        t.setStatus(TopicStatus.DRAFT);
        if (t.getCapacity() == null || t.getCapacity() < 1) {
            t.setCapacity(1);
        }
        return topicRepository.save(t);
    }

    public Topic update(Long id, Topic t) {
        Topic existing = topicRepository.findById(id).orElseThrow();
        if (t.getTitle() != null && !t.getTitle().trim().isEmpty()) {
            existing.setTitle(t.getTitle());
        }
        if (t.getDescription() != null) {
            existing.setDescription(t.getDescription());
        }
        if (t.getCapacity() != null && t.getCapacity() > 0) {
            existing.setCapacity(t.getCapacity());
        }
        return topicRepository.save(existing);
    }

    @Transactional
    public void delete(Long id) {
        // 检查是否有学生已选择该课题
        List<StudentSelection> selections = selectionRepository.findByTopicId(id);
        long activeSelections = selections.stream()
                .filter(s -> s.getStatus() == StudentSelection.SelectionStatus.SELECTED
                        || s.getStatus() == StudentSelection.SelectionStatus.LOCKED)
                .count();
        if (activeSelections > 0) {
            throw new IllegalStateException("该课题已有学生选择，无法删除");
        }
        topicRepository.deleteById(id);
    }

    public Topic submit(Long id) {
        Topic t = topicRepository.findById(id).orElseThrow();
        t.setStatus(TopicStatus.SUBMITTED);
        return topicRepository.save(t);
    }

    @Transactional
    public Topic approve(Long topicId, Long reviewerId, TopicStatus decision, String comment) {
        Topic t = topicRepository.findById(topicId).orElseThrow();
        t.setStatus(decision);
        TopicApproval approval = new TopicApproval();
        approval.setTopicId(topicId);
        approval.setReviewerId(reviewerId);
        approval.setDecision(decision);
        approval.setComment(comment);
        approvalRepository.save(approval);
        return topicRepository.save(t);
    }

    @Transactional
    public StudentSelection select(Long topicId, Long studentId) {
        if (studentId == null) {
            throw new IllegalArgumentException("缺少学生标识");
        }
        Topic t = topicRepository.findById(topicId).orElseThrow();
        if (selectionRepository.existsByStudentIdAndStatus(studentId, StudentSelection.SelectionStatus.SELECTED)) {
            throw new IllegalStateException("学生已有选题");
        }
        List<StudentSelection> selections = selectionRepository.findByTopicId(topicId);
        long active = selections.stream()
                .filter(s -> s.getStatus() == StudentSelection.SelectionStatus.SELECTED
                        || s.getStatus() == StudentSelection.SelectionStatus.LOCKED)
                .count();
        if (active >= t.getCapacity()) {
            throw new IllegalStateException("课题容量已满");
        }
        StudentSelection s = new StudentSelection();
        s.setTopicId(topicId);
        s.setStudentId(studentId);
        s.setStatus(StudentSelection.SelectionStatus.SELECTED);
        return selectionRepository.save(s);
    }

    @Transactional
    public StudentSelection lockSelection(Long selectionId) {
        StudentSelection s = selectionRepository.findById(selectionId).orElseThrow();
        s.setStatus(StudentSelection.SelectionStatus.LOCKED);
        return selectionRepository.save(s);
    }

    @Transactional
    public StudentSelection cancelSelection(Long selectionId, Long studentId) {
        StudentSelection s = selectionRepository.findById(selectionId).orElseThrow();
        if (studentId != null && !s.getStudentId().equals(studentId)) {
            throw new IllegalStateException("只能取消自己的选题");
        }
        s.setStatus(StudentSelection.SelectionStatus.CANCELLED);
        return selectionRepository.save(s);
    }

    public List<StudentSelection> getSelections(Long topicId) {
        return selectionRepository.findByTopicId(topicId);
    }

    public Topic getById(Long id) {
        return topicRepository.findById(id).orElseThrow();
    }

    public List<StudentSelection> getStudentSelections(Long studentId) {
        return selectionRepository.findByStudentId(studentId);
    }

    public Optional<StudentSelection> getActiveSelection(Long studentId) {
        Optional<StudentSelection> selected = selectionRepository.findByStudentIdAndStatus(studentId, StudentSelection.SelectionStatus.SELECTED);
        if (selected.isPresent()) {
            return selected;
        }
        return selectionRepository.findByStudentIdAndStatus(studentId, StudentSelection.SelectionStatus.LOCKED);
    }

    public List<Map<String, Object>> getTeacherStudents(Long teacherId) {
        // 获取教师创建的所有课题
        List<Topic> teacherTopics = topicRepository.findByCreatorId(teacherId);
        List<Map<String, Object>> result = new ArrayList<>();
        
        for (Topic topic : teacherTopics) {
            List<StudentSelection> selections = selectionRepository.findByTopicId(topic.getId());
            for (StudentSelection selection : selections) {
                if (selection.getStatus() == StudentSelection.SelectionStatus.SELECTED 
                        || selection.getStatus() == StudentSelection.SelectionStatus.LOCKED) {
                    Map<String, Object> item = new java.util.HashMap<>();
                    item.put("studentId", selection.getStudentId());
                    item.put("topicId", topic.getId());
                    item.put("topicTitle", topic.getTitle());
                    item.put("selectionId", selection.getId());
                    item.put("selectionStatus", selection.getStatus());
                    item.put("createdAt", selection.getCreatedAt());
                    result.add(item);
                }
            }
        }
        
        return result;
    }
}

