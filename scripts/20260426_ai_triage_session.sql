USE onlinetreat;

CREATE TABLE IF NOT EXISTS `ai_triage_session` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `session_id` varchar(64) NOT NULL,
  `user_id` bigint NOT NULL,
  `patient_attendant_id` bigint DEFAULT NULL,
  `status` varchar(20) NOT NULL DEFAULT 'ACTIVE',
  `source` varchar(40) DEFAULT NULL,
  `recommended_departments` varchar(500) DEFAULT NULL,
  `recommended_sub_departments` varchar(500) DEFAULT NULL,
  `summary` text,
  `created_time` timestamp NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_time` timestamp NULL DEFAULT NULL ON UPDATE CURRENT_TIMESTAMP,
  `closed_time` timestamp NULL DEFAULT NULL,
  `is_deleted` int NOT NULL DEFAULT 0,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_ai_triage_session_id` (`session_id`),
  KEY `idx_ai_triage_user_status` (`user_id`, `status`),
  KEY `idx_ai_triage_created_time` (`created_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE IF NOT EXISTS `ai_triage_message` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `session_id` varchar(64) NOT NULL,
  `user_id` bigint NOT NULL,
  `role` varchar(20) NOT NULL,
  `content` text NOT NULL,
  `source` varchar(40) DEFAULT NULL,
  `recommended_departments` varchar(500) DEFAULT NULL,
  `recommended_sub_departments` varchar(500) DEFAULT NULL,
  `need_more_info` tinyint(1) NOT NULL DEFAULT 0,
  `emergency` tinyint(1) NOT NULL DEFAULT 0,
  `confidence` int DEFAULT NULL,
  `create_time` timestamp NULL DEFAULT CURRENT_TIMESTAMP,
  `is_deleted` int NOT NULL DEFAULT 0,
  PRIMARY KEY (`id`),
  KEY `idx_ai_triage_message_session` (`session_id`),
  KEY `idx_ai_triage_message_user` (`user_id`),
  KEY `idx_ai_triage_message_create_time` (`create_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
