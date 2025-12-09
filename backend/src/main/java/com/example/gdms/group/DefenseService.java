package com.example.gdms.group;

import com.example.gdms.topic.StudentSelection;
import com.example.gdms.topic.StudentSelectionRepository;
import com.example.gdms.user.User;
import com.example.gdms.user.UserRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import jakarta.persistence.criteria.Predicate;
import java.security.Principal;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class DefenseService {
    private final DefenseGroupRepository groupRepository;
    private final GroupMemberRepository memberRepository;
    private final ReviewAssignmentRepository reviewRepository;
    private final DefenseScoreRepository scoreRepository;
    private final StudentSelectionRepository selectionRepository;
    private final UserRepository userRepository;

    public DefenseService(DefenseGroupRepository groupRepository,
                          GroupMemberRepository memberRepository,
                          ReviewAssignmentRepository reviewRepository,
                          DefenseScoreRepository scoreRepository,
                          StudentSelectionRepository selectionRepository,
                          UserRepository userRepository) {
        this.groupRepository = groupRepository;
        this.memberRepository = memberRepository;
        this.reviewRepository = reviewRepository;
        this.scoreRepository = scoreRepository;
        this.selectionRepository = selectionRepository;
        this.userRepository = userRepository;
    }

    public List<DefenseGroup> listGroups() {
        return groupRepository.findAll();
    }

    public DefenseGroup createGroup(DefenseGroup g) {
        // 增强输入验证
        if (g.getName() == null || g.getName().trim().isEmpty()) {
            throw new IllegalArgumentException("分组名称不能为空");
        }
        String name = g.getName().trim();
        if (name.length() > 100) {
            throw new IllegalArgumentException("分组名称长度不能超过100个字符");
        }
        g.setName(name);
        
        // 验证类型
        if (g.getType() != null && !g.getType().trim().isEmpty()) {
            String type = g.getType().trim();
            if (!"OPENING".equals(type) && !"FINAL".equals(type)) {
                throw new IllegalArgumentException("无效的分组类型，只支持OPENING或FINAL");
            }
            g.setType(type);
        }
        
        // 验证容量
        if (g.getCapacity() != null) {
            if (g.getCapacity() < 1) {
                throw new IllegalArgumentException("分组容量必须大于0");
            }
            if (g.getCapacity() > 100) {
                throw new IllegalArgumentException("分组容量不能超过100");
            }
        }
        
        return groupRepository.save(g);
    }

    public DefenseGroup getGroupById(Long id) {
        // 改进异常处理
        if (id == null || id <= 0) {
            throw new IllegalArgumentException("无效的分组ID");
        }
        return groupRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("分组不存在"));
    }

    public DefenseGroup updateGroup(Long id, DefenseGroup g) {
        DefenseGroup existing = getGroupById(id);
        
        // 增强输入验证
        if (g.getName() != null && !g.getName().trim().isEmpty()) {
            String name = g.getName().trim();
            if (name.length() > 100) {
                throw new IllegalArgumentException("分组名称长度不能超过100个字符");
            }
            existing.setName(name);
        }
        
        if (g.getType() != null && !g.getType().trim().isEmpty()) {
            String type = g.getType().trim();
            if (!"OPENING".equals(type) && !"FINAL".equals(type)) {
                throw new IllegalArgumentException("无效的分组类型，只支持OPENING或FINAL");
            }
            existing.setType(type);
        }
        
        if (g.getCapacity() != null) {
            if (g.getCapacity() < 1) {
                throw new IllegalArgumentException("分组容量必须大于0");
            }
            if (g.getCapacity() > 100) {
                throw new IllegalArgumentException("分组容量不能超过100");
            }
            // 检查当前成员数是否超过新容量
            List<GroupMember> members = memberRepository.findByGroupId(id);
            if (members.size() > g.getCapacity()) {
                throw new IllegalStateException("当前成员数(" + members.size() + ")已超过新容量(" + g.getCapacity() + ")");
            }
            existing.setCapacity(g.getCapacity());
        }
        
        return groupRepository.save(existing);
    }

    @Transactional
    public void deleteGroup(Long id) {
        // 检查是否有成员
        List<GroupMember> members = memberRepository.findByGroupId(id);
        if (!members.isEmpty()) {
            throw new IllegalStateException("该分组下存在成员，无法删除");
        }
        groupRepository.deleteById(id);
    }

    public Page<DefenseGroup> findGroups(Pageable pageable, String keyword, String type) {
        Specification<DefenseGroup> spec = (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            
            if (keyword != null && !keyword.trim().isEmpty()) {
                String pattern = "%" + keyword.trim() + "%";
                predicates.add(cb.like(root.get("name"), pattern));
            }
            
            if (type != null && !type.trim().isEmpty()) {
                predicates.add(cb.equal(root.get("type"), type));
            }
            
            return cb.and(predicates.toArray(new Predicate[0]));
        };
        
        return groupRepository.findAll(spec, pageable);
    }

    public Page<ReviewAssignment> findReviewTasks(Pageable pageable, Long reviewerId, String status, String type) {
        Specification<ReviewAssignment> spec = (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            
            if (reviewerId != null) {
                predicates.add(cb.equal(root.get("reviewerId"), reviewerId));
            }
            
            if (status != null && !status.trim().isEmpty()) {
                predicates.add(cb.equal(root.get("status"), status));
            }
            
            if (type != null && !type.trim().isEmpty()) {
                predicates.add(cb.equal(root.get("type"), type));
            }
            
            return cb.and(predicates.toArray(new Predicate[0]));
        };
        
        return reviewRepository.findAll(spec, pageable);
    }

    @Transactional
    public List<GroupMember> autoAssign(String type, int capacity) {
        // 输入验证
        if (type == null || type.trim().isEmpty()) {
            throw new IllegalArgumentException("分组类型不能为空");
        }
        if (!"OPENING".equals(type) && !"FINAL".equals(type)) {
            throw new IllegalArgumentException("无效的分组类型，只支持OPENING或FINAL");
        }
        if (capacity < 1) {
            throw new IllegalArgumentException("分组容量必须大于0");
        }
        if (capacity > 100) {
            throw new IllegalArgumentException("分组容量不能超过100");
        }
        
        List<StudentSelection> selections = selectionRepository.findAll().stream()
                .filter(s -> s.getStatus() == StudentSelection.SelectionStatus.SELECTED
                        || s.getStatus() == StudentSelection.SelectionStatus.LOCKED)
                .toList();
        if (selections.isEmpty()) {
            return List.of();
        }

        // 检查学生是否已经在其他分组中
        for (StudentSelection s : selections) {
            List<GroupMember> existing = memberRepository.findByStudentId(s.getStudentId());
            if (!existing.isEmpty()) {
                throw new IllegalStateException("学生ID " + s.getStudentId() + " 已在其他分组中，无法自动分配");
            }
        }

        int groupCount = (int) Math.ceil((double) selections.size() / capacity);
        List<DefenseGroup> groups = new ArrayList<>();
        for (int i = 0; i < groupCount; i++) {
            DefenseGroup g = new DefenseGroup();
            g.setName(type + "-G" + (i + 1));
            g.setType(type);
            g.setCapacity(capacity);
            groups.add(groupRepository.save(g));
        }

        List<GroupMember> members = new ArrayList<>();
        int idx = 0;
        for (StudentSelection s : selections) {
            DefenseGroup g = groups.get(idx % groupCount);
            GroupMember gm = new GroupMember();
            gm.setGroupId(g.getId());
            gm.setStudentId(s.getStudentId());
            gm.setTopicId(s.getTopicId());
            members.add(memberRepository.save(gm));
            idx++;
        }
        return members;
    }

    public List<GroupMember> members(Long groupId) {
        return memberRepository.findByGroupId(groupId);
    }

    public ReviewAssignment assignReview(Long reviewerId, Long studentId, Long topicId, String type) {
        // 增强输入验证
        if (reviewerId == null || reviewerId <= 0) {
            throw new IllegalArgumentException("评阅人ID无效");
        }
        if (studentId == null || studentId <= 0) {
            throw new IllegalArgumentException("学生ID无效");
        }
        if (type == null || type.trim().isEmpty()) {
            throw new IllegalArgumentException("评阅类型不能为空");
        }
        
        // 验证评阅人是否存在且是教师
        User reviewer = userRepository.findById(reviewerId).orElse(null);
        if (reviewer == null) {
            throw new IllegalArgumentException("评阅人不存在");
        }
        if (!"TEACHER".equalsIgnoreCase(reviewer.getRole()) && !"ADMIN".equalsIgnoreCase(reviewer.getRole())) {
            throw new IllegalArgumentException("评阅人必须是教师或管理员");
        }
        
        // 验证学生是否存在
        User student = userRepository.findById(studentId).orElse(null);
        if (student == null) {
            throw new IllegalArgumentException("学生不存在");
        }
        
        if (reviewRepository.existsByReviewerIdAndStudentIdAndType(reviewerId, studentId, type)) {
            throw new IllegalStateException("已存在相同评阅任务");
        }
        
        ReviewAssignment r = new ReviewAssignment();
        r.setReviewerId(reviewerId);
        r.setStudentId(studentId);
        r.setTopicId(topicId);
        r.setType(type.trim());
        r.setStatus("PENDING");
        return reviewRepository.save(r);
    }

    public List<ReviewAssignment> reviewerTasks(Long reviewerId) {
        return reviewRepository.findByReviewerId(reviewerId);
    }

    public List<ReviewAssignment> reviewerTasks(Long reviewerId, String status, String type) {
        List<ReviewAssignment> all = reviewRepository.findByReviewerId(reviewerId);
        if (status != null) {
            all = all.stream().filter(r -> status.equals(r.getStatus())).toList();
        }
        if (type != null) {
            all = all.stream().filter(r -> type.equals(r.getType())).toList();
        }
        return all;
    }

    public GroupMember addMember(Long groupId, Long studentId, Long topicId) {
        // 输入验证
        if (groupId == null || groupId <= 0) {
            throw new IllegalArgumentException("无效的分组ID");
        }
        if (studentId == null || studentId <= 0) {
            throw new IllegalArgumentException("无效的学生ID");
        }
        
        DefenseGroup group = getGroupById(groupId);
        
        // 检查学生是否已经在其他分组中
        List<GroupMember> studentGroups = memberRepository.findByStudentId(studentId);
        if (!studentGroups.isEmpty()) {
            throw new IllegalStateException("该学生已在其他分组中，无法重复添加");
        }
        
        List<GroupMember> existing = memberRepository.findByGroupId(groupId);
        if (existing.size() >= group.getCapacity()) {
            throw new IllegalStateException("分组容量已满");
        }
        
        // 验证学生是否存在
        User student = userRepository.findById(studentId).orElse(null);
        if (student == null) {
            throw new IllegalArgumentException("学生不存在");
        }
        
        GroupMember gm = new GroupMember();
        gm.setGroupId(groupId);
        gm.setStudentId(studentId);
        gm.setTopicId(topicId);
        return memberRepository.save(gm);
    }

    public void removeMember(Long memberId) {
        memberRepository.deleteById(memberId);
    }

    @Transactional
    public List<ReviewAssignment> autoCrossReview() {
        List<StudentSelection> selections = selectionRepository.findAll().stream()
                .filter(s -> s.getStatus() == StudentSelection.SelectionStatus.SELECTED
                        || s.getStatus() == StudentSelection.SelectionStatus.LOCKED)
                .toList();
        List<User> teachers = userRepository.findAll().stream()
                .filter(u -> "TEACHER".equals(u.getRole()))
                .toList();
        if (teachers.isEmpty()) {
            throw new IllegalStateException("没有可用教师");
        }
        List<ReviewAssignment> assignments = new ArrayList<>();
        int teacherIdx = 0;
        for (StudentSelection sel : selections) {
            User teacher = teachers.get(teacherIdx % teachers.size());
            if (!reviewRepository.existsByReviewerIdAndStudentIdAndType(teacher.getId(), sel.getStudentId(), "CROSS")) {
                ReviewAssignment r = new ReviewAssignment();
                r.setReviewerId(teacher.getId());
                r.setStudentId(sel.getStudentId());
                r.setTopicId(sel.getTopicId());
                r.setType("CROSS");
                r.setStatus("PENDING");
                assignments.add(reviewRepository.save(r));
            }
            teacherIdx++;
        }
        return assignments;
    }

    public ReviewAssignment completeReview(Long reviewId, String comment, Double score, Long actorId, boolean isAdmin) {
        // 输入验证
        if (reviewId == null || reviewId <= 0) {
            throw new IllegalArgumentException("无效的评阅ID");
        }
        
        ReviewAssignment r = reviewRepository.findById(reviewId)
                .orElseThrow(() -> new IllegalArgumentException("评阅任务不存在"));
        
        // 权限检查
        if (!isAdmin && actorId != null && !actorId.equals(r.getReviewerId())) {
            throw new SecurityException("无权完成他人评阅");
        }
        
        // 验证分数范围
        if (score != null) {
            if (score < 0 || score > 100) {
                throw new IllegalArgumentException("分数必须在0-100之间");
            }
        }
        
        // 验证评论长度
        if (comment != null && comment.length() > 1000) {
            throw new IllegalArgumentException("评论长度不能超过1000个字符");
        }
        
        r.setStatus("DONE");
        r.setComment(comment != null ? comment.trim() : null);
        r.setScore(score);
        return reviewRepository.save(r);
    }

    public DefenseScore recordScore(Long groupId, Long studentId, Double score, String comment) {
        // 输入验证
        if (groupId == null || groupId <= 0) {
            throw new IllegalArgumentException("无效的分组ID");
        }
        if (studentId == null || studentId <= 0) {
            throw new IllegalArgumentException("无效的学生ID");
        }
        if (score == null) {
            throw new IllegalArgumentException("分数不能为空");
        }
        if (score < 0 || score > 100) {
            throw new IllegalArgumentException("分数必须在0-100之间");
        }
        
        // 验证分组是否存在
        getGroupById(groupId);
        
        // 验证学生是否在该分组中
        List<GroupMember> members = memberRepository.findByGroupId(groupId);
        boolean studentInGroup = members.stream()
                .anyMatch(m -> m.getStudentId().equals(studentId));
        if (!studentInGroup) {
            throw new IllegalStateException("该学生不在指定分组中");
        }
        
        // 验证评论长度
        if (comment != null && comment.length() > 1000) {
            throw new IllegalArgumentException("评论长度不能超过1000个字符");
        }
        
        DefenseScore ds = new DefenseScore();
        ds.setGroupId(groupId);
        ds.setStudentId(studentId);
        ds.setScore(score);
        ds.setComment(comment != null ? comment.trim() : null);
        return scoreRepository.save(ds);
    }

    public List<DefenseScore> scoresByGroup(Long groupId) {
        return scoreRepository.findByGroupId(groupId);
    }

    public List<Map<String, Object>> gradeSummary() {
        Map<Long, List<DefenseScore>> byStudent = scoreRepository.findAll().stream()
                .collect(Collectors.groupingBy(DefenseScore::getStudentId));
        List<Map<String, Object>> result = new ArrayList<>();
        byStudent.forEach((sid, scores) -> {
            double avg = scores.stream().mapToDouble(DefenseScore::getScore).average().orElse(0);
            result.add(Map.of("studentId", sid, "avgScore", avg, "count", scores.size()));
        });
        return result;
    }

    public Long findUserId(String username) {
        return userRepository.findByUsername(username).map(User::getId).orElse(null);
    }

    public boolean isAdmin(Principal principal) {
        if (principal == null) return false;
        return userRepository.findByUsername(principal.getName())
                .map(u -> "ADMIN".equalsIgnoreCase(u.getRole()))
                .orElse(false);
    }

    public Optional<GroupMember> getStudentGroup(Long studentId) {
        return memberRepository.findByStudentId(studentId).stream().findFirst();
    }

    public List<GroupMember> getStudentGroups(Long studentId) {
        return memberRepository.findByStudentId(studentId);
    }

    public List<DefenseScore> getStudentScores(Long studentId) {
        return scoreRepository.findByStudentId(studentId);
    }

    // 获取分组详情（包含成员数量）
    public Map<String, Object> getGroupDetail(Long groupId) {
        DefenseGroup group = getGroupById(groupId);
        List<GroupMember> members = memberRepository.findByGroupId(groupId);
        List<DefenseScore> scores = scoreRepository.findByGroupId(groupId);
        
        return Map.of(
                "group", group,
                "memberCount", members.size(),
                "scoreCount", scores.size()
        );
    }

    // 获取成员详情列表（包含学生和课题信息）
    public List<Map<String, Object>> getMembersWithDetails(Long groupId) {
        List<GroupMember> members = memberRepository.findByGroupId(groupId);
        List<DefenseScore> scores = scoreRepository.findByGroupId(groupId);
        Map<Long, DefenseScore> scoreMap = scores.stream()
                .collect(Collectors.toMap(DefenseScore::getStudentId, s -> s, (s1, s2) -> s1));
        
        return members.stream().map(member -> {
            Map<String, Object> detail = new HashMap<>();
            detail.put("member", member);
            
            // 获取学生信息
            User student = userRepository.findById(member.getStudentId()).orElse(null);
            if (student != null) {
                detail.put("studentName", student.getFullName() != null ? student.getFullName() : student.getUsername());
                detail.put("studentUsername", student.getUsername());
            }
            
            // 获取成绩
            DefenseScore score = scoreMap.get(member.getStudentId());
            if (score != null) {
                detail.put("score", score.getScore());
                detail.put("comment", score.getComment());
            }
            
            return detail;
        }).collect(Collectors.toList());
    }
}

