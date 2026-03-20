ALTER TABLE `registration`
    ADD COLUMN `person_key` varchar(64) NULL DEFAULT NULL AFTER `registration_status`,
    ADD COLUMN `request_token` varchar(80) NULL DEFAULT NULL AFTER `person_key`,
    ADD INDEX `idx_registration_schedule_person` (`schedule_id`, `person_key`),
    ADD UNIQUE INDEX `uk_registration_request_token` (`request_token`);

UPDATE `registration` r
JOIN `patient_attendant` p ON r.`patient_id` = p.`id`
SET r.`person_key` = CASE
    WHEN p.`id_card` IS NULL OR TRIM(p.`id_card`) = '' THEN NULL
    ELSE SHA2(UPPER(TRIM(p.`id_card`)), 256)
END
WHERE r.`person_key` IS NULL;

CREATE TABLE IF NOT EXISTS `registration_person_lock` (
    `id` bigint NOT NULL AUTO_INCREMENT,
    `schedule_id` bigint NOT NULL,
    `person_key` varchar(64) NOT NULL,
    `request_token` varchar(80) NOT NULL,
    `registration_id` bigint NULL DEFAULT NULL,
    `create_time` timestamp NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_registration_person_lock` (`schedule_id`, `person_key`),
    UNIQUE KEY `uk_registration_person_request` (`request_token`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
