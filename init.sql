-- 毕业设计过程管理及质量控制综合服务智能平台数据库初始化脚本
-- 确保所有表结构与实体类完全一致

-- 删除已存在的表（如果存在）
SET FOREIGN_KEY_CHECKS = 0;
DROP TABLE IF EXISTS `announcement_reads`;
DROP TABLE IF EXISTS `announcements`;
DROP TABLE IF EXISTS `application_logs`;
DROP TABLE IF EXISTS `applications`;
DROP TABLE IF EXISTS `defense_score`;
DROP TABLE IF EXISTS `review_assignment`;
DROP TABLE IF EXISTS `group_member`;
DROP TABLE IF EXISTS `defense_group`;
DROP TABLE IF EXISTS `stage_review`;
DROP TABLE IF EXISTS `stage_task`;
DROP TABLE IF EXISTS `stage_config`;
DROP TABLE IF EXISTS `student_selections`;
DROP TABLE IF EXISTS `topic_approvals`;
DROP TABLE IF EXISTS `topics`;
DROP TABLE IF EXISTS `menus`;
DROP TABLE IF EXISTS `orgs`;
DROP TABLE IF EXISTS `users`;
SET FOREIGN_KEY_CHECKS = 1;

-- 创建组织表
CREATE TABLE `orgs` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `name` VARCHAR(255) NOT NULL,
  `parent_id` BIGINT,
  `type` VARCHAR(50),
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_org_name` (`name`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 创建用户表
CREATE TABLE `users` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `username` VARCHAR(255) NOT NULL,
  `password` VARCHAR(255) NOT NULL,
  `full_name` VARCHAR(255),
  `role` VARCHAR(50),
  `org_id` BIGINT,
  `enabled` BOOLEAN DEFAULT TRUE,
  `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP,
  `phone` VARCHAR(50),
  `signature_url` VARCHAR(500),
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_username` (`username`),
  KEY `idx_org_id` (`org_id`),
  KEY `idx_role` (`role`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 创建菜单表
CREATE TABLE `menus` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `name` VARCHAR(255),
  `path` VARCHAR(500),
  `role` VARCHAR(50),
  `order_index` INT DEFAULT 0,
  PRIMARY KEY (`id`),
  KEY `idx_role_order` (`role`, `order_index`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 创建公告表
CREATE TABLE `announcements` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `title` VARCHAR(500),
  `content` TEXT,
  `created_by` BIGINT,
  `status` VARCHAR(50) DEFAULT 'DRAFT',
  `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP,
  `published_at` DATETIME,
  PRIMARY KEY (`id`),
  KEY `idx_status` (`status`),
  KEY `idx_created_at` (`created_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 创建公告已读表
CREATE TABLE `announcement_reads` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `announcement_id` BIGINT NOT NULL,
  `user_id` BIGINT NOT NULL,
  `read_at` DATETIME DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_announcement_user` (`announcement_id`, `user_id`),
  KEY `idx_announcement_id` (`announcement_id`),
  KEY `idx_user_id` (`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 创建课题表
CREATE TABLE `topics` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `title` VARCHAR(500),
  `description` TEXT,
  `creator_id` BIGINT,
  `capacity` INT DEFAULT 1,
  `status` VARCHAR(50) DEFAULT 'DRAFT',
  `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_creator_id` (`creator_id`),
  KEY `idx_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 创建课题审批表
CREATE TABLE `topic_approvals` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `topic_id` BIGINT NOT NULL,
  `reviewer_id` BIGINT NOT NULL,
  `decision` VARCHAR(50),
  `comment` TEXT,
  `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_topic_id` (`topic_id`),
  KEY `idx_reviewer_id` (`reviewer_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 创建学生选题表
CREATE TABLE `student_selections` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `student_id` BIGINT NOT NULL,
  `topic_id` BIGINT NOT NULL,
  `status` VARCHAR(50) DEFAULT 'SELECTED',
  `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_student_id` (`student_id`),
  KEY `idx_topic_id` (`topic_id`),
  KEY `idx_student_status` (`student_id`, `status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 创建阶段配置表
CREATE TABLE `stage_config` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `name` VARCHAR(255),
  `order_index` INT,
  `active` BOOLEAN DEFAULT TRUE,
  `start_at` DATETIME,
  `end_at` DATETIME,
  PRIMARY KEY (`id`),
  KEY `idx_order_index` (`order_index`),
  KEY `idx_active` (`active`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 创建阶段任务表
CREATE TABLE `stage_task` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `stage_id` BIGINT NOT NULL,
  `student_id` BIGINT NOT NULL,
  `topic_id` BIGINT,
  `status` VARCHAR(50) DEFAULT 'PENDING',
  `content` TEXT,
  `updated_at` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_stage_id` (`stage_id`),
  KEY `idx_student_id` (`student_id`),
  KEY `idx_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 创建阶段审核表
CREATE TABLE `stage_review` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `task_id` BIGINT NOT NULL,
  `reviewer_id` BIGINT NOT NULL,
  `decision` VARCHAR(50),
  `comment` TEXT,
  `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_task_id` (`task_id`),
  KEY `idx_reviewer_id` (`reviewer_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 创建申请表
CREATE TABLE `applications` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `type` VARCHAR(50) NOT NULL,
  `student_id` BIGINT NOT NULL,
  `topic_id` BIGINT,
  `status` VARCHAR(50) DEFAULT 'SUBMITTED',
  `payload` TEXT,
  `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_student_id` (`student_id`),
  KEY `idx_type` (`type`),
  KEY `idx_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 创建申请日志表
CREATE TABLE `application_logs` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `application_id` BIGINT NOT NULL,
  `actor_id` BIGINT,
  `action` VARCHAR(100),
  `comment` TEXT,
  `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_application_id` (`application_id`),
  KEY `idx_actor_id` (`actor_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 创建答辩分组表
CREATE TABLE `defense_group` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `name` VARCHAR(255),
  `type` VARCHAR(50),
  `capacity` INT DEFAULT 8,
  `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_type` (`type`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 创建分组成员表
CREATE TABLE `group_member` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `group_id` BIGINT NOT NULL,
  `student_id` BIGINT NOT NULL,
  `topic_id` BIGINT,
  PRIMARY KEY (`id`),
  KEY `idx_group_id` (`group_id`),
  KEY `idx_student_id` (`student_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 创建评阅任务表
CREATE TABLE `review_assignment` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `reviewer_id` BIGINT NOT NULL,
  `student_id` BIGINT NOT NULL,
  `topic_id` BIGINT,
  `type` VARCHAR(50),
  `status` VARCHAR(50) DEFAULT 'PENDING',
  `comment` TEXT,
  `score` DOUBLE,
  `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_reviewer_id` (`reviewer_id`),
  KEY `idx_student_id` (`student_id`),
  KEY `idx_status` (`status`),
  KEY `idx_type` (`type`),
  UNIQUE KEY `uk_reviewer_student_type` (`reviewer_id`, `student_id`, `type`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 创建答辩成绩表
CREATE TABLE `defense_score` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `group_id` BIGINT NOT NULL,
  `student_id` BIGINT NOT NULL,
  `score` DOUBLE,
  `comment` TEXT,
  PRIMARY KEY (`id`),
  KEY `idx_group_id` (`group_id`),
  KEY `idx_student_id` (`student_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 插入初始数据

-- 插入组织数据
INSERT INTO `orgs` (`id`, `name`, `type`, `parent_id`) VALUES
(1, '计算机学院', 'COLLEGE', NULL),
(2, '软件工程系', 'DEPT', 1),
(3, '计算机科学系', 'DEPT', 1);

-- 插入用户数据（密码使用BCrypt哈希，强度12，添加{bcrypt}前缀以兼容DelegatingPasswordEncoder）
-- 每个用户使用不同的哈希值（BCrypt特性：相同密码每次哈希都不同）
INSERT INTO `users` (`id`, `username`, `password`, `full_name`, `role`, `org_id`, `enabled`, `phone`, `signature_url`) VALUES
(1, 'admin', '{bcrypt}$2a$12$pqpxLXfOI3DSMe6TfAGR9.0fTn1tWZdR5ZhjeiuAfxqerEIx/cfcq', '系统管理员', 'ADMIN', 1, TRUE, '13800138000', NULL),
(2, 'teacher1', '{bcrypt}$2a$12$OM7DUV2kuu1cRanBRI5JLOe8mrCkLy5X8ZwVKOsR4YBEL7b5w5hdy', '张老师', 'TEACHER', 2, TRUE, '13800138001', NULL),
(3, 'teacher2', '{bcrypt}$2a$12$jHaVKbk64QC8VkFAO41.3OFoQivDmUPw4aT.K8tDb1LWW1mPPZpOq', '李老师', 'TEACHER', 2, TRUE, '13800138002', NULL),
(4, 'teacher3', '{bcrypt}$2a$12$Aqwp8EXD56quYQa1NXfZTeOo.2Dg0EtGRzKqaSaxeAaW1dROylcmG', '王老师', 'TEACHER', 3, TRUE, '13800138003', NULL),
(5, 'student1', '{bcrypt}$2a$12$FfvMF4Ojy8s4pYJSG60CvOT7EjqCklRtoaG77zJdAMzR/4aIDKL9e', '学生1', 'STUDENT', 2, TRUE, '13800138010', NULL),
(6, 'student2', '{bcrypt}$2a$12$AHOxj6wxJcc4YHDEgJnmveLwG5daYrX8LHYjo64SzyM4r/M5vUkyS', '学生2', 'STUDENT', 2, TRUE, '13800138011', NULL),
(7, 'student3', '{bcrypt}$2a$12$3kRhXJ5G/RUbN03lnJlLaexTFJi2tysRz/iFC24RjFZJw5UONop0.', '学生3', 'STUDENT', 3, TRUE, '13800138012', NULL),
(8, 'student4', '{bcrypt}$2a$12$cvgHWT0DUybw6harPHj1hON61GOWGPM3xEuY5/AiDCJwlrjJAZfvm', '学生4', 'STUDENT', 3, TRUE, '13800138013', NULL);

-- 插入菜单数据
INSERT INTO `menus` (`id`, `name`, `path`, `role`, `order_index`) VALUES
(1, '首页', '/', 'ADMIN', 1),
(2, '首页', '/', 'TEACHER', 1),
(3, '首页', '/', 'STUDENT', 1),
(4, '用户管理', '/users', 'ADMIN', 2),
(5, '组织管理', '/orgs', 'ADMIN', 3),
(6, '菜单管理', '/menus', 'ADMIN', 4),
(7, '课题管理', '/topics', 'ADMIN', 5),
(8, '课题管理', '/topics', 'TEACHER', 2),
(9, '选题', '/topics', 'STUDENT', 2),
(10, '阶段管理', '/stage-admin', 'ADMIN', 6),
(11, '阶段任务', '/stage', 'TEACHER', 3),
(12, '阶段任务', '/stage', 'STUDENT', 3),
(13, '申请管理', '/applications', 'ADMIN', 7),
(14, '申请管理', '/applications', 'TEACHER', 4),
(15, '我的申请', '/applications', 'STUDENT', 4),
(16, '答辩管理', '/defense', 'ADMIN', 8),
(17, '答辩管理', '/defense', 'TEACHER', 5),
(18, '公告管理', '/announcements', 'ADMIN', 9),
(19, '公告管理', '/announcements', 'TEACHER', 6),
(20, '公告', '/announcements', 'STUDENT', 5);

-- 插入阶段配置数据
INSERT INTO `stage_config` (`id`, `name`, `order_index`, `active`, `start_at`, `end_at`) VALUES
(1, '开题报告', 1, TRUE, '2024-01-01 00:00:00', '2024-03-31 23:59:59'),
(2, '中期检查', 2, TRUE, '2024-04-01 00:00:00', '2024-06-30 23:59:59'),
(3, '答辩前准备', 3, TRUE, '2024-07-01 00:00:00', '2024-08-31 23:59:59');

-- 插入示例课题数据
INSERT INTO `topics` (`id`, `title`, `description`, `creator_id`, `capacity`, `status`, `created_at`) VALUES
(1, '基于Spring Boot的毕业设计管理系统', '开发一个完整的毕业设计管理系统', 2, 3, 'APPROVED', NOW()),
(2, '基于Vue.js的前端框架研究', '研究Vue.js框架的核心原理和应用', 2, 2, 'APPROVED', NOW()),
(3, '机器学习在数据分析中的应用', '研究机器学习算法在数据分析中的实际应用', 3, 2, 'SUBMITTED', NOW()),
(4, '分布式系统设计与实现', '设计并实现一个分布式系统', 3, 1, 'DRAFT', NOW());

-- 插入示例选题数据
INSERT INTO `student_selections` (`id`, `student_id`, `topic_id`, `status`, `created_at`) VALUES
(1, 5, 1, 'SELECTED', NOW()),
(2, 6, 1, 'SELECTED', NOW()),
(3, 7, 2, 'LOCKED', NOW());

-- 插入示例公告数据
INSERT INTO `announcements` (`id`, `title`, `content`, `created_by`, `status`, `created_at`, `published_at`) VALUES
(1, '关于毕业设计开题的通知', '请各位同学按时提交开题报告', 1, 'PUBLISHED', NOW(), NOW()),
(2, '中期检查时间安排', '中期检查将于下周三进行', 1, 'PUBLISHED', NOW(), NOW()),
(3, '答辩安排草案', '答辩安排草案，待确认', 1, 'DRAFT', NOW(), NULL);

-- 插入示例公告已读数据
INSERT INTO `announcement_reads` (`id`, `announcement_id`, `user_id`, `read_at`) VALUES
(1, 1, 5, NOW()),
(2, 1, 6, NOW()),
(3, 2, 5, NOW());

-- 插入示例答辩分组数据
INSERT INTO `defense_group` (`id`, `name`, `type`, `capacity`, `created_at`) VALUES
(1, '第一答辩组', 'FINAL', 8, NOW()),
(2, '第二答辩组', 'FINAL', 8, NOW()),
(3, '开题答辩组', 'OPENING', 6, NOW());

-- 插入示例分组成员数据
INSERT INTO `group_member` (`id`, `group_id`, `student_id`, `topic_id`) VALUES
(1, 1, 5, 1),
(2, 1, 6, 1),
(3, 2, 7, 2);

-- 插入示例评阅任务数据
INSERT INTO `review_assignment` (`id`, `reviewer_id`, `student_id`, `topic_id`, `type`, `status`, `score`, `created_at`) VALUES
(1, 3, 5, 1, 'CROSS', 'PENDING', NULL, NOW()),
(2, 4, 6, 1, 'CROSS', 'DONE', 85.5, NOW()),
(3, 2, 7, 2, 'ADVISOR', 'PENDING', NULL, NOW());

-- 插入示例答辩成绩数据
INSERT INTO `defense_score` (`id`, `group_id`, `student_id`, `score`, `comment`) VALUES
(1, 1, 5, 88.5, '表现优秀'),
(2, 1, 6, 85.0, '表现良好');

-- 插入示例申请数据
INSERT INTO `applications` (`id`, `type`, `student_id`, `topic_id`, `status`, `payload`, `created_at`) VALUES
(1, 'EXTERNAL', 5, 1, 'SUBMITTED', '申请在校外完成毕业设计', NOW()),
(2, 'DEFENSE', 6, 1, 'APPROVED', '申请参加答辩', NOW()),
(3, 'EXTENSION', 7, 2, 'REJECTED', '申请延期提交', NOW());

-- 插入示例申请日志数据
INSERT INTO `application_logs` (`id`, `application_id`, `actor_id`, `action`, `comment`, `created_at`) VALUES
(1, 1, 5, 'SUBMITTED', '学生提交申请', NOW()),
(2, 2, 6, 'SUBMITTED', '学生提交申请', NOW()),
(3, 2, 1, 'APPROVED', '管理员审批通过', NOW()),
(4, 3, 7, 'SUBMITTED', '学生提交申请', NOW()),
(5, 3, 1, 'REJECTED', '管理员审批驳回', NOW());

-- 插入示例阶段任务数据
INSERT INTO `stage_task` (`id`, `stage_id`, `student_id`, `topic_id`, `status`, `content`, `updated_at`) VALUES
(1, 1, 5, 1, 'SUBMITTED', '开题报告内容...', NOW()),
(2, 1, 6, 1, 'APPROVED', '开题报告内容...', NOW()),
(3, 2, 5, 1, 'PENDING', NULL, NOW());

-- 插入示例阶段审核数据
INSERT INTO `stage_review` (`id`, `task_id`, `reviewer_id`, `decision`, `comment`, `created_at`) VALUES
(1, 1, 2, 'SUBMITTED', '已提交，待审核', NOW()),
(2, 2, 2, 'APPROVED', '开题报告审核通过', NOW());

-- 插入示例课题审批数据
INSERT INTO `topic_approvals` (`id`, `topic_id`, `reviewer_id`, `decision`, `comment`, `created_at`) VALUES
(1, 1, 1, 'APPROVED', '课题审核通过', NOW()),
(2, 2, 1, 'APPROVED', '课题审核通过', NOW()),
(3, 3, 1, 'SUBMITTED', '待审核', NOW());

