package com.example.gdms.group;

import jakarta.persistence.*;
import lombok.Data;

@Entity
@Table(name = "group_member")
@Data
public class GroupMember {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private Long groupId;
    private Long studentId;
    private Long topicId;
}

