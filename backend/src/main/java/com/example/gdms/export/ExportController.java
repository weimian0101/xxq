package com.example.gdms.export;

import com.example.gdms.group.DefenseScoreRepository;
import com.example.gdms.group.GroupMemberRepository;
import com.example.gdms.group.ReviewAssignmentRepository;
import com.example.gdms.stage.StageReviewRepository;
import com.example.gdms.topic.StudentSelectionRepository;
import com.example.gdms.topic.TopicApprovalRepository;
import com.example.gdms.topic.TopicRepository;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.nio.charset.StandardCharsets;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/exports")
@PreAuthorize("hasRole('ADMIN')")
public class ExportController {
    private final TopicRepository topicRepository;
    private final TopicApprovalRepository approvalRepository;
    private final StudentSelectionRepository selectionRepository;
    private final GroupMemberRepository memberRepository;
    private final DefenseScoreRepository scoreRepository;
    private final ReviewAssignmentRepository reviewAssignmentRepository;
    private final StageReviewRepository stageReviewRepository;

    public ExportController(TopicRepository topicRepository, TopicApprovalRepository approvalRepository, StudentSelectionRepository selectionRepository, GroupMemberRepository memberRepository, DefenseScoreRepository scoreRepository, ReviewAssignmentRepository reviewAssignmentRepository, StageReviewRepository stageReviewRepository) {
        this.topicRepository = topicRepository;
        this.approvalRepository = approvalRepository;
        this.selectionRepository = selectionRepository;
        this.memberRepository = memberRepository;
        this.scoreRepository = scoreRepository;
        this.reviewAssignmentRepository = reviewAssignmentRepository;
        this.stageReviewRepository = stageReviewRepository;
    }

    @GetMapping("/topics")
    public ResponseEntity<byte[]> exportTopics() {
        String csv = "id,title,status\n" + topicRepository.findAll().stream()
                .map(t -> t.getId() + "," + safe(t.getTitle()) + "," + t.getStatus())
                .collect(Collectors.joining("\n"));
        return csvResponse(csv, "topics.csv");
    }

    @GetMapping("/approvals")
    public ResponseEntity<byte[]> exportApprovals() {
        String csv = "topicId,reviewerId,decision,comment\n" + approvalRepository.findAll().stream()
                .map(a -> a.getTopicId() + "," + a.getReviewerId() + "," + a.getDecision() + "," + safe(a.getComment()))
                .collect(Collectors.joining("\n"));
        return csvResponse(csv, "approvals.csv");
    }

    @GetMapping("/selections")
    public ResponseEntity<byte[]> exportSelections() {
        String csv = "studentId,topicId,status\n" + selectionRepository.findAll().stream()
                .map(s -> s.getStudentId() + "," + s.getTopicId() + "," + s.getStatus())
                .collect(Collectors.joining("\n"));
        return csvResponse(csv, "selections.csv");
    }

    @GetMapping("/groups")
    public ResponseEntity<byte[]> exportGroups() {
        String csv = "groupId,studentId,topicId\n" + memberRepository.findAll().stream()
                .map(m -> m.getGroupId() + "," + m.getStudentId() + "," + m.getTopicId())
                .collect(Collectors.joining("\n"));
        return csvResponse(csv, "groups.csv");
    }

    @GetMapping("/scores")
    public ResponseEntity<byte[]> exportScores() {
        String csv = "groupId,studentId,score,comment\n" + scoreRepository.findAll().stream()
                .map(s -> s.getGroupId() + "," + s.getStudentId() + "," + s.getScore() + "," + safe(s.getComment()))
                .collect(Collectors.joining("\n"));
        return csvResponse(csv, "scores.csv");
    }

    @GetMapping("/reviews")
    public ResponseEntity<byte[]> exportReviews() {
        String csv = "reviewerId,studentId,topicId,type,status,score,comment\n" + reviewAssignmentRepository.findAll().stream()
                .map(r -> r.getReviewerId() + "," + r.getStudentId() + "," + r.getTopicId() + "," + r.getType() + "," + r.getStatus() + "," + (r.getScore() == null ? "" : r.getScore()) + "," + safe(r.getComment()))
                .collect(Collectors.joining("\n"));
        return csvResponse(csv, "reviews.csv");
    }

    @GetMapping("/stage-reviews")
    public ResponseEntity<byte[]> exportStageReviews() {
        String csv = "taskId,reviewerId,decision,comment,createdAt\n" + stageReviewRepository.findAll().stream()
                .map(r -> r.getTaskId() + "," + r.getReviewerId() + "," + r.getDecision() + "," + safe(r.getComment()) + "," + r.getCreatedAt())
                .collect(Collectors.joining("\n"));
        return csvResponse(csv, "stage_reviews.csv");
    }

    @GetMapping("/announcement-reads")
    public ResponseEntity<byte[]> exportAnnouncementReads() {
        // 需要注入AnnouncementReadRepository
        return csvResponse("announcementId,userId,readAt\n", "announcement_reads.csv");
    }

    private ResponseEntity<byte[]> csvResponse(String csv, String filename) {
        byte[] bytes = csv.getBytes(StandardCharsets.UTF_8);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=" + filename)
                .contentType(new MediaType("text", "csv"))
                .body(bytes);
    }

    private String safe(String s) {
        return s == null ? "" : s.replace(",", " ");
    }
}

