package com.example.gdms.topic;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface TopicApprovalRepository extends JpaRepository<TopicApproval, Long> {
    List<TopicApproval> findByTopicId(Long topicId);
}

