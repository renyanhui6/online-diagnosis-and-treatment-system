/*
 Navicat Premium Dump SQL (filtered)

 Source Schema         : OnlineTreat
 Target Server Type    : MySQL 8.0+
 File Encoding         : 65001 (UTF-8)
 Date                  : 2025-12-16

 说明：
 - 该脚本仅保留 medical-back 项目实际使用的表（MyBatis-Plus 实体/Mapper 引用的表）。
 - 已移除 song/song_list/singer/collect/comment/consumer 等与本项目无关的音乐类表。
 - 含真实业务数据（包含身份证号/手机号等敏感信息），请妥善保管。
*/

SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

CREATE DATABASE IF NOT EXISTS `OnlineTreat`
  DEFAULT CHARACTER SET utf8mb4
  DEFAULT COLLATE utf8mb4_0900_ai_ci;

USE `OnlineTreat`;

-- ----------------------------
-- Table structure for department
-- ----------------------------
DROP TABLE IF EXISTS `department`;
CREATE TABLE `department`  (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `department_name` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL,
  `description` text CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL,
  `create_time` timestamp NULL DEFAULT CURRENT_TIMESTAMP,
  `is_deleted` int NULL DEFAULT 0,
  PRIMARY KEY (`id`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 8 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci ROW_FORMAT = DYNAMIC;

-- ----------------------------
-- Records of department
-- ----------------------------
INSERT INTO `department` VALUES (1, '内科', '负责诊断和治疗内脏器官疾病', '2025-07-29 18:44:42', 0);
INSERT INTO `department` VALUES (2, '外科', '负责手术治疗的科室', '2025-07-29 18:44:42', 0);
INSERT INTO `department` VALUES (3, '妇产科', '负责女性生殖系统疾病诊治及分娩', '2025-07-29 18:44:42', 0);
INSERT INTO `department` VALUES (4, '儿科', '专注于儿童疾病诊治', '2025-07-29 18:44:42', 0);
INSERT INTO `department` VALUES (5, '眼科', '负责眼部疾病诊疗和视力保健', '2025-07-29 18:44:42', 0);
INSERT INTO `department` VALUES (7, '部门6', '第六个部门', '2025-08-02 09:37:33', 1);

-- ----------------------------
-- Table structure for sub_department
-- ----------------------------
DROP TABLE IF EXISTS `sub_department`;
CREATE TABLE `sub_department`  (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `parent_department_id` bigint NOT NULL,
  `department_name` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL,
  `description` text CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL,
  `treatment_scope` text CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL,
  `department_features` text CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL,
  `image_path` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL,
  `create_time` timestamp NULL DEFAULT CURRENT_TIMESTAMP,
  `is_deleted` int NULL DEFAULT 0,
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `parent_department_id`(`parent_department_id` ASC) USING BTREE,
  CONSTRAINT `sub_department_ibfk_1` FOREIGN KEY (`parent_department_id`) REFERENCES `department` (`id`) ON DELETE RESTRICT ON UPDATE RESTRICT
) ENGINE = InnoDB AUTO_INCREMENT = 16 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci ROW_FORMAT = DYNAMIC;

-- ----------------------------
-- Records of sub_department
-- ----------------------------
INSERT INTO `sub_department` VALUES (1, 1, '心血管内科', '心脏及血管疾病诊疗', '高血压、冠心病、心律失常', '介入治疗中心', 'http://szwlb5xin.hn-bkt.clouddn.com/%E7%A7%91%E5%AE%A4/xinxueguan.png', '2025-07-29 18:44:42', 0);
INSERT INTO `sub_department` VALUES (2, 1, '呼吸内科', '呼吸系统疾病诊疗', '肺炎、哮喘、COPD', '呼吸重症监护室', 'http://szwlb5xin.hn-bkt.clouddn.com/%E7%A7%91%E5%AE%A4/huxineike.png', '2025-07-29 18:44:42', 0);
INSERT INTO `sub_department` VALUES (3, 1, '消化内科', '消化道疾病诊疗', '胃炎、溃疡、肝炎', '内镜中心', 'http://szwlb5xin.hn-bkt.clouddn.com/%E7%A7%91%E5%AE%A4/xiaohuanei.png', '2025-07-29 18:44:42', 0);
INSERT INTO `sub_department` VALUES (4, 2, '骨科', '骨骼肌肉系统疾病', '骨折、关节炎、脊柱病变', '微创关节置换', 'http://szwlb5xin.hn-bkt.clouddn.com/%E7%A7%91%E5%AE%A4/guke.png', '2025-07-29 18:44:42', 0);
INSERT INTO `sub_department` VALUES (5, 2, '神经外科', '神经系统外科治疗', '脑肿瘤、脑外伤、脑血管病', '立体定向手术', 'http://szwlb5xin.hn-bkt.clouddn.com/%E7%A7%91%E5%AE%A4/shenjingwai.png', '2025-07-29 18:44:42', 0);
INSERT INTO `sub_department` VALUES (6, 2, '普外科', '腹部外科综合诊疗', '阑尾炎、疝气、胃肠道肿瘤', '腹腔镜手术', 'http://szwlb5xin.hn-bkt.clouddn.com/%E7%A7%91%E5%AE%A4/puwaike.png', '2025-07-29 18:44:42', 0);
INSERT INTO `sub_department` VALUES (7, 3, '妇科', '女性生殖系统疾病', '子宫肌瘤、月经不调、妇科肿瘤', '宫腹腔镜中心', 'http://szwlb5xin.hn-bkt.clouddn.com/%E7%A7%91%E5%AE%A4/fuke.png', '2025-07-29 18:44:42', 0);
INSERT INTO `sub_department` VALUES (8, 3, '产科', '孕产期保健与分娩', '高危妊娠、无痛分娩、产后康复', 'LDR一体化产房', 'http://szwlb5xin.hn-bkt.clouddn.com/%E7%A7%91%E5%AE%A4/chanke.png', '2025-07-29 18:44:42', 0);
INSERT INTO `sub_department` VALUES (9, 4, '新生儿科', '新生儿疾病诊疗', '早产儿、新生儿黄疸、呼吸窘迫', 'NICU监护中心', 'http://szwlb5xin.hn-bkt.clouddn.com/%E7%A7%91%E5%AE%A4/xinshenger.png', '2025-07-29 18:44:42', 0);
INSERT INTO `sub_department` VALUES (10, 4, '小儿内科', '儿童内科疾病', '肺炎、腹泻、生长发育障碍', '儿童哮喘门诊', 'http://szwlb5xin.hn-bkt.clouddn.com/%E7%A7%91%E5%AE%A4/xiaoernei.png', '2025-07-29 18:44:42', 0);
INSERT INTO `sub_department` VALUES (11, 4, '小儿外科', '儿童外科疾病', '先天性畸形、急腹症、创伤', '微创小儿外科', 'http://szwlb5xin.hn-bkt.clouddn.com/%E7%A7%91%E5%AE%A4/xiaoerwai.png', '2025-07-29 18:44:42', 0);
INSERT INTO `sub_department` VALUES (12, 5, '屈光矫正科', '视力矫正治疗', '近视、远视、散光', '全飞秒激光中心', 'http://szwlb5xin.hn-bkt.clouddn.com/%E7%A7%91%E5%AE%A4/quguangjiaozheng.png', '2025-07-29 18:44:42', 0);
INSERT INTO `sub_department` VALUES (13, 5, '眼底病科', '眼底疾病诊疗', '糖尿病视网膜病变、黄斑变性', 'OCT影像诊断', 'http://szwlb5xin.hn-bkt.clouddn.com/%E7%A7%91%E5%AE%A4/yandibing.png', '2025-07-29 18:44:42', 0);

-- ----------------------------
-- Table structure for system_user
-- ----------------------------
DROP TABLE IF EXISTS `system_user`;
CREATE TABLE `system_user`  (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `username` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL,
  `password` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL,
  `type` int NOT NULL,
  `create_time` timestamp NULL DEFAULT CURRENT_TIMESTAMP,
  `email` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL,
  `update_time` timestamp NULL DEFAULT NULL ON UPDATE CURRENT_TIMESTAMP,
  `register_type` int NULL DEFAULT 1,
  `status` int NULL DEFAULT 1,
  `is_deleted` int NULL DEFAULT 0,
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `username`(`username` ASC) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 82 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci ROW_FORMAT = DYNAMIC;

-- ----------------------------
-- Records of system_user
-- ----------------------------
INSERT INTO `system_user` VALUES (3, 'user1', '8d969eef6ecad3c29a3a629280e686cf0c3f5d5a86aff3ca12020c923adc6c92', 1, '2025-07-29 00:49:34', '2679327648@qq.com', '2025-08-08 13:59:44', 1, 1, 0);
INSERT INTO `system_user` VALUES (4, 'cardio_spec', '8d969eef6ecad3c29a3a629280e686cf0c3f5d5a86aff3ca12020c923adc6c92', 2, '2025-07-29 21:36:13', 'cardio_spec@qq.com', NULL, 0, 1, 0);
INSERT INTO `system_user` VALUES (5, 'cardio_dir1', '8d969eef6ecad3c29a3a629280e686cf0c3f5d5a86aff3ca12020c923adc6c92', 2, '2025-07-29 21:36:13', 'cardio_dir1@qq.com', NULL, 0, 1, 0);
INSERT INTO `system_user` VALUES (6, 'cardio_dir2', '8d969eef6ecad3c29a3a629280e686cf0c3f5d5a86aff3ca12020c923adc6c92', 2, '2025-07-29 21:36:13', 'cardio_dir2@qq.com', NULL, 0, 1, 0);
INSERT INTO `system_user` VALUES (7, 'cardio_dir3', '8d969eef6ecad3c29a3a629280e686cf0c3f5d5a86aff3ca12020c923adc6c92', 2, '2025-07-29 21:36:13', 'cardio_dir3@qq.com', NULL, 0, 1, 0);
INSERT INTO `system_user` VALUES (8, 'cardio_dir4', '8d969eef6ecad3c29a3a629280e686cf0c3f5d5a86aff3ca12020c923adc6c92', 2, '2025-07-29 21:36:13', 'cardio_dir4@qq.com', NULL, 0, 1, 0);
INSERT INTO `system_user` VALUES (9, 'pulmo_spec', '8d969eef6ecad3c29a3a629280e686cf0c3f5d5a86aff3ca12020c923adc6c92', 2, '2025-07-29 21:36:13', 'pulmo_spec@qq.com', NULL, 0, 1, 0);
INSERT INTO `system_user` VALUES (10, 'pulmo_dir1', '8d969eef6ecad3c29a3a629280e686cf0c3f5d5a86aff3ca12020c923adc6c92', 2, '2025-07-29 21:36:13', 'pulmo_dir1@qq.com', NULL, 0, 1, 0);
INSERT INTO `system_user` VALUES (11, 'pulmo_dir2', '8d969eef6ecad3c29a3a629280e686cf0c3f5d5a86aff3ca12020c923adc6c92', 2, '2025-07-29 21:36:13', 'pulmo_dir2@qq.com', NULL, 0, 1, 0);
INSERT INTO `system_user` VALUES (12, 'pulmo_dir3', '8d969eef6ecad3c29a3a629280e686cf0c3f5d5a86aff3ca12020c923adc6c92', 2, '2025-07-29 21:36:13', 'pulmo_dir3@qq.com', NULL, 0, 1, 0);
INSERT INTO `system_user` VALUES (13, 'pulmo_dir4', '8d969eef6ecad3c29a3a629280e686cf0c3f5d5a86aff3ca12020c923adc6c92', 2, '2025-07-29 21:36:13', 'pulmo_dir4@qq.com', NULL, 0, 1, 0);
INSERT INTO `system_user` VALUES (14, 'gi_spec', '8d969eef6ecad3c29a3a629280e686cf0c3f5d5a86aff3ca12020c923adc6c92', 2, '2025-07-29 21:36:13', 'gi_spec@qq.com', NULL, 0, 1, 0);
INSERT INTO `system_user` VALUES (15, 'gi_dir1', '8d969eef6ecad3c29a3a629280e686cf0c3f5d5a86aff3ca12020c923adc6c92', 2, '2025-07-29 21:36:13', 'gi_dir1@qq.com', NULL, 0, 1, 0);
INSERT INTO `system_user` VALUES (16, 'gi_dir2', '8d969eef6ecad3c29a3a629280e686cf0c3f5d5a86aff3ca12020c923adc6c92', 2, '2025-07-29 21:36:13', 'gi_dir2@qq.com', NULL, 0, 1, 0);
INSERT INTO `system_user` VALUES (17, 'gi_dir3', '8d969eef6ecad3c29a3a629280e686cf0c3f5d5a86aff3ca12020c923adc6c92', 2, '2025-07-29 21:36:13', 'gi_dir3@qq.com', NULL, 0, 1, 0);
INSERT INTO `system_user` VALUES (18, 'gi_dir4', '8d969eef6ecad3c29a3a629280e686cf0c3f5d5a86aff3ca12020c923adc6c92', 2, '2025-07-29 21:36:13', 'gi_dir4@qq.com', NULL, 0, 1, 0);
INSERT INTO `system_user` VALUES (19, 'ortho_spec', '8d969eef6ecad3c29a3a629280e686cf0c3f5d5a86aff3ca12020c923adc6c92', 2, '2025-07-29 21:36:13', 'ortho_spec@qq.com', NULL, 0, 1, 0);
INSERT INTO `system_user` VALUES (20, 'ortho_dir1', '8d969eef6ecad3c29a3a629280e686cf0c3f5d5a86aff3ca12020c923adc6c92', 2, '2025-07-29 21:36:13', 'ortho_dir1@qq.com', NULL, 0, 1, 0);
INSERT INTO `system_user` VALUES (21, 'ortho_dir2', '8d969eef6ecad3c29a3a629280e686cf0c3f5d5a86aff3ca12020c923adc6c92', 2, '2025-07-29 21:36:13', 'ortho_dir2@qq.com', NULL, 0, 1, 0);
INSERT INTO `system_user` VALUES (22, 'ortho_dir3', '8d969eef6ecad3c29a3a629280e686cf0c3f5d5a86aff3ca12020c923adc6c92', 2, '2025-07-29 21:36:13', 'ortho_dir3@qq.com', NULL, 0, 1, 0);
INSERT INTO `system_user` VALUES (23, 'ortho_dir4', '8d969eef6ecad3c29a3a629280e686cf0c3f5d5a86aff3ca12020c923adc6c92', 2, '2025-07-29 21:36:13', 'ortho_dir4@qq.com', NULL, 0, 1, 0);
INSERT INTO `system_user` VALUES (24, 'neuro_spec', '8d969eef6ecad3c29a3a629280e686cf0c3f5d5a86aff3ca12020c923adc6c92', 2, '2025-07-29 21:36:13', 'neuro_spec@qq.com', NULL, 0, 1, 0);
INSERT INTO `system_user` VALUES (25, 'neuro_dir1', '8d969eef6ecad3c29a3a629280e686cf0c3f5d5a86aff3ca12020c923adc6c92', 2, '2025-07-29 21:36:13', 'neuro_dir1@qq.com', NULL, 0, 1, 0);
INSERT INTO `system_user` VALUES (26, 'neuro_dir2', '8d969eef6ecad3c29a3a629280e686cf0c3f5d5a86aff3ca12020c923adc6c92', 2, '2025-07-29 21:36:13', 'neuro_dir2@qq.com', NULL, 0, 1, 0);
INSERT INTO `system_user` VALUES (27, 'neuro_dir3', '8d969eef6ecad3c29a3a629280e686cf0c3f5d5a86aff3ca12020c923adc6c92', 2, '2025-07-29 21:36:13', 'neuro_dir3@qq.com', NULL, 0, 1, 0);
INSERT INTO `system_user` VALUES (28, 'neuro_dir4', '8d969eef6ecad3c29a3a629280e686cf0c3f5d5a86aff3ca12020c923adc6c92', 2, '2025-07-29 21:36:13', 'neuro_dir4@qq.com', NULL, 0, 1, 0);
INSERT INTO `system_user` VALUES (29, 'surgery_spec', '8d969eef6ecad3c29a3a629280e686cf0c3f5d5a86aff3ca12020c923adc6c92', 2, '2025-07-29 21:36:13', 'surgery_spec@qq.com', NULL, 0, 1, 0);
INSERT INTO `system_user` VALUES (30, 'surgery_dir1', '8d969eef6ecad3c29a3a629280e686cf0c3f5d5a86aff3ca12020c923adc6c92', 2, '2025-07-29 21:36:13', 'surgery_dir1@qq.com', NULL, 0, 1, 0);
INSERT INTO `system_user` VALUES (31, 'surgery_dir2', '8d969eef6ecad3c29a3a629280e686cf0c3f5d5a86aff3ca12020c923adc6c92', 2, '2025-07-29 21:36:13', 'surgery_dir2@qq.com', NULL, 0, 1, 0);
INSERT INTO `system_user` VALUES (32, 'surgery_dir3', '8d969eef6ecad3c29a3a629280e686cf0c3f5d5a86aff3ca12020c923adc6c92', 2, '2025-07-29 21:36:13', 'surgery_dir3@qq.com', NULL, 0, 1, 0);
INSERT INTO `system_user` VALUES (33, 'surgery_dir4', '8d969eef6ecad3c29a3a629280e686cf0c3f5d5a86aff3ca12020c923adc6c92', 2, '2025-07-29 21:36:13', 'surgery_dir4@qq.com', NULL, 0, 1, 0);
INSERT INTO `system_user` VALUES (34, 'gyn_spec', '8d969eef6ecad3c29a3a629280e686cf0c3f5d5a86aff3ca12020c923adc6c92', 2, '2025-07-29 21:36:13', 'gyn_spec@qq.com', NULL, 0, 1, 0);
INSERT INTO `system_user` VALUES (35, 'gyn_dir1', '8d969eef6ecad3c29a3a629280e686cf0c3f5d5a86aff3ca12020c923adc6c92', 2, '2025-07-29 21:36:13', 'gyn_dir1@qq.com', NULL, 0, 1, 0);
INSERT INTO `system_user` VALUES (36, 'gyn_dir2', '8d969eef6ecad3c29a3a629280e686cf0c3f5d5a86aff3ca12020c923adc6c92', 2, '2025-07-29 21:36:13', 'gyn_dir2@qq.com', NULL, 0, 1, 0);
INSERT INTO `system_user` VALUES (37, 'gyn_dir3', '8d969eef6ecad3c29a3a629280e686cf0c3f5d5a86aff3ca12020c923adc6c92', 2, '2025-07-29 21:36:13', 'gyn_dir3@qq.com', NULL, 0, 1, 0);
INSERT INTO `system_user` VALUES (38, 'gyn_dir4', '8d969eef6ecad3c29a3a629280e686cf0c3f5d5a86aff3ca12020c923adc6c92', 2, '2025-07-29 21:36:13', 'gyn_dir4@qq.com', NULL, 0, 1, 0);
INSERT INTO `system_user` VALUES (39, 'obstet_spec', '8d969eef6ecad3c29a3a629280e686cf0c3f5d5a86aff3ca12020c923adc6c92', 2, '2025-07-29 21:36:13', 'obstet_spec@qq.com', NULL, 0, 1, 0);
INSERT INTO `system_user` VALUES (40, 'obstet_dir1', '8d969eef6ecad3c29a3a629280e686cf0c3f5d5a86aff3ca12020c923adc6c92', 2, '2025-07-29 21:36:13', 'obstet_dir1@qq.com', NULL, 0, 1, 0);
INSERT INTO `system_user` VALUES (41, 'obstet_dir2', '8d969eef6ecad3c29a3a629280e686cf0c3f5d5a86aff3ca12020c923adc6c92', 2, '2025-07-29 21:36:13', 'obstet_dir2@qq.com', NULL, 0, 1, 0);
INSERT INTO `system_user` VALUES (42, 'obstet_dir3', '8d969eef6ecad3c29a3a629280e686cf0c3f5d5a86aff3ca12020c923adc6c92', 2, '2025-07-29 21:36:13', 'obstet_dir3@qq.com', NULL, 0, 1, 0);
INSERT INTO `system_user` VALUES (43, 'obstet_dir4', '8d969eef6ecad3c29a3a629280e686cf0c3f5d5a86aff3ca12020c923adc6c92', 2, '2025-07-29 21:36:13', 'obstet_dir4@qq.com', NULL, 0, 1, 0);
INSERT INTO `system_user` VALUES (44, 'neonat_spec', '8d969eef6ecad3c29a3a629280e686cf0c3f5d5a86aff3ca12020c923adc6c92', 2, '2025-07-29 21:36:13', 'neonat_spec@qq.com', NULL, 0, 1, 0);
INSERT INTO `system_user` VALUES (45, 'neonat_dir1', '8d969eef6ecad3c29a3a629280e686cf0c3f5d5a86aff3ca12020c923adc6c92', 2, '2025-07-29 21:36:13', 'neonat_dir1@qq.com', NULL, 0, 1, 0);
INSERT INTO `system_user` VALUES (46, 'neonat_dir2', '8d969eef6ecad3c29a3a629280e686cf0c3f5d5a86aff3ca12020c923adc6c92', 2, '2025-07-29 21:36:13', 'neonat_dir2@qq.com', NULL, 0, 1, 0);
INSERT INTO `system_user` VALUES (47, 'neonat_dir3', '8d969eef6ecad3c29a3a629280e686cf0c3f5d5a86aff3ca12020c923adc6c92', 2, '2025-07-29 21:36:13', 'neonat_dir3@qq.com', NULL, 0, 1, 0);
INSERT INTO `system_user` VALUES (48, 'neonat_dir4', '8d969eef6ecad3c29a3a629280e686cf0c3f5d5a86aff3ca12020c923adc6c92', 2, '2025-07-29 21:36:13', 'neonat_dir4@qq.com', NULL, 0, 1, 0);
INSERT INTO `system_user` VALUES (49, 'pedsmed_spec', '8d969eef6ecad3c29a3a629280e686cf0c3f5d5a86aff3ca12020c923adc6c92', 2, '2025-07-29 21:36:13', 'pedsmed_spec@qq.com', NULL, 0, 1, 0);
INSERT INTO `system_user` VALUES (50, 'pedsmed_dir1', '8d969eef6ecad3c29a3a629280e686cf0c3f5d5a86aff3ca12020c923adc6c92', 2, '2025-07-29 21:36:13', 'pedsmed_dir1@qq.com', NULL, 0, 1, 0);
INSERT INTO `system_user` VALUES (51, 'pedsmed_dir2', '8d969eef6ecad3c29a3a629280e686cf0c3f5d5a86aff3ca12020c923adc6c92', 2, '2025-07-29 21:36:13', 'pedsmed_dir2@qq.com', NULL, 0, 1, 0);
INSERT INTO `system_user` VALUES (52, 'pedsmed_dir3', '8d969eef6ecad3c29a3a629280e686cf0c3f5d5a86aff3ca12020c923adc6c92', 2, '2025-07-29 21:36:13', 'pedsmed_dir3@qq.com', NULL, 0, 1, 0);
INSERT INTO `system_user` VALUES (53, 'pedsmed_dir4', '8d969eef6ecad3c29a3a629280e686cf0c3f5d5a86aff3ca12020c923adc6c92', 2, '2025-07-29 21:36:13', 'pedsmed_dir4@qq.com', NULL, 0, 1, 0);
INSERT INTO `system_user` VALUES (54, 'pedssurg_spec', '8d969eef6ecad3c29a3a629280e686cf0c3f5d5a86aff3ca12020c923adc6c92', 2, '2025-07-29 21:36:13', 'pedssurg_spec@qq.com', NULL, 0, 1, 0);
INSERT INTO `system_user` VALUES (55, 'pedssurg_dir1', '8d969eef6ecad3c29a3a629280e686cf0c3f5d5a86aff3ca12020c923adc6c92', 2, '2025-07-29 21:36:13', 'pedssurg_dir1@qq.com', NULL, 0, 1, 0);
INSERT INTO `system_user` VALUES (56, 'pedssurg_dir2', '8d969eef6ecad3c29a3a629280e686cf0c3f5d5a86aff3ca12020c923adc6c92', 2, '2025-07-29 21:36:13', 'pedssurg_dir2@qq.com', NULL, 0, 1, 0);
INSERT INTO `system_user` VALUES (57, 'pedssurg_dir3', '8d969eef6ecad3c29a3a629280e686cf0c3f5d5a86aff3ca12020c923adc6c92', 2, '2025-07-29 21:36:13', 'pedssurg_dir3@qq.com', NULL, 0, 1, 0);
INSERT INTO `system_user` VALUES (58, 'pedssurg_dir4', '8d969eef6ecad3c29a3a629280e686cf0c3f5d5a86aff3ca12020c923adc6c92', 2, '2025-07-29 21:36:13', 'pedssurg_dir4@qq.com', NULL, 0, 1, 0);
INSERT INTO `system_user` VALUES (59, 'refract_spec', '8d969eef6ecad3c29a3a629280e686cf0c3f5d5a86aff3ca12020c923adc6c92', 2, '2025-07-29 21:36:13', 'refract_spec@qq.com', NULL, 0, 1, 0);
INSERT INTO `system_user` VALUES (60, 'refract_dir1', '8d969eef6ecad3c29a3a629280e686cf0c3f5d5a86aff3ca12020c923adc6c92', 2, '2025-07-29 21:36:13', 'refract_dir1@qq.com', NULL, 0, 1, 0);
INSERT INTO `system_user` VALUES (61, 'refract_dir2', '8d969eef6ecad3c29a3a629280e686cf0c3f5d5a86aff3ca12020c923adc6c92', 2, '2025-07-29 21:36:13', 'refract_dir2@qq.com', NULL, 0, 1, 0);
INSERT INTO `system_user` VALUES (62, 'refract_dir3', '8d969eef6ecad3c29a3a629280e686cf0c3f5d5a86aff3ca12020c923adc6c92', 2, '2025-07-29 21:36:13', 'refract_dir3@qq.com', NULL, 0, 1, 0);
INSERT INTO `system_user` VALUES (63, 'refract_dir4', '8d969eef6ecad3c29a3a629280e686cf0c3f5d5a86aff3ca12020c923adc6c92', 2, '2025-07-29 21:36:13', 'refract_dir4@qq.com', NULL, 0, 1, 0);
INSERT INTO `system_user` VALUES (64, 'retina_spec', '8d969eef6ecad3c29a3a629280e686cf0c3f5d5a86aff3ca12020c923adc6c92', 2, '2025-07-29 21:36:13', 'retina_spec@qq.com', NULL, 0, 1, 0);
INSERT INTO `system_user` VALUES (65, 'retina_dir1', '8d969eef6ecad3c29a3a629280e686cf0c3f5d5a86aff3ca12020c923adc6c92', 2, '2025-07-29 21:36:13', 'retina_dir1@qq.com', NULL, 0, 1, 0);
INSERT INTO `system_user` VALUES (66, 'retina_dir2', '8d969eef6ecad3c29a3a629280e686cf0c3f5d5a86aff3ca12020c923adc6c92', 2, '2025-07-29 21:36:13', 'retina_dir2@qq.com', NULL, 0, 1, 0);
INSERT INTO `system_user` VALUES (67, 'retina_dir3', '8d969eef6ecad3c29a3a629280e686cf0c3f5d5a86aff3ca12020c923adc6c92', 2, '2025-07-29 21:36:13', 'retina_dir3@qq.com', NULL, 0, 1, 0);
INSERT INTO `system_user` VALUES (68, 'retina_dir4', '8d969eef6ecad3c29a3a629280e686cf0c3f5d5a86aff3ca12020c923adc6c92', 2, '2025-07-29 21:36:13', 'retina_dir4@qq.com', NULL, 0, 1, 0);
INSERT INTO `system_user` VALUES (69, 'zrtest', '8d969eef6ecad3c29a3a629280e686cf0c3f5d5a86aff3ca12020c923adc6c92', 1, '2025-07-29 23:07:24', '2749480940@qq.com', '2025-07-30 21:33:02', 1, 1, 0);
INSERT INTO `system_user` VALUES (76, 'doctor1', '8d969eef6ecad3c29a3a629280e686cf0c3f5d5a86aff3ca12020c923adc6c92', 2, '2025-07-30 02:02:05', '23333@qq.com', '2025-07-30 21:38:37', 0, 1, 0);
INSERT INTO `system_user` VALUES (79, 'doctor2', '8d969eef6ecad3c29a3a629280e686cf0c3f5d5a86aff3ca12020c923adc6c92', 2, '2025-07-30 09:54:35', '23333@qq.com', '2025-07-30 10:01:56', 0, 1, 1);
INSERT INTO `system_user` VALUES (80, 'user2', '8d969eef6ecad3c29a3a629280e686cf0c3f5d5a86aff3ca12020c923adc6c92', 1, '2025-08-04 18:01:21', '267932764@qq.com', '2025-08-08 13:59:52', 1, 1, 0);
INSERT INTO `system_user` VALUES (81, 'test01', '8d969eef6ecad3c29a3a629280e686cf0c3f5d5a86aff3ca12020c923adc6c92', 1, '2025-08-08 14:08:17', '1654752365@163.com', NULL, 1, 1, 0);

-- ----------------------------
-- Table structure for doctor_detail
-- ----------------------------
DROP TABLE IF EXISTS `doctor_detail`;
CREATE TABLE `doctor_detail`  (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `system_user_id` bigint NOT NULL,
  `real_name` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL,
  `id_card` varchar(18) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL,
  `create_time` timestamp NULL DEFAULT CURRENT_TIMESTAMP,
  `update_time` timestamp NULL DEFAULT NULL ON UPDATE CURRENT_TIMESTAMP,
  `title` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL,
  `sub_department_id` bigint NOT NULL,
  `professional_license_number` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL,
  `introduction` text CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL,
  `is_deleted` int NULL DEFAULT 0,
  `price` decimal(5, 2) NULL DEFAULT NULL,
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `system_user_id`(`system_user_id` ASC) USING BTREE,
  INDEX `department_id`(`sub_department_id` ASC) USING BTREE,
  CONSTRAINT `doctor_detail_ibfk` FOREIGN KEY (`sub_department_id`) REFERENCES `sub_department` (`id`) ON DELETE RESTRICT ON UPDATE RESTRICT,
  CONSTRAINT `doctor_detail_ibfk_1` FOREIGN KEY (`system_user_id`) REFERENCES `system_user` (`id`) ON DELETE RESTRICT ON UPDATE RESTRICT
) ENGINE = InnoDB AUTO_INCREMENT = 199 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci ROW_FORMAT = DYNAMIC;

-- ----------------------------
-- Records of doctor_detail
-- ----------------------------
INSERT INTO `doctor_detail` VALUES (131, 4, '张心明', '110101198001011234', '2025-07-29 22:22:43', '2025-07-30 01:26:25', '专家', 1, 'PL110101001', '心血管内科专家，擅长冠心病介入治疗，完成手术3000余例。', 0, 100.00);
INSERT INTO `doctor_detail` VALUES (132, 5, '李国华', '110102198102022345', '2025-07-29 22:22:43', NULL, '主任医师', 1, 'PL110102002', '主任医师，心血管疾病诊疗经验丰富，擅长心力衰竭治疗。', 0, 30.00);
INSERT INTO `doctor_detail` VALUES (133, 6, '王雪梅', '110103198203033456', '2025-07-29 22:22:43', NULL, '主任医师', 1, 'PL110103003', '主任医师，高血压病防治专家，发表SCI论文20余篇。', 0, 30.00);
INSERT INTO `doctor_detail` VALUES (134, 7, '赵立伟', '110104198304044567', '2025-07-29 22:22:43', NULL, '主任医师', 1, 'PL110104004', '主任医师，心律失常诊疗专家，擅长射频消融手术。', 0, 30.00);
INSERT INTO `doctor_detail` VALUES (135, 8, '陈晓芳', '110105198405055678', '2025-07-29 22:22:43', NULL, '主任医师', 1, 'PL110105005', '主任医师，心脏康复领域专家，制定个性化康复方案。', 0, 30.00);
INSERT INTO `doctor_detail` VALUES (136, 9, '刘振华', '110106198506066789', '2025-07-29 22:22:43', NULL, '专家', 2, 'PL110106006', '呼吸内科专家，擅长重症肺炎和呼吸衰竭救治。', 0, 100.00);
INSERT INTO `doctor_detail` VALUES (137, 10, '杨晓红', '110107198607077890', '2025-07-29 22:22:43', NULL, '主任医师', 2, 'PL110107007', '主任医师，慢性阻塞性肺疾病诊疗专家。', 0, 30.00);
INSERT INTO `doctor_detail` VALUES (138, 11, '周建国', '110108198708088901', '2025-07-29 22:22:43', NULL, '主任医师', 2, 'PL110108008', '主任医师，肺癌早期诊断和治疗专家。', 0, 30.00);
INSERT INTO `doctor_detail` VALUES (139, 12, '吴丽娜', '110109198809099012', '2025-07-29 22:22:43', NULL, '主任医师', 2, 'PL110109009', '主任医师，支气管哮喘诊疗专家，擅长难治性哮喘。', 0, 30.00);
INSERT INTO `doctor_detail` VALUES (140, 13, '郑海洋', '110110198910101123', '2025-07-29 22:22:43', NULL, '主任医师', 2, 'PL110110010', '主任医师，肺间质疾病诊疗专家，开展多学科诊疗。', 0, 30.00);
INSERT INTO `doctor_detail` VALUES (141, 14, '钱卫东', '110111199011112234', '2025-07-29 22:22:43', NULL, '专家', 3, 'PL110111011', '消化内科专家，擅长ERCP和内镜下微创治疗。', 0, 100.00);
INSERT INTO `doctor_detail` VALUES (142, 15, '孙雅静', '110112199112123345', '2025-07-29 22:22:43', NULL, '主任医师', 3, 'PL110112012', '主任医师，炎症性肠病诊疗专家，制定个体化方案。', 0, 30.00);
INSERT INTO `doctor_detail` VALUES (143, 16, '朱志强', '110113199213134456', '2025-07-29 22:22:43', NULL, '主任医师', 3, 'PL110113013', '主任医师，肝硬化并发症治疗专家，开展TIPSS手术。', 0, 30.00);
INSERT INTO `doctor_detail` VALUES (144, 17, '马玉华', '110114199314145567', '2025-07-29 22:22:43', NULL, '主任医师', 3, 'PL110114014', '主任医师，胃肠功能性疾病诊疗专家。', 0, 30.00);
INSERT INTO `doctor_detail` VALUES (145, 18, '林国栋', '110115199415156678', '2025-07-29 22:22:43', NULL, '主任医师', 3, 'PL110115015', '主任医师，胰腺疾病诊疗专家，擅长EUS引导治疗。', 0, 30.00);
INSERT INTO `doctor_detail` VALUES (146, 19, '黄伟明', '110116199516167789', '2025-07-29 22:22:43', NULL, '专家', 4, 'PL110116016', '骨科专家，擅长微创关节置换手术，完成2000余例。', 0, 100.00);
INSERT INTO `doctor_detail` VALUES (147, 20, '徐丽娟', '110117199617178890', '2025-07-29 22:22:43', NULL, '主任医师', 4, 'PL110117017', '主任医师，脊柱微创手术专家，开展椎间孔镜技术。', 0, 30.00);
INSERT INTO `doctor_detail` VALUES (148, 21, '高建国', '110118199718189901', '2025-07-29 22:22:43', NULL, '主任医师', 4, 'PL110118018', '主任医师，创伤骨科专家，复杂骨折手术治疗。', 0, 30.00);
INSERT INTO `doctor_detail` VALUES (149, 22, '胡晓燕', '110119199819191012', '2025-07-29 22:22:43', NULL, '主任医师', 4, 'PL110119019', '主任医师，运动医学专家，关节镜手术经验丰富。', 0, 30.00);
INSERT INTO `doctor_detail` VALUES (150, 23, '欧阳峰', '110120199920201123', '2025-07-29 22:22:43', NULL, '主任医师', 4, 'PL110120020', '主任医师，骨肿瘤诊疗专家，保肢手术成功率95%。', 0, 30.00);
INSERT INTO `doctor_detail` VALUES (151, 24, '曹志强', '110121200021212234', '2025-07-29 22:22:43', NULL, '专家', 5, 'PL110121021', '神经外科专家，擅长脑肿瘤显微手术，成功率98%。', 0, 100.00);
INSERT INTO `doctor_detail` VALUES (152, 25, '薛文静', '110122200122223345', '2025-07-29 22:22:43', NULL, '主任医师', 5, 'PL110122022', '主任医师，脑血管病专家，开展介入栓塞治疗。', 0, 30.00);
INSERT INTO `doctor_detail` VALUES (153, 26, '彭建国', '110123200223234456', '2025-07-29 22:22:43', NULL, '主任医师', 5, 'PL110123023', '主任医师，功能神经外科专家，帕金森DBS手术。', 0, 30.00);
INSERT INTO `doctor_detail` VALUES (154, 27, '曾丽华', '110124200324245567', '2025-07-29 22:22:43', NULL, '主任医师', 5, 'PL110124024', '主任医师，脊髓脊柱外科专家，微创手术经验丰富。', 0, 30.00);
INSERT INTO `doctor_detail` VALUES (155, 28, '苏振华', '110125200425256678', '2025-07-29 22:22:43', NULL, '主任医师', 5, 'PL110125025', '主任医师，颅脑创伤救治专家，重症监护经验丰富。', 0, 30.00);
INSERT INTO `doctor_detail` VALUES (156, 29, '董卫东', '110126200526267789', '2025-07-29 22:22:43', NULL, '专家', 6, 'PL110126026', '普外科专家，擅长腹腔镜胃癌根治术，完成1500例。', 0, 100.00);
INSERT INTO `doctor_detail` VALUES (157, 30, '梁玉梅', '110127200627278890', '2025-07-29 22:22:43', NULL, '主任医师', 6, 'PL110127027', '主任医师，乳腺疾病专家，开展保乳手术和重建。', 0, 30.00);
INSERT INTO `doctor_detail` VALUES (158, 31, '谢国华', '110128200728289901', '2025-07-29 22:22:43', NULL, '主任医师', 6, 'PL110128028', '主任医师，肝胆外科专家，腹腔镜肝切除技术领先。', 0, 30.00);
INSERT INTO `doctor_detail` VALUES (159, 32, '宋丽萍', '110129200829291012', '2025-07-29 22:22:43', NULL, '主任医师', 6, 'PL110129029', '主任医师，甲状腺疾病专家，纳米炭示踪技术应用。', 0, 30.00);
INSERT INTO `doctor_detail` VALUES (160, 33, '唐志强', '110130200930301123', '2025-07-29 22:22:43', NULL, '主任医师', 6, 'PL110130030', '主任医师，疝与腹壁外科专家，开展TAPP/TEP手术。', 0, 30.00);
INSERT INTO `doctor_detail` VALUES (161, 34, '韩雪梅', '110131201031312234', '2025-07-29 22:22:43', NULL, '专家', 7, 'PL110131031', '妇科专家，擅长宫腹腔镜联合手术，微创治疗。', 0, 100.00);
INSERT INTO `doctor_detail` VALUES (162, 35, '于丽华', '110132201132323345', '2025-07-29 22:22:43', NULL, '主任医师', 7, 'PL110132032', '主任医师，妇科肿瘤专家，开展精准化综合治疗。', 0, 30.00);
INSERT INTO `doctor_detail` VALUES (163, 36, '傅晓燕', '110133201233334456', '2025-07-29 22:22:43', NULL, '主任医师', 7, 'PL110133033', '主任医师，盆底功能障碍专家，康复和手术结合。', 0, 30.00);
INSERT INTO `doctor_detail` VALUES (164, 37, '崔玉珍', '110134201334345567', '2025-07-29 22:22:43', NULL, '主任医师', 7, 'PL110134034', '主任医师，生殖内分泌专家，不孕不育诊疗。', 0, 30.00);
INSERT INTO `doctor_detail` VALUES (165, 38, '姜丽娜', '110135201435356678', '2025-07-29 22:22:43', NULL, '主任医师', 7, 'PL110135035', '主任医师，妇科感染性疾病专家，精准抗感染治疗。', 0, 30.00);
INSERT INTO `doctor_detail` VALUES (166, 39, '蔡文静', '110136201536367789', '2025-07-29 22:22:43', NULL, '专家', 8, 'PL110136036', '产科专家，高危妊娠管理，成功救治危重孕产妇。', 0, 100.00);
INSERT INTO `doctor_detail` VALUES (167, 40, '潘晓红', '110137201637378890', '2025-07-29 22:22:43', NULL, '主任医师', 8, 'PL110137037', '主任医师，产前诊断专家，遗传咨询和介入诊断。', 0, 30.00);
INSERT INTO `doctor_detail` VALUES (168, 41, '钟玉华', '110138201738389901', '2025-07-29 22:22:43', NULL, '主任医师', 8, 'PL110138038', '主任医师，分娩镇痛专家，开展全程无痛分娩。', 0, 30.00);
INSERT INTO `doctor_detail` VALUES (169, 42, '田丽娟', '110139201839391012', '2025-07-29 22:22:43', NULL, '主任医师', 8, 'PL110139039', '主任医师，产后康复专家，盆底肌修复方案制定。', 0, 30.00);
INSERT INTO `doctor_detail` VALUES (170, 43, '杜小燕', '110140201940401123', '2025-07-29 22:22:43', NULL, '主任医师', 8, 'PL110140040', '主任医师，妊娠合并症专家，内分泌疾病管理。', 0, 30.00);
INSERT INTO `doctor_detail` VALUES (171, 44, '魏国栋', '110141202041412234', '2025-07-29 22:22:43', NULL, '专家', 9, 'PL110141041', '新生儿科专家，NICU主任，超早产儿救治成功率90%。', 0, 100.00);
INSERT INTO `doctor_detail` VALUES (172, 45, '蒋丽萍', '110142202142423345', '2025-07-29 22:22:43', NULL, '主任医师', 9, 'PL110142042', '主任医师，新生儿呼吸疾病专家，高频通气技术。', 0, 30.00);
INSERT INTO `doctor_detail` VALUES (173, 46, '沈志强', '110143202243434456', '2025-07-29 22:22:43', NULL, '主任医师', 9, 'PL110143043', '主任医师，新生儿神经重症专家，脑功能监测。', 0, 30.00);
INSERT INTO `doctor_detail` VALUES (174, 47, '范晓华', '110144202344445567', '2025-07-29 22:22:43', NULL, '主任医师', 9, 'PL110144044', '主任医师，新生儿感染专家，抗生素合理应用。', 0, 30.00);
INSERT INTO `doctor_detail` VALUES (175, 48, '龙玉梅', '110145202445456678', '2025-07-29 22:22:43', NULL, '主任医师', 9, 'PL110145045', '主任医师，新生儿营养专家，早产儿喂养方案制定。', 0, 30.00);
INSERT INTO `doctor_detail` VALUES (176, 49, '孟建国', '110146202546467789', '2025-07-29 22:22:43', NULL, '专家', 10, 'PL110146046', '小儿内科专家，儿童哮喘中心主任，制定标准化方案。', 0, 100.00);
INSERT INTO `doctor_detail` VALUES (177, 50, '方丽华', '110147202647478890', '2025-07-29 22:22:43', NULL, '主任医师', 10, 'PL110147047', '主任医师，儿童消化疾病专家，胃肠镜检查和治疗。', 0, 30.00);
INSERT INTO `doctor_detail` VALUES (178, 51, '石志伟', '110148202748489901', '2025-07-29 22:22:43', NULL, '主任医师', 10, 'PL110148048', '主任医师，儿童肾病专家，开展肾活检和免疫治疗。', 0, 30.00);
INSERT INTO `doctor_detail` VALUES (179, 52, '姚晓燕', '110149202849491012', '2025-07-29 22:22:43', NULL, '主任医师', 10, 'PL110149049', '主任医师，儿童血液肿瘤专家，精准化疗方案。', 0, 30.00);
INSERT INTO `doctor_detail` VALUES (180, 53, '毛玉珍', '110150202950501123', '2025-07-29 22:22:43', NULL, '主任医师', 10, 'PL110150050', '主任医师，儿童内分泌专家，生长激素治疗。', 0, 30.00);
INSERT INTO `doctor_detail` VALUES (181, 54, '江伟明', '110151203051512234', '2025-07-29 22:22:43', NULL, '专家', 11, 'PL110151051', '小儿外科专家，微创手术中心主任，腹腔镜技术领先。', 0, 100.00);
INSERT INTO `doctor_detail` VALUES (182, 55, '卢小娟', '110152203152523345', '2025-07-29 22:22:43', NULL, '主任医师', 11, 'PL110152052', '主任医师，新生儿外科专家，先天畸形矫正手术。', 0, 30.00);
INSERT INTO `doctor_detail` VALUES (183, 56, '丁国华', '110153203253534456', '2025-07-29 22:22:43', NULL, '主任医师', 11, 'PL110153053', '主任医师，小儿泌尿外科专家，尿道下裂修复。', 0, 30.00);
INSERT INTO `doctor_detail` VALUES (184, 57, '文丽娜', '110154203354545567', '2025-07-29 22:22:43', NULL, '主任医师', 11, 'PL110154054', '主任医师，小儿肿瘤外科专家，实体瘤切除手术。', 0, 30.00);
INSERT INTO `doctor_detail` VALUES (185, 58, '龚志强', '110155203455556678', '2025-07-29 22:22:43', NULL, '主任医师', 11, 'PL110155055', '主任医师，小儿骨科专家，发育性髋关节脱位治疗。', 0, 30.00);
INSERT INTO `doctor_detail` VALUES (186, 59, '雷晓明', '110156203556567789', '2025-07-29 22:22:43', NULL, '专家', 12, 'PL110156056', '屈光矫正专家，全飞秒手术中心主任，完成万例手术。', 0, 100.00);
INSERT INTO `doctor_detail` VALUES (187, 60, '白丽华', '110157203657578890', '2025-07-29 22:22:43', NULL, '主任医师', 12, 'PL110157057', '主任医师，高度近视矫正专家，ICL手术经验丰富。', 0, 30.00);
INSERT INTO `doctor_detail` VALUES (188, 61, '陆志伟', '110158203758589901', '2025-07-29 22:22:43', NULL, '主任医师', 12, 'PL110158058', '主任医师，老视矫正专家，三焦点晶体植入。', 0, 30.00);
INSERT INTO `doctor_detail` VALUES (189, 62, '肖玉珍', '110159203859591012', '2025-07-29 22:22:43', NULL, '主任医师', 12, 'PL110159059', '主任医师，角膜屈光手术专家，个性化切削方案。', 0, 30.00);
INSERT INTO `doctor_detail` VALUES (190, 63, '侯小燕', '110160203960601123', '2025-07-29 22:22:43', NULL, '主任医师', 12, 'PL110160060', '主任医师，青少年近视防控专家，角膜塑形镜验配。', 0, 30.00);
INSERT INTO `doctor_detail` VALUES (191, 64, '谭卫东', '110161204061612234', '2025-07-29 22:22:43', NULL, '专家', 13, 'PL110161061', '眼底病专家，OCT影像诊断中心主任，黄斑疾病权威。', 0, 100.00);
INSERT INTO `doctor_detail` VALUES (192, 65, '黎丽萍', '110162204162623345', '2025-07-29 22:22:43', NULL, '主任医师', 13, 'PL110162062', '主任医师，糖尿病视网膜病变专家，抗VEGF治疗。', 0, 30.00);
INSERT INTO `doctor_detail` VALUES (193, 66, '秦建国', '110163204263634456', '2025-07-29 22:22:43', NULL, '主任医师', 13, 'PL110163063', '主任医师，视网膜脱离专家，微创玻璃体切割手术。', 0, 30.00);
INSERT INTO `doctor_detail` VALUES (194, 67, '史晓红', '110164204364645567', '2025-07-29 22:22:43', NULL, '主任医师', 13, 'PL110164064', '主任医师，葡萄膜炎专家，免疫抑制剂个体化治疗。', 0, 30.00);
INSERT INTO `doctor_detail` VALUES (195, 68, '廖玉华', '110165204465656678', '2025-07-29 22:22:43', NULL, '主任医师', 13, 'PL110165065', '主任医师，遗传性视网膜疾病专家，基因诊断技术。', 0, 30.00);
INSERT INTO `doctor_detail` VALUES (197, 76, '张杨明', '510127197009294613', '2025-07-30 02:02:05', '2025-07-30 21:38:25', '专家', 6, 'PL110101200', '心血管内科专家，擅长冠心病介入治疗，完成手术4000余例。', 0, 100.00);
INSERT INTO `doctor_detail` VALUES (198, 79, '张杨明', '510127197009294613', '2025-07-30 09:54:36', '2025-07-30 10:01:56', '专家', 6, 'PL110101200', '心血管内科专家，擅长冠心病介入治疗，完成手术4000余例。', 1, 100.00);

-- ----------------------------
-- Table structure for schedule_template
-- ----------------------------
DROP TABLE IF EXISTS `schedule_template`;
CREATE TABLE `schedule_template`  (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `doctor_id` bigint NOT NULL,
  `week_day` int NOT NULL COMMENT '1-7(周一到周日)',
  `morning_limit` int NULL DEFAULT 0 COMMENT '上午预约上限',
  `afternoon_limit` int NULL DEFAULT 0 COMMENT '下午预约上限',
  `is_active` int NULL DEFAULT 1 COMMENT '是否生效',
  `create_time` timestamp NULL DEFAULT CURRENT_TIMESTAMP,
  `is_deleted` int NULL DEFAULT 0,
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `doctor_id`(`doctor_id` ASC) USING BTREE,
  CONSTRAINT `schedule_template_ibfk_1` FOREIGN KEY (`doctor_id`) REFERENCES `doctor_detail` (`id`) ON DELETE RESTRICT ON UPDATE RESTRICT
) ENGINE = InnoDB AUTO_INCREMENT = 257 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of schedule_template
-- ----------------------------
INSERT INTO `schedule_template` VALUES (1, 131, 3, 15, 10, 1, '2025-07-31 02:16:46', 0);
INSERT INTO `schedule_template` VALUES (2, 131, 1, 15, 10, 1, '2025-07-31 02:16:46', 0);
INSERT INTO `schedule_template` VALUES (3, 136, 3, 15, 10, 1, '2025-07-31 02:16:46', 0);
INSERT INTO `schedule_template` VALUES (4, 136, 1, 15, 10, 1, '2025-07-31 02:16:46', 0);
INSERT INTO `schedule_template` VALUES (5, 141, 3, 15, 10, 1, '2025-07-31 02:16:46', 0);
INSERT INTO `schedule_template` VALUES (6, 141, 1, 15, 10, 1, '2025-07-31 02:16:46', 0);
INSERT INTO `schedule_template` VALUES (7, 146, 3, 15, 10, 1, '2025-07-31 02:16:46', 0);
INSERT INTO `schedule_template` VALUES (8, 146, 1, 15, 10, 1, '2025-07-31 02:16:46', 0);
INSERT INTO `schedule_template` VALUES (9, 151, 3, 15, 10, 1, '2025-07-31 02:16:46', 0);
INSERT INTO `schedule_template` VALUES (10, 151, 1, 15, 10, 1, '2025-07-31 02:16:46', 0);
INSERT INTO `schedule_template` VALUES (11, 156, 3, 15, 10, 1, '2025-07-31 02:16:46', 0);
INSERT INTO `schedule_template` VALUES (12, 156, 1, 15, 10, 1, '2025-07-31 02:16:46', 0);
INSERT INTO `schedule_template` VALUES (13, 161, 3, 15, 10, 1, '2025-07-31 02:16:46', 0);
INSERT INTO `schedule_template` VALUES (14, 161, 1, 15, 10, 1, '2025-07-31 02:16:46', 0);
INSERT INTO `schedule_template` VALUES (15, 166, 3, 15, 10, 1, '2025-07-31 02:16:46', 0);
INSERT INTO `schedule_template` VALUES (16, 166, 1, 15, 10, 1, '2025-07-31 02:16:46', 0);
INSERT INTO `schedule_template` VALUES (17, 171, 3, 15, 10, 1, '2025-07-31 02:16:46', 0);
INSERT INTO `schedule_template` VALUES (18, 171, 1, 15, 10, 1, '2025-07-31 02:16:46', 0);
INSERT INTO `schedule_template` VALUES (19, 176, 3, 15, 10, 1, '2025-07-31 02:16:46', 0);
INSERT INTO `schedule_template` VALUES (20, 176, 1, 15, 10, 1, '2025-07-31 02:16:46', 0);
INSERT INTO `schedule_template` VALUES (21, 181, 3, 15, 10, 1, '2025-07-31 02:16:46', 0);
INSERT INTO `schedule_template` VALUES (22, 181, 1, 15, 10, 1, '2025-07-31 02:16:46', 0);
INSERT INTO `schedule_template` VALUES (23, 186, 3, 15, 10, 1, '2025-07-31 02:16:46', 0);
INSERT INTO `schedule_template` VALUES (24, 186, 1, 15, 10, 1, '2025-07-31 02:16:46', 0);
INSERT INTO `schedule_template` VALUES (25, 191, 3, 15, 10, 1, '2025-07-31 02:16:46', 0);
INSERT INTO `schedule_template` VALUES (26, 191, 1, 15, 10, 1, '2025-07-31 02:16:46', 0);
INSERT INTO `schedule_template` VALUES (27, 197, 3, 15, 10, 1, '2025-07-31 02:16:46', 0);
INSERT INTO `schedule_template` VALUES (28, 197, 1, 15, 10, 1, '2025-07-31 02:16:46', 0);
INSERT INTO `schedule_template` VALUES (29, 198, 3, 15, 10, 1, '2025-07-31 02:16:46', 0);
INSERT INTO `schedule_template` VALUES (30, 198, 1, 15, 10, 1, '2025-07-31 02:16:46', 0);
INSERT INTO `schedule_template` VALUES (31, 132, 5, 30, 20, 1, '2025-07-31 02:16:46', 0);
INSERT INTO `schedule_template` VALUES (32, 132, 4, 30, 20, 1, '2025-07-31 02:16:46', 0);
INSERT INTO `schedule_template` VALUES (33, 132, 2, 30, 20, 1, '2025-07-31 02:16:46', 0);
INSERT INTO `schedule_template` VALUES (34, 133, 5, 30, 20, 1, '2025-07-31 02:16:46', 0);
INSERT INTO `schedule_template` VALUES (35, 133, 4, 30, 20, 1, '2025-07-31 02:16:46', 0);
INSERT INTO `schedule_template` VALUES (36, 133, 2, 30, 20, 1, '2025-07-31 02:16:46', 0);
INSERT INTO `schedule_template` VALUES (37, 134, 5, 30, 20, 1, '2025-07-31 02:16:46', 0);
INSERT INTO `schedule_template` VALUES (38, 134, 4, 30, 20, 1, '2025-07-31 02:16:46', 0);
INSERT INTO `schedule_template` VALUES (39, 134, 2, 30, 20, 1, '2025-07-31 02:16:46', 0);
INSERT INTO `schedule_template` VALUES (40, 135, 5, 30, 20, 1, '2025-07-31 02:16:46', 0);
INSERT INTO `schedule_template` VALUES (41, 135, 4, 30, 20, 1, '2025-07-31 02:16:46', 0);
INSERT INTO `schedule_template` VALUES (42, 135, 2, 30, 20, 1, '2025-07-31 02:16:46', 0);
INSERT INTO `schedule_template` VALUES (43, 137, 5, 30, 20, 1, '2025-07-31 02:16:46', 0);
INSERT INTO `schedule_template` VALUES (44, 137, 4, 30, 20, 1, '2025-07-31 02:16:46', 0);
INSERT INTO `schedule_template` VALUES (45, 137, 2, 30, 20, 1, '2025-07-31 02:16:46', 0);
INSERT INTO `schedule_template` VALUES (46, 138, 5, 30, 20, 1, '2025-07-31 02:16:46', 0);
INSERT INTO `schedule_template` VALUES (47, 138, 4, 30, 20, 1, '2025-07-31 02:16:46', 0);
INSERT INTO `schedule_template` VALUES (48, 138, 2, 30, 20, 1, '2025-07-31 02:16:46', 0);
INSERT INTO `schedule_template` VALUES (49, 139, 5, 30, 20, 1, '2025-07-31 02:16:46', 0);
INSERT INTO `schedule_template` VALUES (50, 139, 4, 30, 20, 1, '2025-07-31 02:16:46', 0);
INSERT INTO `schedule_template` VALUES (51, 139, 2, 30, 20, 1, '2025-07-31 02:16:46', 0);
INSERT INTO `schedule_template` VALUES (52, 140, 5, 30, 20, 1, '2025-07-31 02:16:46', 0);
INSERT INTO `schedule_template` VALUES (53, 140, 4, 30, 20, 1, '2025-07-31 02:16:46', 0);
INSERT INTO `schedule_template` VALUES (54, 140, 2, 30, 20, 1, '2025-07-31 02:16:46', 0);
INSERT INTO `schedule_template` VALUES (55, 142, 5, 30, 20, 1, '2025-07-31 02:16:46', 0);
INSERT INTO `schedule_template` VALUES (56, 142, 4, 30, 20, 1, '2025-07-31 02:16:46', 0);
INSERT INTO `schedule_template` VALUES (57, 142, 2, 30, 20, 1, '2025-07-31 02:16:46', 0);
INSERT INTO `schedule_template` VALUES (58, 143, 5, 30, 20, 1, '2025-07-31 02:16:46', 0);
INSERT INTO `schedule_template` VALUES (59, 143, 4, 30, 20, 1, '2025-07-31 02:16:46', 0);
INSERT INTO `schedule_template` VALUES (60, 143, 2, 30, 20, 1, '2025-07-31 02:16:46', 0);
INSERT INTO `schedule_template` VALUES (61, 144, 5, 30, 20, 1, '2025-07-31 02:16:46', 0);
INSERT INTO `schedule_template` VALUES (62, 144, 4, 30, 20, 1, '2025-07-31 02:16:46', 0);
INSERT INTO `schedule_template` VALUES (63, 144, 2, 30, 20, 1, '2025-07-31 02:16:46', 0);
INSERT INTO `schedule_template` VALUES (64, 145, 5, 30, 20, 1, '2025-07-31 02:16:46', 0);
INSERT INTO `schedule_template` VALUES (65, 145, 4, 30, 20, 1, '2025-07-31 02:16:46', 0);
INSERT INTO `schedule_template` VALUES (66, 145, 2, 30, 20, 1, '2025-07-31 02:16:46', 0);
INSERT INTO `schedule_template` VALUES (67, 147, 5, 30, 20, 1, '2025-07-31 02:16:46', 0);
INSERT INTO `schedule_template` VALUES (68, 147, 4, 30, 20, 1, '2025-07-31 02:16:46', 0);
INSERT INTO `schedule_template` VALUES (69, 147, 2, 30, 20, 1, '2025-07-31 02:16:46', 0);
INSERT INTO `schedule_template` VALUES (70, 148, 5, 30, 20, 1, '2025-07-31 02:16:46', 0);
INSERT INTO `schedule_template` VALUES (71, 148, 4, 30, 20, 1, '2025-07-31 02:16:46', 0);
INSERT INTO `schedule_template` VALUES (72, 148, 2, 30, 20, 1, '2025-07-31 02:16:46', 0);
INSERT INTO `schedule_template` VALUES (73, 149, 5, 30, 20, 1, '2025-07-31 02:16:46', 0);
INSERT INTO `schedule_template` VALUES (74, 149, 4, 30, 20, 1, '2025-07-31 02:16:46', 0);
INSERT INTO `schedule_template` VALUES (75, 149, 2, 30, 20, 1, '2025-07-31 02:16:46', 0);
INSERT INTO `schedule_template` VALUES (76, 150, 5, 30, 20, 1, '2025-07-31 02:16:46', 0);
INSERT INTO `schedule_template` VALUES (77, 150, 4, 30, 20, 1, '2025-07-31 02:16:46', 0);
INSERT INTO `schedule_template` VALUES (78, 150, 2, 30, 20, 1, '2025-07-31 02:16:46', 0);
INSERT INTO `schedule_template` VALUES (79, 152, 5, 30, 20, 1, '2025-07-31 02:16:46', 0);
INSERT INTO `schedule_template` VALUES (80, 152, 4, 30, 20, 1, '2025-07-31 02:16:46', 0);
INSERT INTO `schedule_template` VALUES (81, 152, 2, 30, 20, 1, '2025-07-31 02:16:46', 0);
INSERT INTO `schedule_template` VALUES (82, 153, 5, 30, 20, 1, '2025-07-31 02:16:46', 0);
INSERT INTO `schedule_template` VALUES (83, 153, 4, 30, 20, 1, '2025-07-31 02:16:46', 0);
INSERT INTO `schedule_template` VALUES (84, 153, 2, 30, 20, 1, '2025-07-31 02:16:46', 0);
INSERT INTO `schedule_template` VALUES (85, 154, 5, 30, 20, 1, '2025-07-31 02:16:46', 0);
INSERT INTO `schedule_template` VALUES (86, 154, 4, 30, 20, 1, '2025-07-31 02:16:46', 0);
INSERT INTO `schedule_template` VALUES (87, 154, 2, 30, 20, 1, '2025-07-31 02:16:46', 0);
INSERT INTO `schedule_template` VALUES (88, 155, 5, 30, 20, 1, '2025-07-31 02:16:46', 0);
INSERT INTO `schedule_template` VALUES (89, 155, 4, 30, 20, 1, '2025-07-31 02:16:46', 0);
INSERT INTO `schedule_template` VALUES (90, 155, 2, 30, 20, 1, '2025-07-31 02:16:46', 0);
INSERT INTO `schedule_template` VALUES (91, 157, 5, 30, 20, 1, '2025-07-31 02:16:46', 0);
INSERT INTO `schedule_template` VALUES (92, 157, 4, 30, 20, 1, '2025-07-31 02:16:46', 0);
INSERT INTO `schedule_template` VALUES (93, 157, 2, 30, 20, 1, '2025-07-31 02:16:46', 0);
INSERT INTO `schedule_template` VALUES (94, 158, 5, 30, 20, 1, '2025-07-31 02:16:46', 0);
INSERT INTO `schedule_template` VALUES (95, 158, 4, 30, 20, 1, '2025-07-31 02:16:46', 0);
INSERT INTO `schedule_template` VALUES (96, 158, 2, 30, 20, 1, '2025-07-31 02:16:46', 0);
INSERT INTO `schedule_template` VALUES (97, 159, 5, 30, 20, 1, '2025-07-31 02:16:46', 0);
INSERT INTO `schedule_template` VALUES (98, 159, 4, 30, 20, 1, '2025-07-31 02:16:46', 0);
INSERT INTO `schedule_template` VALUES (99, 159, 2, 30, 20, 1, '2025-07-31 02:16:46', 0);
INSERT INTO `schedule_template` VALUES (100, 160, 5, 30, 20, 1, '2025-07-31 02:16:46', 0);
INSERT INTO `schedule_template` VALUES (101, 160, 4, 30, 20, 1, '2025-07-31 02:16:46', 0);
INSERT INTO `schedule_template` VALUES (102, 160, 2, 30, 20, 1, '2025-07-31 02:16:46', 0);
INSERT INTO `schedule_template` VALUES (103, 162, 5, 30, 20, 1, '2025-07-31 02:16:46', 0);
INSERT INTO `schedule_template` VALUES (104, 162, 4, 30, 20, 1, '2025-07-31 02:16:46', 0);
INSERT INTO `schedule_template` VALUES (105, 162, 2, 30, 20, 1, '2025-07-31 02:16:46', 0);
INSERT INTO `schedule_template` VALUES (106, 163, 5, 30, 20, 1, '2025-07-31 02:16:46', 0);
INSERT INTO `schedule_template` VALUES (107, 163, 4, 30, 20, 1, '2025-07-31 02:16:46', 0);
INSERT INTO `schedule_template` VALUES (108, 163, 2, 30, 20, 1, '2025-07-31 02:16:46', 0);
INSERT INTO `schedule_template` VALUES (109, 164, 5, 30, 20, 1, '2025-07-31 02:16:46', 0);
INSERT INTO `schedule_template` VALUES (110, 164, 4, 30, 20, 1, '2025-07-31 02:16:46', 0);
INSERT INTO `schedule_template` VALUES (111, 164, 2, 30, 20, 1, '2025-07-31 02:16:46', 0);
INSERT INTO `schedule_template` VALUES (112, 165, 5, 30, 20, 1, '2025-07-31 02:16:46', 0);
INSERT INTO `schedule_template` VALUES (113, 165, 4, 30, 20, 1, '2025-07-31 02:16:46', 0);
INSERT INTO `schedule_template` VALUES (114, 165, 2, 30, 20, 1, '2025-07-31 02:16:46', 0);
INSERT INTO `schedule_template` VALUES (115, 167, 5, 30, 20, 1, '2025-07-31 02:16:46', 0);
INSERT INTO `schedule_template` VALUES (116, 167, 4, 30, 20, 1, '2025-07-31 02:16:46', 0);
INSERT INTO `schedule_template` VALUES (117, 167, 2, 30, 20, 1, '2025-07-31 02:16:46', 0);
INSERT INTO `schedule_template` VALUES (118, 168, 5, 30, 20, 1, '2025-07-31 02:16:46', 0);
INSERT INTO `schedule_template` VALUES (119, 168, 4, 30, 20, 1, '2025-07-31 02:16:46', 0);
INSERT INTO `schedule_template` VALUES (120, 168, 2, 30, 20, 1, '2025-07-31 02:16:46', 0);
INSERT INTO `schedule_template` VALUES (121, 169, 5, 30, 20, 1, '2025-07-31 02:16:46', 0);
INSERT INTO `schedule_template` VALUES (122, 169, 4, 30, 20, 1, '2025-07-31 02:16:46', 0);
INSERT INTO `schedule_template` VALUES (123, 169, 2, 30, 20, 1, '2025-07-31 02:16:46', 0);
INSERT INTO `schedule_template` VALUES (124, 170, 5, 30, 20, 1, '2025-07-31 02:16:46', 0);
INSERT INTO `schedule_template` VALUES (125, 170, 4, 30, 20, 1, '2025-07-31 02:16:46', 0);
INSERT INTO `schedule_template` VALUES (126, 170, 2, 30, 20, 1, '2025-07-31 02:16:46', 0);
INSERT INTO `schedule_template` VALUES (127, 172, 5, 30, 20, 1, '2025-07-31 02:16:46', 0);
INSERT INTO `schedule_template` VALUES (128, 172, 4, 30, 20, 1, '2025-07-31 02:16:46', 0);
INSERT INTO `schedule_template` VALUES (129, 172, 2, 30, 20, 1, '2025-07-31 02:16:46', 0);
INSERT INTO `schedule_template` VALUES (130, 173, 5, 30, 20, 1, '2025-07-31 02:16:46', 0);
INSERT INTO `schedule_template` VALUES (131, 173, 4, 30, 20, 1, '2025-07-31 02:16:46', 0);
INSERT INTO `schedule_template` VALUES (132, 173, 2, 30, 20, 1, '2025-07-31 02:16:46', 0);
INSERT INTO `schedule_template` VALUES (133, 174, 5, 30, 20, 1, '2025-07-31 02:16:46', 0);
INSERT INTO `schedule_template` VALUES (134, 174, 4, 30, 20, 1, '2025-07-31 02:16:46', 0);
INSERT INTO `schedule_template` VALUES (135, 174, 2, 30, 20, 1, '2025-07-31 02:16:46', 0);
INSERT INTO `schedule_template` VALUES (136, 175, 5, 30, 20, 1, '2025-07-31 02:16:46', 0);
INSERT INTO `schedule_template` VALUES (137, 175, 4, 30, 20, 1, '2025-07-31 02:16:46', 0);
INSERT INTO `schedule_template` VALUES (138, 175, 2, 30, 20, 1, '2025-07-31 02:16:46', 0);
INSERT INTO `schedule_template` VALUES (139, 177, 5, 30, 20, 1, '2025-07-31 02:16:46', 0);
INSERT INTO `schedule_template` VALUES (140, 177, 4, 30, 20, 1, '2025-07-31 02:16:46', 0);
INSERT INTO `schedule_template` VALUES (141, 177, 2, 30, 20, 1, '2025-07-31 02:16:46', 0);
INSERT INTO `schedule_template` VALUES (142, 178, 5, 30, 20, 1, '2025-07-31 02:16:46', 0);
INSERT INTO `schedule_template` VALUES (143, 178, 4, 30, 20, 1, '2025-07-31 02:16:46', 0);
INSERT INTO `schedule_template` VALUES (144, 178, 2, 30, 20, 1, '2025-07-31 02:16:46', 0);
INSERT INTO `schedule_template` VALUES (145, 179, 5, 30, 20, 1, '2025-07-31 02:16:46', 0);
INSERT INTO `schedule_template` VALUES (146, 179, 4, 30, 20, 1, '2025-07-31 02:16:46', 0);
INSERT INTO `schedule_template` VALUES (147, 179, 2, 30, 20, 1, '2025-07-31 02:16:46', 0);
INSERT INTO `schedule_template` VALUES (148, 180, 5, 30, 20, 1, '2025-07-31 02:16:46', 0);
INSERT INTO `schedule_template` VALUES (149, 180, 4, 30, 20, 1, '2025-07-31 02:16:46', 0);
INSERT INTO `schedule_template` VALUES (150, 180, 2, 30, 20, 1, '2025-07-31 02:16:46', 0);
INSERT INTO `schedule_template` VALUES (151, 182, 5, 30, 20, 1, '2025-07-31 02:16:46', 0);
INSERT INTO `schedule_template` VALUES (152, 182, 4, 30, 20, 1, '2025-07-31 02:16:46', 0);
INSERT INTO `schedule_template` VALUES (153, 182, 2, 30, 20, 1, '2025-07-31 02:16:46', 0);
INSERT INTO `schedule_template` VALUES (154, 183, 5, 30, 20, 1, '2025-07-31 02:16:46', 0);
INSERT INTO `schedule_template` VALUES (155, 183, 4, 30, 20, 1, '2025-07-31 02:16:46', 0);
INSERT INTO `schedule_template` VALUES (156, 183, 2, 30, 20, 1, '2025-07-31 02:16:46', 0);
INSERT INTO `schedule_template` VALUES (157, 184, 5, 30, 20, 1, '2025-07-31 02:16:46', 0);
INSERT INTO `schedule_template` VALUES (158, 184, 4, 30, 20, 1, '2025-07-31 02:16:46', 0);
INSERT INTO `schedule_template` VALUES (159, 184, 2, 30, 20, 1, '2025-07-31 02:16:46', 0);
INSERT INTO `schedule_template` VALUES (160, 185, 5, 30, 20, 1, '2025-07-31 02:16:46', 0);
INSERT INTO `schedule_template` VALUES (161, 185, 4, 30, 20, 1, '2025-07-31 02:16:46', 0);
INSERT INTO `schedule_template` VALUES (162, 185, 2, 30, 20, 1, '2025-07-31 02:16:46', 0);
INSERT INTO `schedule_template` VALUES (163, 187, 5, 30, 20, 1, '2025-07-31 02:16:46', 0);
INSERT INTO `schedule_template` VALUES (164, 187, 4, 30, 20, 1, '2025-07-31 02:16:46', 0);
INSERT INTO `schedule_template` VALUES (165, 187, 2, 30, 20, 1, '2025-07-31 02:16:46', 0);
INSERT INTO `schedule_template` VALUES (166, 188, 5, 30, 20, 1, '2025-07-31 02:16:46', 0);
INSERT INTO `schedule_template` VALUES (167, 188, 4, 30, 20, 1, '2025-07-31 02:16:46', 0);
INSERT INTO `schedule_template` VALUES (168, 188, 2, 30, 20, 1, '2025-07-31 02:16:46', 0);
INSERT INTO `schedule_template` VALUES (169, 189, 5, 30, 20, 1, '2025-07-31 02:16:46', 0);
INSERT INTO `schedule_template` VALUES (170, 189, 4, 30, 20, 1, '2025-07-31 02:16:46', 0);
INSERT INTO `schedule_template` VALUES (171, 189, 2, 30, 20, 1, '2025-07-31 02:16:46', 0);
INSERT INTO `schedule_template` VALUES (172, 190, 5, 30, 20, 1, '2025-07-31 02:16:46', 0);
INSERT INTO `schedule_template` VALUES (173, 190, 4, 30, 20, 1, '2025-07-31 02:16:46', 0);
INSERT INTO `schedule_template` VALUES (174, 190, 2, 30, 20, 1, '2025-07-31 02:16:46', 0);
INSERT INTO `schedule_template` VALUES (175, 192, 5, 30, 20, 1, '2025-07-31 02:16:46', 0);
INSERT INTO `schedule_template` VALUES (176, 192, 4, 30, 20, 1, '2025-07-31 02:16:46', 0);
INSERT INTO `schedule_template` VALUES (177, 192, 2, 30, 20, 1, '2025-07-31 02:16:46', 0);
INSERT INTO `schedule_template` VALUES (178, 193, 5, 30, 20, 1, '2025-07-31 02:16:46', 0);
INSERT INTO `schedule_template` VALUES (179, 193, 4, 30, 20, 1, '2025-07-31 02:16:46', 0);
INSERT INTO `schedule_template` VALUES (180, 193, 2, 30, 20, 1, '2025-07-31 02:16:46', 0);
INSERT INTO `schedule_template` VALUES (181, 194, 5, 30, 20, 1, '2025-07-31 02:16:46', 0);
INSERT INTO `schedule_template` VALUES (182, 194, 4, 30, 20, 1, '2025-07-31 02:16:46', 0);
INSERT INTO `schedule_template` VALUES (183, 194, 2, 30, 20, 1, '2025-07-31 02:16:46', 0);
INSERT INTO `schedule_template` VALUES (184, 195, 5, 30, 20, 1, '2025-07-31 02:16:46', 0);
INSERT INTO `schedule_template` VALUES (185, 195, 4, 30, 20, 1, '2025-07-31 02:16:46', 0);
INSERT INTO `schedule_template` VALUES (186, 195, 2, 30, 0, 1, '2025-07-31 02:16:46', 0);

-- ----------------------------
-- Table structure for schedule
-- ----------------------------
DROP TABLE IF EXISTS `schedule`;
CREATE TABLE `schedule`  (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `template_id` bigint NULL DEFAULT NULL COMMENT '关联模板ID',
  `sub_department_id` bigint NOT NULL,
  `department_name` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL,
  `doctor_id` bigint NOT NULL,
  `doctor_name` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL,
  `schedule_date` date NOT NULL,
  `is_morning` tinyint(1) NOT NULL,
  `is_afternoon` tinyint(1) NOT NULL,
  `status` int NULL DEFAULT 0,
  `current_appointment_count` int NULL DEFAULT 0,
  `appointment_limit` int NOT NULL,
  `create_time` timestamp NULL DEFAULT CURRENT_TIMESTAMP,
  `is_deleted` int NULL DEFAULT 0,
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `department_id`(`sub_department_id` ASC) USING BTREE,
  INDEX `doctor_id`(`doctor_id` ASC) USING BTREE,
  INDEX `template_id`(`template_id` ASC) USING BTREE,
  CONSTRAINT `schedule_ibfk` FOREIGN KEY (`sub_department_id`) REFERENCES `sub_department` (`id`) ON DELETE RESTRICT ON UPDATE RESTRICT,
  CONSTRAINT `schedule_ibfk_2` FOREIGN KEY (`doctor_id`) REFERENCES `doctor_detail` (`id`) ON DELETE RESTRICT ON UPDATE RESTRICT,
  CONSTRAINT `schedule_ibfk_3` FOREIGN KEY (`template_id`) REFERENCES `schedule_template` (`id`) ON DELETE RESTRICT ON UPDATE RESTRICT
) ENGINE = InnoDB AUTO_INCREMENT = 633 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci ROW_FORMAT = DYNAMIC;

-- ----------------------------
-- Records of schedule
-- ----------------------------
INSERT INTO `schedule` VALUES (501, 31, 1, '心血管内科', 132, '李国华', '2025-08-08', 1, 0, 1, 0, 30, '2025-08-08 10:13:05', 1);
INSERT INTO `schedule` VALUES (502, 31, 1, '心血管内科', 132, '李国华', '2025-08-08', 0, 1, 1, 1, 20, '2025-08-08 10:13:06', 0);
INSERT INTO `schedule` VALUES (503, 34, 1, '心血管内科', 133, '王雪梅', '2025-08-08', 1, 0, 1, 0, 30, '2025-08-08 10:13:06', 1);
INSERT INTO `schedule` VALUES (504, 34, 1, '心血管内科', 133, '王雪梅', '2025-08-08', 0, 1, 1, 1, 20, '2025-08-08 10:13:06', 0);
INSERT INTO `schedule` VALUES (505, 37, 1, '心血管内科', 134, '赵立伟', '2025-08-08', 1, 0, 1, 0, 30, '2025-08-08 10:13:06', 1);
INSERT INTO `schedule` VALUES (506, 37, 1, '心血管内科', 134, '赵立伟', '2025-08-08', 0, 1, 1, 0, 20, '2025-08-08 10:13:06', 0);
INSERT INTO `schedule` VALUES (507, 40, 1, '心血管内科', 135, '陈晓芳', '2025-08-08', 1, 0, 1, 0, 30, '2025-08-08 10:13:06', 1);
INSERT INTO `schedule` VALUES (508, 40, 1, '心血管内科', 135, '陈晓芳', '2025-08-08', 0, 1, 1, 0, 20, '2025-08-08 10:13:07', 0);
INSERT INTO `schedule` VALUES (509, 43, 2, '呼吸内科', 137, '杨晓红', '2025-08-08', 1, 0, 1, 0, 30, '2025-08-08 10:13:07', 1);
INSERT INTO `schedule` VALUES (510, 43, 2, '呼吸内科', 137, '杨晓红', '2025-08-08', 0, 1, 1, 0, 20, '2025-08-08 10:13:07', 0);
INSERT INTO `schedule` VALUES (511, 46, 2, '呼吸内科', 138, '周建国', '2025-08-08', 1, 0, 1, 0, 30, '2025-08-08 10:13:07', 1);
INSERT INTO `schedule` VALUES (512, 46, 2, '呼吸内科', 138, '周建国', '2025-08-08', 0, 1, 1, 0, 20, '2025-08-08 10:13:07', 0);
INSERT INTO `schedule` VALUES (513, 49, 2, '呼吸内科', 139, '吴丽娜', '2025-08-08', 1, 0, 1, 0, 30, '2025-08-08 10:13:08', 1);
INSERT INTO `schedule` VALUES (514, 49, 2, '呼吸内科', 139, '吴丽娜', '2025-08-08', 0, 1, 1, 0, 20, '2025-08-08 10:13:07', 0);
INSERT INTO `schedule` VALUES (515, 52, 2, '呼吸内科', 140, '郑海洋', '2025-08-08', 1, 0, 1, 0, 30, '2025-08-08 10:13:08', 1);
INSERT INTO `schedule` VALUES (516, 52, 2, '呼吸内科', 140, '郑海洋', '2025-08-08', 0, 1, 1, 0, 20, '2025-08-08 10:13:08', 0);
INSERT INTO `schedule` VALUES (517, 55, 3, '消化内科', 142, '孙雅静', '2025-08-08', 1, 0, 1, 0, 30, '2025-08-08 10:13:08', 1);
INSERT INTO `schedule` VALUES (518, 55, 3, '消化内科', 142, '孙雅静', '2025-08-08', 0, 1, 1, 0, 20, '2025-08-08 10:13:08', 0);
INSERT INTO `schedule` VALUES (519, 58, 3, '消化内科', 143, '朱志强', '2025-08-08', 1, 0, 1, 0, 30, '2025-08-08 10:13:08', 1);
INSERT INTO `schedule` VALUES (520, 58, 3, '消化内科', 143, '朱志强', '2025-08-08', 0, 1, 1, 0, 20, '2025-08-08 10:13:08', 0);
INSERT INTO `schedule` VALUES (521, 61, 3, '消化内科', 144, '马玉华', '2025-08-08', 1, 0, 1, 0, 30, '2025-08-08 10:13:09', 1);
INSERT INTO `schedule` VALUES (522, 61, 3, '消化内科', 144, '马玉华', '2025-08-08', 0, 1, 1, 0, 20, '2025-08-08 10:13:09', 0);
INSERT INTO `schedule` VALUES (523, 64, 3, '消化内科', 145, '林国栋', '2025-08-08', 1, 0, 1, 0, 30, '2025-08-08 10:13:09', 1);
INSERT INTO `schedule` VALUES (524, 64, 3, '消化内科', 145, '林国栋', '2025-08-08', 0, 1, 1, 0, 20, '2025-08-08 10:13:09', 0);
INSERT INTO `schedule` VALUES (525, 67, 4, '骨科', 147, '徐丽娟', '2025-08-08', 1, 0, 1, 0, 30, '2025-08-08 10:13:09', 1);
INSERT INTO `schedule` VALUES (526, 67, 4, '骨科', 147, '徐丽娟', '2025-08-08', 0, 1, 1, 0, 20, '2025-08-08 10:13:09', 0);
INSERT INTO `schedule` VALUES (527, 70, 4, '骨科', 148, '高建国', '2025-08-08', 1, 0, 1, 0, 30, '2025-08-08 10:13:09', 1);
INSERT INTO `schedule` VALUES (528, 70, 4, '骨科', 148, '高建国', '2025-08-08', 0, 1, 1, 0, 20, '2025-08-08 10:13:10', 0);
INSERT INTO `schedule` VALUES (529, 73, 4, '骨科', 149, '胡晓燕', '2025-08-08', 1, 0, 1, 0, 30, '2025-08-08 10:13:10', 1);
INSERT INTO `schedule` VALUES (530, 73, 4, '骨科', 149, '胡晓燕', '2025-08-08', 0, 1, 1, 0, 20, '2025-08-08 10:13:10', 0);
INSERT INTO `schedule` VALUES (531, 76, 4, '骨科', 150, '欧阳峰', '2025-08-08', 1, 0, 1, 0, 30, '2025-08-08 10:13:10', 1);
INSERT INTO `schedule` VALUES (532, 76, 4, '骨科', 150, '欧阳峰', '2025-08-08', 0, 1, 1, 0, 20, '2025-08-08 10:13:10', 0);
INSERT INTO `schedule` VALUES (533, 79, 5, '神经外科', 152, '薛文静', '2025-08-08', 1, 0, 1, 0, 30, '2025-08-08 10:13:10', 1);
INSERT INTO `schedule` VALUES (534, 79, 5, '神经外科', 152, '薛文静', '2025-08-08', 0, 1, 1, 0, 20, '2025-08-08 10:13:10', 0);
INSERT INTO `schedule` VALUES (535, 82, 5, '神经外科', 153, '彭建国', '2025-08-08', 1, 0, 1, 0, 30, '2025-08-08 10:13:11', 1);
INSERT INTO `schedule` VALUES (536, 82, 5, '神经外科', 153, '彭建国', '2025-08-08', 0, 1, 1, 0, 20, '2025-08-08 10:13:11', 0);
INSERT INTO `schedule` VALUES (537, 85, 5, '神经外科', 154, '曾丽华', '2025-08-08', 1, 0, 1, 0, 30, '2025-08-08 10:13:11', 1);
INSERT INTO `schedule` VALUES (538, 85, 5, '神经外科', 154, '曾丽华', '2025-08-08', 0, 1, 1, 0, 20, '2025-08-08 10:13:11', 0);
INSERT INTO `schedule` VALUES (539, 88, 5, '神经外科', 155, '苏振华', '2025-08-08', 1, 0, 1, 0, 30, '2025-08-08 10:13:11', 1);
INSERT INTO `schedule` VALUES (540, 88, 5, '神经外科', 155, '苏振华', '2025-08-08', 0, 1, 1, 0, 20, '2025-08-08 10:13:11', 0);
INSERT INTO `schedule` VALUES (541, 91, 6, '普外科', 157, '梁玉梅', '2025-08-08', 1, 0, 1, 0, 30, '2025-08-08 10:13:12', 1);
INSERT INTO `schedule` VALUES (542, 91, 6, '普外科', 157, '梁玉梅', '2025-08-08', 0, 1, 1, 0, 20, '2025-08-08 10:13:12', 0);
INSERT INTO `schedule` VALUES (543, 94, 6, '普外科', 158, '谢国华', '2025-08-08', 1, 0, 1, 0, 30, '2025-08-08 10:13:12', 1);
INSERT INTO `schedule` VALUES (544, 94, 6, '普外科', 158, '谢国华', '2025-08-08', 0, 1, 1, 0, 20, '2025-08-08 10:13:12', 0);
INSERT INTO `schedule` VALUES (545, 97, 6, '普外科', 159, '宋丽萍', '2025-08-08', 1, 0, 1, 0, 30, '2025-08-08 10:13:12', 1);
INSERT INTO `schedule` VALUES (546, 97, 6, '普外科', 159, '宋丽萍', '2025-08-08', 0, 1, 1, 0, 20, '2025-08-08 10:13:12', 0);
INSERT INTO `schedule` VALUES (547, 100, 6, '普外科', 160, '唐志强', '2025-08-08', 1, 0, 1, 0, 30, '2025-08-08 10:13:12', 1);
INSERT INTO `schedule` VALUES (548, 100, 6, '普外科', 160, '唐志强', '2025-08-08', 0, 1, 1, 0, 20, '2025-08-08 10:13:13', 0);
INSERT INTO `schedule` VALUES (549, 103, 7, '妇科', 162, '于丽华', '2025-08-08', 1, 0, 1, 0, 30, '2025-08-08 10:13:13', 1);
INSERT INTO `schedule` VALUES (550, 103, 7, '妇科', 162, '于丽华', '2025-08-08', 0, 1, 1, 0, 20, '2025-08-08 10:13:13', 0);
INSERT INTO `schedule` VALUES (551, 106, 7, '妇科', 163, '傅晓燕', '2025-08-08', 1, 0, 1, 0, 30, '2025-08-08 10:13:13', 1);
INSERT INTO `schedule` VALUES (552, 106, 7, '妇科', 163, '傅晓燕', '2025-08-08', 0, 1, 1, 0, 20, '2025-08-08 10:13:13', 0);
INSERT INTO `schedule` VALUES (553, 109, 7, '妇科', 164, '崔玉珍', '2025-08-08', 1, 0, 1, 0, 30, '2025-08-08 10:13:13', 1);
INSERT INTO `schedule` VALUES (554, 109, 7, '妇科', 164, '崔玉珍', '2025-08-08', 0, 1, 1, 0, 20, '2025-08-08 10:13:14', 0);
INSERT INTO `schedule` VALUES (555, 112, 7, '妇科', 165, '姜丽娜', '2025-08-08', 1, 0, 1, 0, 30, '2025-08-08 10:13:14', 1);
INSERT INTO `schedule` VALUES (556, 112, 7, '妇科', 165, '姜丽娜', '2025-08-08', 0, 1, 1, 0, 20, '2025-08-08 10:13:14', 0);
INSERT INTO `schedule` VALUES (557, 115, 8, '产科', 167, '潘晓红', '2025-08-08', 1, 0, 1, 0, 30, '2025-08-08 10:13:14', 1);
INSERT INTO `schedule` VALUES (558, 115, 8, '产科', 167, '潘晓红', '2025-08-08', 0, 1, 1, 0, 20, '2025-08-08 10:13:14', 0);
INSERT INTO `schedule` VALUES (559, 118, 8, '产科', 168, '钟玉华', '2025-08-08', 1, 0, 1, 0, 30, '2025-08-08 10:13:14', 1);
INSERT INTO `schedule` VALUES (560, 118, 8, '产科', 168, '钟玉华', '2025-08-08', 0, 1, 1, 0, 20, '2025-08-08 10:13:14', 0);
INSERT INTO `schedule` VALUES (561, 121, 8, '产科', 169, '田丽娟', '2025-08-08', 1, 0, 1, 0, 30, '2025-08-08 10:13:15', 1);
INSERT INTO `schedule` VALUES (562, 121, 8, '产科', 169, '田丽娟', '2025-08-08', 0, 1, 1, 0, 20, '2025-08-08 10:13:15', 0);
INSERT INTO `schedule` VALUES (563, 124, 8, '产科', 170, '杜小燕', '2025-08-08', 1, 0, 1, 0, 30, '2025-08-08 10:13:15', 1);
INSERT INTO `schedule` VALUES (564, 124, 8, '产科', 170, '杜小燕', '2025-08-08', 0, 1, 1, 0, 20, '2025-08-08 10:13:15', 0);
INSERT INTO `schedule` VALUES (565, 127, 9, '新生儿科', 172, '蒋丽萍', '2025-08-08', 1, 0, 1, 0, 30, '2025-08-08 10:13:15', 1);
INSERT INTO `schedule` VALUES (566, 127, 9, '新生儿科', 172, '蒋丽萍', '2025-08-08', 0, 1, 1, 0, 20, '2025-08-08 10:13:15', 0);
INSERT INTO `schedule` VALUES (567, 130, 9, '新生儿科', 173, '沈志强', '2025-08-08', 1, 0, 1, 0, 30, '2025-08-08 10:13:16', 1);
INSERT INTO `schedule` VALUES (568, 130, 9, '新生儿科', 173, '沈志强', '2025-08-08', 0, 1, 1, 0, 20, '2025-08-08 10:13:16', 0);
INSERT INTO `schedule` VALUES (569, 133, 9, '新生儿科', 174, '范晓华', '2025-08-08', 1, 0, 1, 0, 30, '2025-08-08 10:13:16', 1);
INSERT INTO `schedule` VALUES (570, 133, 9, '新生儿科', 174, '范晓华', '2025-08-08', 0, 1, 1, 0, 20, '2025-08-08 10:13:16', 0);
INSERT INTO `schedule` VALUES (571, 136, 9, '新生儿科', 175, '龙玉梅', '2025-08-08', 1, 0, 1, 0, 30, '2025-08-08 10:13:16', 1);
INSERT INTO `schedule` VALUES (572, 136, 9, '新生儿科', 175, '龙玉梅', '2025-08-08', 0, 1, 1, 0, 20, '2025-08-08 10:13:16', 0);
INSERT INTO `schedule` VALUES (573, 139, 10, '小儿内科', 177, '方丽华', '2025-08-08', 1, 0, 1, 0, 30, '2025-08-08 10:13:16', 1);
INSERT INTO `schedule` VALUES (574, 139, 10, '小儿内科', 177, '方丽华', '2025-08-08', 0, 1, 1, 0, 20, '2025-08-08 10:13:17', 0);
INSERT INTO `schedule` VALUES (575, 142, 10, '小儿内科', 178, '石志伟', '2025-08-08', 1, 0, 1, 0, 30, '2025-08-08 10:13:17', 1);
INSERT INTO `schedule` VALUES (576, 142, 10, '小儿内科', 178, '石志伟', '2025-08-08', 0, 1, 1, 0, 20, '2025-08-08 10:13:17', 0);
INSERT INTO `schedule` VALUES (577, 145, 10, '小儿内科', 179, '姚晓燕', '2025-08-08', 1, 0, 1, 0, 30, '2025-08-08 10:13:17', 1);
INSERT INTO `schedule` VALUES (578, 145, 10, '小儿内科', 179, '姚晓燕', '2025-08-08', 0, 1, 1, 0, 20, '2025-08-08 10:13:17', 0);
INSERT INTO `schedule` VALUES (579, 148, 10, '小儿内科', 180, '毛玉珍', '2025-08-08', 1, 0, 1, 0, 30, '2025-08-08 10:13:17', 1);
INSERT INTO `schedule` VALUES (580, 148, 10, '小儿内科', 180, '毛玉珍', '2025-08-08', 0, 1, 1, 0, 20, '2025-08-08 10:13:18', 0);
INSERT INTO `schedule` VALUES (581, 151, 11, '小儿外科', 182, '卢小娟', '2025-08-08', 1, 0, 1, 0, 30, '2025-08-08 10:13:18', 1);
INSERT INTO `schedule` VALUES (582, 151, 11, '小儿外科', 182, '卢小娟', '2025-08-08', 0, 1, 1, 0, 20, '2025-08-08 10:13:18', 0);
INSERT INTO `schedule` VALUES (583, 154, 11, '小儿外科', 183, '丁国华', '2025-08-08', 1, 0, 1, 0, 30, '2025-08-08 10:13:18', 1);
INSERT INTO `schedule` VALUES (584, 154, 11, '小儿外科', 183, '丁国华', '2025-08-08', 0, 1, 1, 0, 20, '2025-08-08 10:13:18', 0);
INSERT INTO `schedule` VALUES (585, 157, 11, '小儿外科', 184, '文丽娜', '2025-08-08', 1, 0, 1, 0, 30, '2025-08-08 10:13:18', 1);
INSERT INTO `schedule` VALUES (586, 157, 11, '小儿外科', 184, '文丽娜', '2025-08-08', 0, 1, 1, 0, 20, '2025-08-08 10:13:19', 0);
INSERT INTO `schedule` VALUES (587, 160, 11, '小儿外科', 185, '龚志强', '2025-08-08', 1, 0, 1, 0, 30, '2025-08-08 10:13:19', 1);
INSERT INTO `schedule` VALUES (588, 160, 11, '小儿外科', 185, '龚志强', '2025-08-08', 0, 1, 1, 0, 20, '2025-08-08 10:13:19', 0);
INSERT INTO `schedule` VALUES (589, 163, 12, '屈光矫正科', 187, '白丽华', '2025-08-08', 1, 0, 1, 0, 30, '2025-08-08 10:13:19', 1);
INSERT INTO `schedule` VALUES (590, 163, 12, '屈光矫正科', 187, '白丽华', '2025-08-08', 0, 1, 1, 0, 20, '2025-08-08 10:13:19', 0);
INSERT INTO `schedule` VALUES (591, 166, 12, '屈光矫正科', 188, '陆志伟', '2025-08-08', 1, 0, 1, 0, 30, '2025-08-08 10:13:19', 1);
INSERT INTO `schedule` VALUES (592, 166, 12, '屈光矫正科', 188, '陆志伟', '2025-08-08', 0, 1, 1, 0, 20, '2025-08-08 10:13:20', 0);
INSERT INTO `schedule` VALUES (593, 169, 12, '屈光矫正科', 189, '肖玉珍', '2025-08-08', 1, 0, 1, 0, 30, '2025-08-08 10:13:20', 1);
INSERT INTO `schedule` VALUES (594, 169, 12, '屈光矫正科', 189, '肖玉珍', '2025-08-08', 0, 1, 1, 0, 20, '2025-08-08 10:13:20', 0);
INSERT INTO `schedule` VALUES (595, 172, 12, '屈光矫正科', 190, '侯小燕', '2025-08-08', 1, 0, 1, 0, 30, '2025-08-08 10:13:20', 1);
INSERT INTO `schedule` VALUES (596, 172, 12, '屈光矫正科', 190, '侯小燕', '2025-08-08', 0, 1, 1, 0, 20, '2025-08-08 10:13:20', 0);
INSERT INTO `schedule` VALUES (597, 175, 13, '眼底病科', 192, '黎丽萍', '2025-08-08', 1, 0, 1, 0, 30, '2025-08-08 10:13:20', 1);
INSERT INTO `schedule` VALUES (598, 175, 13, '眼底病科', 192, '黎丽萍', '2025-08-08', 0, 1, 1, 0, 20, '2025-08-08 10:13:20', 0);
INSERT INTO `schedule` VALUES (599, 178, 13, '眼底病科', 193, '秦建国', '2025-08-08', 1, 0, 1, 0, 30, '2025-08-08 10:13:21', 1);
INSERT INTO `schedule` VALUES (600, 178, 13, '眼底病科', 193, '秦建国', '2025-08-08', 0, 1, 1, 0, 20, '2025-08-08 10:13:21', 0);
INSERT INTO `schedule` VALUES (601, 181, 13, '眼底病科', 194, '史晓红', '2025-08-08', 1, 0, 1, 0, 30, '2025-08-08 10:13:21', 1);
INSERT INTO `schedule` VALUES (602, 181, 13, '眼底病科', 194, '史晓红', '2025-08-08', 0, 1, 1, 0, 20, '2025-08-08 10:13:21', 0);
INSERT INTO `schedule` VALUES (603, 184, 13, '眼底病科', 195, '廖玉华', '2025-08-08', 1, 0, 1, 0, 30, '2025-08-08 10:13:21', 1);
INSERT INTO `schedule` VALUES (604, 184, 13, '眼底病科', 195, '廖玉华', '2025-08-08', 0, 1, 1, 0, 20, '2025-08-08 10:13:21', 0);
INSERT INTO `schedule` VALUES (605, 2, 1, '心血管内科', 131, '张心明', '2025-08-11', 1, 0, 1, 0, 15, '2025-08-08 10:13:22', 0);
INSERT INTO `schedule` VALUES (606, 2, 1, '心血管内科', 131, '张心明', '2025-08-11', 0, 1, 1, 0, 10, '2025-08-08 10:13:22', 0);
INSERT INTO `schedule` VALUES (607, 4, 2, '呼吸内科', 136, '刘振华', '2025-08-11', 1, 0, 1, 0, 15, '2025-08-08 10:13:22', 0);
INSERT INTO `schedule` VALUES (608, 4, 2, '呼吸内科', 136, '刘振华', '2025-08-11', 0, 1, 1, 0, 10, '2025-08-08 10:13:22', 0);
INSERT INTO `schedule` VALUES (609, 6, 3, '消化内科', 141, '钱卫东', '2025-08-11', 1, 0, 1, 0, 15, '2025-08-08 10:13:22', 0);
INSERT INTO `schedule` VALUES (610, 6, 3, '消化内科', 141, '钱卫东', '2025-08-11', 0, 1, 1, 0, 10, '2025-08-08 10:13:22', 0);
INSERT INTO `schedule` VALUES (611, 8, 4, '骨科', 146, '黄伟明', '2025-08-11', 1, 0, 1, 0, 15, '2025-08-08 10:13:22', 0);
INSERT INTO `schedule` VALUES (612, 8, 4, '骨科', 146, '黄伟明', '2025-08-11', 0, 1, 1, 0, 10, '2025-08-08 10:13:23', 0);
INSERT INTO `schedule` VALUES (613, 10, 5, '神经外科', 151, '曹志强', '2025-08-11', 1, 0, 1, 0, 15, '2025-08-08 10:13:23', 0);
INSERT INTO `schedule` VALUES (614, 10, 5, '神经外科', 151, '曹志强', '2025-08-11', 0, 1, 1, 0, 10, '2025-08-08 10:13:23', 0);
INSERT INTO `schedule` VALUES (615, 12, 6, '普外科', 156, '董卫东', '2025-08-11', 1, 0, 1, 0, 15, '2025-08-08 10:13:23', 0);
INSERT INTO `schedule` VALUES (616, 12, 6, '普外科', 156, '董卫东', '2025-08-11', 0, 1, 1, 0, 10, '2025-08-08 10:13:23', 0);
INSERT INTO `schedule` VALUES (617, 14, 7, '妇科', 161, '韩雪梅', '2025-08-11', 1, 0, 1, 0, 15, '2025-08-08 10:13:23', 0);
INSERT INTO `schedule` VALUES (618, 14, 7, '妇科', 161, '韩雪梅', '2025-08-11', 0, 1, 1, 0, 10, '2025-08-08 10:13:23', 0);
INSERT INTO `schedule` VALUES (619, 16, 8, '产科', 166, '蔡文静', '2025-08-11', 1, 0, 1, 0, 15, '2025-08-08 10:13:24', 0);
INSERT INTO `schedule` VALUES (620, 16, 8, '产科', 166, '蔡文静', '2025-08-11', 0, 1, 1, 0, 10, '2025-08-08 10:13:24', 0);
INSERT INTO `schedule` VALUES (621, 18, 9, '新生儿科', 171, '魏国栋', '2025-08-11', 1, 0, 1, 0, 15, '2025-08-08 10:13:24', 0);
INSERT INTO `schedule` VALUES (622, 18, 9, '新生儿科', 171, '魏国栋', '2025-08-11', 0, 1, 1, 0, 10, '2025-08-08 10:13:24', 0);
INSERT INTO `schedule` VALUES (623, 20, 10, '小儿内科', 176, '孟建国', '2025-08-11', 1, 0, 1, 0, 15, '2025-08-08 10:13:24', 0);
INSERT INTO `schedule` VALUES (624, 20, 10, '小儿内科', 176, '孟建国', '2025-08-11', 0, 1, 1, 0, 10, '2025-08-08 10:13:24', 0);
INSERT INTO `schedule` VALUES (625, 22, 11, '小儿外科', 181, '江伟明', '2025-08-11', 1, 0, 1, 0, 15, '2025-08-08 10:13:24', 0);
INSERT INTO `schedule` VALUES (626, 22, 11, '小儿外科', 181, '江伟明', '2025-08-11', 0, 1, 1, 0, 10, '2025-08-08 10:13:25', 0);
INSERT INTO `schedule` VALUES (627, 24, 12, '屈光矫正科', 186, '雷晓明', '2025-08-11', 1, 0, 1, 0, 15, '2025-08-08 10:13:25', 0);
INSERT INTO `schedule` VALUES (628, 24, 12, '屈光矫正科', 186, '雷晓明', '2025-08-11', 0, 1, 1, 0, 10, '2025-08-08 10:13:25', 0);
INSERT INTO `schedule` VALUES (629, 26, 13, '眼底病科', 191, '谭卫东', '2025-08-11', 1, 0, 1, 0, 15, '2025-08-08 10:13:25', 0);
INSERT INTO `schedule` VALUES (630, 26, 13, '眼底病科', 191, '谭卫东', '2025-08-11', 0, 1, 1, 0, 10, '2025-08-08 10:13:25', 0);
INSERT INTO `schedule` VALUES (631, 28, 6, '普外科', 197, '张杨明', '2025-08-11', 1, 0, 1, 0, 15, '2025-08-08 10:13:25', 0);
INSERT INTO `schedule` VALUES (632, 28, 6, '普外科', 197, '张杨明', '2025-08-11', 0, 1, 1, 0, 10, '2025-08-08 10:13:25', 0);

-- ----------------------------
-- Table structure for drug
-- ----------------------------
DROP TABLE IF EXISTS `drug`;
CREATE TABLE `drug`  (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `generic_name` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL,
  `specification` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL,
  `minimum_sales_unit` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL,
  `drug_price` decimal(10, 2) NULL DEFAULT NULL,
  `quantity` int NULL DEFAULT NULL,
  `is_prescription` tinyint(1) NULL DEFAULT NULL,
  `create_time` timestamp NULL DEFAULT CURRENT_TIMESTAMP,
  `is_deleted` int NULL DEFAULT 0,
  PRIMARY KEY (`id`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 31 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci ROW_FORMAT = DYNAMIC;

-- ----------------------------
-- Records of drug
-- ----------------------------
INSERT INTO `drug` VALUES (1, '阿莫西林胶囊', '0.25g/粒', '盒', 28.50, 497, 1, '2025-08-02 08:43:47', 0);
INSERT INTO `drug` VALUES (2, '头孢克肟分散片', '0.1g/片', '盒', 35.80, 300, 1, '2025-08-02 08:43:47', 0);
INSERT INTO `drug` VALUES (3, '阿奇霉素干混悬剂', '0.1g/袋', '盒', 42.30, 178, 1, '2025-08-02 08:43:47', 0);
INSERT INTO `drug` VALUES (4, '左氧氟沙星片', '0.5g/片', '盒', 58.00, 150, 1, '2025-08-02 08:43:47', 0);
INSERT INTO `drug` VALUES (5, '克林霉素凝胶', '20g/支', '支', 18.90, 250, 1, '2025-08-02 08:43:47', 0);
INSERT INTO `drug` VALUES (6, '复方氨酚烷胺片', '12片/板', '盒', 15.60, 798, 0, '2025-08-02 08:43:47', 0);
INSERT INTO `drug` VALUES (7, '感冒灵颗粒', '10g/袋', '盒', 22.50, 592, 0, '2025-08-02 08:43:47', 0);
INSERT INTO `drug` VALUES (8, '布洛芬缓释胶囊', '0.3g/粒', '盒', 28.80, 693, 0, '2025-08-02 08:43:47', 0);
INSERT INTO `drug` VALUES (9, '对乙酰氨基酚片', '0.5g/片', '瓶', 9.90, 1000, 0, '2025-08-02 08:43:47', 0);
INSERT INTO `drug` VALUES (10, '盐酸伪麻黄碱滴鼻液', '10ml/支', '支', 12.30, 400, 0, '2025-08-02 08:43:47', 0);
INSERT INTO `drug` VALUES (11, '奥美拉唑肠溶胶囊', '20mg/粒', '盒', 45.00, 300, 1, '2025-08-02 08:43:47', 0);
INSERT INTO `drug` VALUES (12, '多潘立酮片', '10mg/片', '盒', 32.80, 400, 1, '2025-08-02 08:43:47', 0);
INSERT INTO `drug` VALUES (13, '铝碳酸镁咀嚼片', '0.5g/片', '盒', 26.50, 500, 0, '2025-08-02 08:43:47', 0);
INSERT INTO `drug` VALUES (14, '蒙脱石散', '3g/袋', '盒', 19.80, 600, 0, '2025-08-02 08:43:47', 0);
INSERT INTO `drug` VALUES (15, '复方消化酶胶囊', '20粒/盒', '盒', 58.60, 200, 1, '2025-08-02 08:43:47', 0);
INSERT INTO `drug` VALUES (16, '硝苯地平缓释片', '20mg/片', '盒', 38.50, 250, 1, '2025-08-02 08:43:47', 0);
INSERT INTO `drug` VALUES (17, '阿托伐他汀钙片', '20mg/片', '盒', 78.00, 180, 1, '2025-08-02 08:43:47', 0);
INSERT INTO `drug` VALUES (18, '阿司匹林肠溶片', '100mg/片', '盒', 15.20, 900, 1, '2025-08-02 08:43:47', 0);
INSERT INTO `drug` VALUES (19, '美托洛尔缓释片', '47.5mg/片', '盒', 65.80, 200, 1, '2025-08-02 08:43:47', 0);
INSERT INTO `drug` VALUES (20, '缬沙坦胶囊', '80mg/粒', '盒', 52.30, 220, 1, '2025-08-02 08:43:47', 0);
INSERT INTO `drug` VALUES (21, '沙丁胺醇气雾剂', '100μg/揿', '支', 48.00, 150, 1, '2025-08-02 08:43:47', 0);
INSERT INTO `drug` VALUES (22, '布地奈德福莫特罗粉吸入剂', '160μg/4.5μg/吸', '盒', 218.00, 100, 1, '2025-08-02 08:43:47', 0);
INSERT INTO `drug` VALUES (23, '盐酸氨溴索口服溶液', '100ml:300mg', '瓶', 35.60, 300, 0, '2025-08-02 08:43:47', 0);
INSERT INTO `drug` VALUES (24, '孟鲁司特钠咀嚼片', '5mg/片', '盒', 88.50, 180, 1, '2025-08-02 08:43:47', 0);
INSERT INTO `drug` VALUES (25, '维生素C片', '0.1g/片', '瓶', 12.80, 1000, 0, '2025-08-02 08:43:47', 0);
INSERT INTO `drug` VALUES (26, '复合维生素B片', '100片/瓶', '瓶', 18.50, 800, 0, '2025-08-02 08:43:47', 0);
INSERT INTO `drug` VALUES (27, '葡萄糖酸钙片', '0.5g/片', '盒', 25.60, 600, 0, '2025-08-02 08:43:47', 0);
INSERT INTO `drug` VALUES (28, '维生素D滴剂', '400IU/粒', '盒', 58.00, 400, 0, '2025-08-02 08:43:47', 0);
INSERT INTO `drug` VALUES (29, '碘伏消毒液', '100ml/瓶', '瓶', 8.90, 1200, 0, '2025-08-02 08:43:47', 0);
INSERT INTO `drug` VALUES (30, '红霉素软膏', '10g/支', '支', 6.50, 1500, 0, '2025-08-02 08:43:47', 0);

-- ----------------------------
-- Table structure for patient_attendant
-- ----------------------------
DROP TABLE IF EXISTS `patient_attendant`;
CREATE TABLE `patient_attendant`  (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `system_user_id` bigint NOT NULL,
  `real_name` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL,
  `id_card` varchar(25) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL,
  `gender` int NULL DEFAULT NULL,
  `home_address` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL,
  `create_time` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `update_time` timestamp NULL DEFAULT NULL ON UPDATE CURRENT_TIMESTAMP,
  `is_deleted` int NOT NULL DEFAULT 0,
  `phone` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL,
  `nickname` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL,
  `is_master` int NOT NULL,
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `system_user_id`(`system_user_id` ASC) USING BTREE,
  CONSTRAINT `patient_attendant_ibfk_1` FOREIGN KEY (`system_user_id`) REFERENCES `system_user` (`id`) ON DELETE RESTRICT ON UPDATE RESTRICT
) ENGINE = InnoDB AUTO_INCREMENT = 9 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci ROW_FORMAT = DYNAMIC;

-- ----------------------------
-- Records of patient_attendant
-- ----------------------------
INSERT INTO `patient_attendant` VALUES (1, 3, '任焱辉', '410402200408275575', 2, 'beijing', '2025-07-29 00:49:34', '2025-08-04 20:35:20', 0, '15036857609', 'nickname02', 1);
INSERT INTO `patient_attendant` VALUES (2, 3, '高梦婷', '513126199903022220', NULL, NULL, '2025-07-29 01:24:45', NULL, 0, NULL, NULL, 0);
INSERT INTO `patient_attendant` VALUES (3, 3, '刘彬', '513126198803120435', NULL, NULL, '2025-07-29 01:25:01', NULL, 0, NULL, NULL, 0);
INSERT INTO `patient_attendant` VALUES (4, 3, '罗开荣', '513126197310101311', NULL, NULL, '2025-07-29 01:25:40', '2025-08-04 19:53:07', 1, NULL, NULL, 0);
INSERT INTO `patient_attendant` VALUES (5, 69, '廖昌文', '511129198901014211', NULL, NULL, '2025-07-29 08:52:39', '2025-07-30 20:36:19', 0, NULL, NULL, 0);
INSERT INTO `patient_attendant` VALUES (6, 80, '叶宏林', '532524199603210010', 1, 'aaa', '2025-08-04 18:01:21', '2025-08-07 17:00:25', 0, '15044445533', 'user2', 1);
INSERT INTO `patient_attendant` VALUES (7, 80, '万玲宏', '532524197810270041', NULL, NULL, '2025-08-04 21:30:23', NULL, 0, NULL, NULL, 0);
INSERT INTO `patient_attendant` VALUES (8, 81, NULL, NULL, NULL, NULL, '2025-08-08 14:08:17', NULL, 0, NULL, 'test01', 1);

-- ----------------------------
-- Table structure for registration
-- ----------------------------
DROP TABLE IF EXISTS `registration`;
CREATE TABLE `registration`  (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `doctor_id` bigint NOT NULL,
  `patient_id` bigint NOT NULL,
  `schedule_id` int NULL DEFAULT NULL,
  `create_time` timestamp NULL DEFAULT CURRENT_TIMESTAMP,
  `registration_status` int NULL DEFAULT NULL,
  `person_key` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL,
  `request_token` varchar(80) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL,
  `update_time` timestamp NULL DEFAULT NULL ON UPDATE CURRENT_TIMESTAMP,
  `is_deleted` int NULL DEFAULT 0,
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `doctor_id`(`doctor_id` ASC) USING BTREE,
  INDEX `patient_id`(`patient_id` ASC) USING BTREE,
  INDEX `idx_registration_schedule_person`(`schedule_id` ASC, `person_key` ASC) USING BTREE,
  UNIQUE INDEX `uk_registration_request_token`(`request_token` ASC) USING BTREE,
  CONSTRAINT `registration_ibfk_1` FOREIGN KEY (`doctor_id`) REFERENCES `doctor_detail` (`id`) ON DELETE RESTRICT ON UPDATE RESTRICT,
  CONSTRAINT `registration_ibfk_2` FOREIGN KEY (`patient_id`) REFERENCES `patient_attendant` (`id`) ON DELETE RESTRICT ON UPDATE RESTRICT
) ENGINE = InnoDB AUTO_INCREMENT = 96 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci ROW_FORMAT = DYNAMIC;

-- ----------------------------
-- Records of registration
-- ----------------------------
INSERT INTO `registration` (`id`, `doctor_id`, `patient_id`, `schedule_id`, `create_time`, `registration_status`, `person_key`, `request_token`, `update_time`, `is_deleted`) VALUES (1, 191, 6, 582, '2025-08-08 09:01:09', 2, NULL, NULL, '2025-08-08 05:03:56', 0);
INSERT INTO `registration` (`id`, `doctor_id`, `patient_id`, `schedule_id`, `create_time`, `registration_status`, `person_key`, `request_token`, `update_time`, `is_deleted`) VALUES (2, 152, 3, 600, '2025-08-08 19:33:18', 6, NULL, NULL, '2025-08-08 11:46:05', 0);
INSERT INTO `registration` (`id`, `doctor_id`, `patient_id`, `schedule_id`, `create_time`, `registration_status`, `person_key`, `request_token`, `update_time`, `is_deleted`) VALUES (3, 161, 5, 567, '2025-08-08 09:51:16', 2, NULL, NULL, '2025-08-08 04:09:37', 0);
INSERT INTO `registration` (`id`, `doctor_id`, `patient_id`, `schedule_id`, `create_time`, `registration_status`, `person_key`, `request_token`, `update_time`, `is_deleted`) VALUES (4, 189, 7, 583, '2025-08-08 11:18:35', 7, NULL, NULL, '2025-08-08 01:09:01', 0);
INSERT INTO `registration` (`id`, `doctor_id`, `patient_id`, `schedule_id`, `create_time`, `registration_status`, `person_key`, `request_token`, `update_time`, `is_deleted`) VALUES (5, 188, 3, 553, '2025-08-08 17:48:45', 6, NULL, NULL, '2025-08-08 20:59:10', 0);
INSERT INTO `registration` (`id`, `doctor_id`, `patient_id`, `schedule_id`, `create_time`, `registration_status`, `person_key`, `request_token`, `update_time`, `is_deleted`) VALUES (6, 143, 3, 591, '2025-08-08 01:00:40', 3, NULL, NULL, '2025-08-08 00:33:15', 0);
INSERT INTO `registration` (`id`, `doctor_id`, `patient_id`, `schedule_id`, `create_time`, `registration_status`, `person_key`, `request_token`, `update_time`, `is_deleted`) VALUES (7, 182, 4, 583, '2025-08-08 14:51:01', 5, NULL, NULL, '2025-08-08 21:04:12', 0);
INSERT INTO `registration` (`id`, `doctor_id`, `patient_id`, `schedule_id`, `create_time`, `registration_status`, `person_key`, `request_token`, `update_time`, `is_deleted`) VALUES (8, 162, 1, 562, '2025-08-08 01:23:37', 2, NULL, NULL, '2025-08-08 18:36:01', 0);
INSERT INTO `registration` (`id`, `doctor_id`, `patient_id`, `schedule_id`, `create_time`, `registration_status`, `person_key`, `request_token`, `update_time`, `is_deleted`) VALUES (9, 168, 5, 582, '2025-08-08 07:31:56', 2, NULL, NULL, '2025-08-08 05:30:22', 0);
INSERT INTO `registration` (`id`, `doctor_id`, `patient_id`, `schedule_id`, `create_time`, `registration_status`, `person_key`, `request_token`, `update_time`, `is_deleted`) VALUES (10, 183, 6, 574, '2025-08-08 02:55:41', 4, NULL, NULL, '2025-08-08 18:55:19', 0);
INSERT INTO `registration` (`id`, `doctor_id`, `patient_id`, `schedule_id`, `create_time`, `registration_status`, `person_key`, `request_token`, `update_time`, `is_deleted`) VALUES (90, 132, 1, 502, '2025-08-08 10:23:53', 3, NULL, NULL, '2025-08-09 18:17:19', 0);
INSERT INTO `registration` (`id`, `doctor_id`, `patient_id`, `schedule_id`, `create_time`, `registration_status`, `person_key`, `request_token`, `update_time`, `is_deleted`) VALUES (91, 132, 2, 502, '2025-08-08 10:33:51', 0, NULL, NULL, '2025-08-08 10:34:23', 1);
INSERT INTO `registration` (`id`, `doctor_id`, `patient_id`, `schedule_id`, `create_time`, `registration_status`, `person_key`, `request_token`, `update_time`, `is_deleted`) VALUES (92, 132, 2, 502, '2025-08-08 10:34:58', 7, NULL, NULL, '2025-08-08 10:36:36', 1);
INSERT INTO `registration` (`id`, `doctor_id`, `patient_id`, `schedule_id`, `create_time`, `registration_status`, `person_key`, `request_token`, `update_time`, `is_deleted`) VALUES (93, 133, 3, 504, '2025-08-08 10:37:30', 2, NULL, NULL, '2025-08-08 10:41:16', 0);
INSERT INTO `registration` (`id`, `doctor_id`, `patient_id`, `schedule_id`, `create_time`, `registration_status`, `person_key`, `request_token`, `update_time`, `is_deleted`) VALUES (94, 131, 2, 605, '2025-08-08 14:13:22', 7, NULL, NULL, '2025-08-08 14:14:40', 1);
INSERT INTO `registration` (`id`, `doctor_id`, `patient_id`, `schedule_id`, `create_time`, `registration_status`, `person_key`, `request_token`, `update_time`, `is_deleted`) VALUES (95, 146, 1, 611, '2025-08-09 18:14:16', 7, NULL, NULL, '2025-08-09 18:14:33', 1);

UPDATE `registration` r
JOIN `patient_attendant` p ON r.`patient_id` = p.`id`
SET r.`person_key` = CASE
    WHEN p.`id_card` IS NULL OR TRIM(p.`id_card`) = '' THEN NULL
    ELSE SHA2(UPPER(TRIM(p.`id_card`)), 256)
END
WHERE r.`person_key` IS NULL;

-- ----------------------------
-- Table structure for registration_person_lock
-- ----------------------------
DROP TABLE IF EXISTS `registration_person_lock`;
CREATE TABLE `registration_person_lock`  (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `schedule_id` bigint NOT NULL,
  `person_key` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL,
  `request_token` varchar(80) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL,
  `registration_id` bigint NULL DEFAULT NULL,
  `create_time` timestamp NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `uk_registration_person_lock`(`schedule_id` ASC, `person_key` ASC) USING BTREE,
  UNIQUE INDEX `uk_registration_person_request`(`request_token` ASC) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci ROW_FORMAT = DYNAMIC;

-- ----------------------------
-- Table structure for room
-- ----------------------------
DROP TABLE IF EXISTS `room`;
CREATE TABLE `room`  (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `registration_id` bigint NOT NULL,
  `doctor_id` bigint NOT NULL,
  `patient_id` bigint NOT NULL,
  `patient_name` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL,
  `room_status` int NULL DEFAULT 1,
  `create_time` datetime NULL DEFAULT CURRENT_TIMESTAMP,
  `update_time` datetime NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `is_deleted` tinyint NULL DEFAULT 0,
  PRIMARY KEY (`id`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 177 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of room
-- ----------------------------
INSERT INTO `room` VALUES (175, 90, 5, 3, '任焱辉', 5, '2025-08-09 18:16:39', '2025-08-09 18:17:20', 1);
INSERT INTO `room` VALUES (176, 90, 5, 3, '任焱辉', 2, '2025-08-09 18:17:20', '2025-08-09 18:17:22', 0);

-- ----------------------------
-- Table structure for chat_message
-- ----------------------------
DROP TABLE IF EXISTS `chat_message`;
CREATE TABLE `chat_message`  (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `room_id` bigint NOT NULL,
  `sender_type` int NOT NULL,
  `sender_id` bigint NOT NULL,
  `message_type` int NULL DEFAULT 1,
  `content` text CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL,
  `create_time` datetime NULL DEFAULT CURRENT_TIMESTAMP,
  `is_deleted` tinyint NULL DEFAULT 0,
  PRIMARY KEY (`id`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 255 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of chat_message
-- ----------------------------
INSERT INTO `chat_message` VALUES (247, 174, 1, 3, 1, '患者已同意开始问诊，可以开始聊天了。', '2025-08-08 14:20:54', 0);
INSERT INTO `chat_message` VALUES (248, 174, 2, 5, 1, '您好，我是您的主治医生', '2025-08-08 14:21:01', 0);
INSERT INTO `chat_message` VALUES (249, 174, 1, 3, 1, '你好医生', '2025-08-08 14:21:13', 0);
INSERT INTO `chat_message` VALUES (250, 174, 2, 5, 2, 'http://szwlb5xin.hn-bkt.clouddn.com/chat_174_96564242-23b0-49ad-8cd3-0f4d308a06fc.png', '2025-08-08 14:21:25', 0);
INSERT INTO `chat_message` VALUES (251, 174, 1, 3, 1, '患者已同意开始问诊，可以开始聊天了。', '2025-08-08 14:21:43', 0);
INSERT INTO `chat_message` VALUES (252, 176, 1, 3, 1, '患者已同意开始问诊，可以开始聊天了。', '2025-08-09 18:17:24', 0);
INSERT INTO `chat_message` VALUES (253, 176, 1, 3, 1, 'szzs', '2025-08-09 18:17:27', 0);
INSERT INTO `chat_message` VALUES (254, 176, 2, 5, 1, 'ad', '2025-08-09 18:17:32', 0);

-- ----------------------------
-- Table structure for medical_record
-- ----------------------------
DROP TABLE IF EXISTS `medical_record`;
CREATE TABLE `medical_record`  (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `patient_id` bigint NOT NULL,
  `doctor_id` bigint NOT NULL,
  `doctor_description` text CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL,
  `is_purchasable` int NOT NULL,
  `create_time` timestamp NULL DEFAULT CURRENT_TIMESTAMP,
  `is_deleted` int NULL DEFAULT 0,
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `patient_id`(`patient_id` ASC) USING BTREE,
  INDEX `doctor_id`(`doctor_id` ASC) USING BTREE,
  CONSTRAINT `medical_record_ibfk_1` FOREIGN KEY (`patient_id`) REFERENCES `patient_attendant` (`id`) ON DELETE RESTRICT ON UPDATE RESTRICT,
  CONSTRAINT `medical_record_ibfk_2` FOREIGN KEY (`doctor_id`) REFERENCES `doctor_detail` (`id`) ON DELETE RESTRICT ON UPDATE RESTRICT
) ENGINE = InnoDB AUTO_INCREMENT = 50 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci ROW_FORMAT = DYNAMIC;

-- ----------------------------
-- Records of medical_record
-- ----------------------------
INSERT INTO `medical_record` VALUES (37, 3, 132, '1111', 0, '2025-08-08 11:28:13', 1);
INSERT INTO `medical_record` VALUES (38, 3, 132, '1111', 0, '2025-08-08 11:28:14', 1);
INSERT INTO `medical_record` VALUES (39, 3, 132, '患者有精神类疾病', 0, '2025-08-08 11:38:08', 0);
INSERT INTO `medical_record` VALUES (40, 3, 132, '结束啦啦啦', 0, '2025-08-08 11:43:57', 0);
INSERT INTO `medical_record` VALUES (41, 3, 132, '123', 0, '2025-08-08 11:46:43', 0);
INSERT INTO `medical_record` VALUES (42, 1, 132, '这是一个就诊记录', 0, '2025-08-08 11:50:42', 0);
INSERT INTO `medical_record` VALUES (43, 3, 132, '', 2, '2025-08-08 11:51:20', 0);
INSERT INTO `medical_record` VALUES (44, 3, 132, '', 2, '2025-08-08 12:03:07', 0);
INSERT INTO `medical_record` VALUES (45, 3, 132, '1231', 0, '2025-08-08 12:05:49', 0);
INSERT INTO `medical_record` VALUES (47, 1, 132, '123', 0, '2025-08-08 12:35:57', 0);
INSERT INTO `medical_record` VALUES (48, 1, 132, 'asdd', 0, '2025-08-08 13:45:29', 0);
INSERT INTO `medical_record` VALUES (49, 1, 132, '感冒了', 1, '2025-08-08 14:22:21', 0);

-- ----------------------------
-- Table structure for prescription
-- ----------------------------
DROP TABLE IF EXISTS `prescription`;
CREATE TABLE `prescription`  (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `drug_id` bigint NOT NULL,
  `drug_quantity` int NOT NULL,
  `medical_record_id` bigint NOT NULL,
  `create_time` timestamp NULL DEFAULT CURRENT_TIMESTAMP,
  `is_deleted` int NULL DEFAULT 0,
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `drug_id`(`drug_id` ASC) USING BTREE,
  INDEX `medical_record_id`(`medical_record_id` ASC) USING BTREE,
  CONSTRAINT `prescription_ibfk_1` FOREIGN KEY (`drug_id`) REFERENCES `drug` (`id`) ON DELETE RESTRICT ON UPDATE RESTRICT,
  CONSTRAINT `prescription_ibfk_2` FOREIGN KEY (`medical_record_id`) REFERENCES `medical_record` (`id`) ON DELETE RESTRICT ON UPDATE RESTRICT
) ENGINE = InnoDB AUTO_INCREMENT = 33 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci ROW_FORMAT = DYNAMIC;

-- ----------------------------
-- Records of prescription
-- ----------------------------
INSERT INTO `prescription` VALUES (17, 3, 1, 37, '2025-08-08 11:28:14', 0);
INSERT INTO `prescription` VALUES (18, 5, 1, 37, '2025-08-08 11:28:14', 0);
INSERT INTO `prescription` VALUES (19, 3, 1, 38, '2025-08-08 11:28:15', 0);
INSERT INTO `prescription` VALUES (20, 5, 1, 38, '2025-08-08 11:28:15', 0);
INSERT INTO `prescription` VALUES (21, 1, 4, 39, '2025-08-08 11:38:08', 0);
INSERT INTO `prescription` VALUES (22, 2, 5, 39, '2025-08-08 11:38:09', 0);
INSERT INTO `prescription` VALUES (23, 1, 4, 40, '2025-08-08 11:43:57', 0);
INSERT INTO `prescription` VALUES (24, 2, 6, 40, '2025-08-08 11:43:57', 0);
INSERT INTO `prescription` VALUES (25, 2, 1, 41, '2025-08-08 11:46:43', 0);
INSERT INTO `prescription` VALUES (26, 1, 4, 42, '2025-08-08 11:50:42', 0);
INSERT INTO `prescription` VALUES (27, 1, 4, 42, '2025-08-08 11:50:42', 0);
INSERT INTO `prescription` VALUES (28, 3, 1, 45, '2025-08-08 12:05:49', 0);
INSERT INTO `prescription` VALUES (29, 2, 1, 47, '2025-08-08 12:35:58', 0);
INSERT INTO `prescription` VALUES (30, 2, 3, 48, '2025-08-08 13:45:30', 0);
INSERT INTO `prescription` VALUES (31, 1, 3, 49, '2025-08-08 14:22:22', 0);
INSERT INTO `prescription` VALUES (32, 3, 2, 49, '2025-08-08 14:22:22', 0);

-- ----------------------------
-- Table structure for order_payment_record
-- ----------------------------
DROP TABLE IF EXISTS `order_payment_record`;
CREATE TABLE `order_payment_record`  (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `payer_id` bigint NULL DEFAULT NULL,
  `payment_status` int NULL DEFAULT 0,
  `payment_amount` decimal(10, 2) NULL DEFAULT NULL,
  `payment_time` timestamp NULL DEFAULT NULL,
  `payment_method` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL,
  `create_time` timestamp NULL DEFAULT CURRENT_TIMESTAMP,
  `medical_record_id` bigint NULL DEFAULT NULL,
  `order_source` int NOT NULL,
  `payment_gateway` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL,
  `is_deleted` int NULL DEFAULT 0,
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `payer_id`(`payer_id` ASC) USING BTREE,
  INDEX `prescription_id`(`medical_record_id` ASC) USING BTREE,
  CONSTRAINT `order_payment_record_ibfk_1` FOREIGN KEY (`payer_id`) REFERENCES `system_user` (`id`) ON DELETE RESTRICT ON UPDATE RESTRICT,
  CONSTRAINT `order_payment_record_ibfk_2` FOREIGN KEY (`medical_record_id`) REFERENCES `medical_record` (`id`) ON DELETE RESTRICT ON UPDATE RESTRICT
) ENGINE = InnoDB AUTO_INCREMENT = 75 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci ROW_FORMAT = DYNAMIC;

-- ----------------------------
-- Records of order_payment_record
-- ----------------------------
INSERT INTO `order_payment_record` VALUES (66, 3, 0, 61.20, NULL, NULL, '2025-08-08 11:31:42', 38, 1, NULL, 1);
INSERT INTO `order_payment_record` VALUES (67, 3, 0, 61.20, NULL, NULL, '2025-08-08 11:31:48', 37, 1, NULL, 1);
INSERT INTO `order_payment_record` VALUES (68, 3, 2, 35.80, '2025-08-08 12:37:38', '1', '2025-08-08 12:37:31', 47, 1, NULL, 0);
INSERT INTO `order_payment_record` VALUES (69, 3, 3, 133.80, '2025-08-08 12:38:45', '1', '2025-08-08 12:37:56', NULL, 0, NULL, 0);
INSERT INTO `order_payment_record` VALUES (70, 3, 0, 107.40, NULL, NULL, '2025-08-08 13:46:16', 48, 1, NULL, 1);
INSERT INTO `order_payment_record` VALUES (71, 3, 3, 76.80, '2025-08-08 14:16:04', '1', '2025-08-08 14:15:41', NULL, 0, NULL, 0);
INSERT INTO `order_payment_record` VALUES (72, 3, 0, 35.80, NULL, NULL, '2025-08-08 14:17:07', 47, 1, NULL, 1);
INSERT INTO `order_payment_record` VALUES (73, 3, 1, 170.10, '2025-08-08 14:56:30', '1', '2025-08-08 14:56:24', 49, 1, NULL, 0);
INSERT INTO `order_payment_record` VALUES (74, 3, 1, 48.00, '2025-08-09 18:15:37', '1', '2025-08-09 18:15:23', NULL, 0, NULL, 0);

-- ----------------------------
-- Table structure for medicine_order
-- ----------------------------
DROP TABLE IF EXISTS `medicine_order`;
CREATE TABLE `medicine_order`  (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `drug_id` bigint NULL DEFAULT NULL,
  `drug_quantity` int NULL DEFAULT NULL,
  `order_payment_record_id` bigint NULL DEFAULT NULL,
  `create_time` timestamp NULL DEFAULT CURRENT_TIMESTAMP,
  `is_deleted` int NULL DEFAULT 0,
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `drug_id`(`drug_id` ASC) USING BTREE,
  INDEX `order_payment_record_id`(`order_payment_record_id` ASC) USING BTREE,
  CONSTRAINT `medicine_order_ibfk_1` FOREIGN KEY (`drug_id`) REFERENCES `drug` (`id`) ON DELETE RESTRICT ON UPDATE RESTRICT,
  CONSTRAINT `medicine_order_ibfk_2` FOREIGN KEY (`order_payment_record_id`) REFERENCES `order_payment_record` (`id`) ON DELETE RESTRICT ON UPDATE RESTRICT
) ENGINE = InnoDB AUTO_INCREMENT = 169 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci ROW_FORMAT = DYNAMIC;

-- ----------------------------
-- Records of medicine_order
-- ----------------------------
INSERT INTO `medicine_order` VALUES (147, 3, 1, 66, '2025-08-08 11:31:44', 1);
INSERT INTO `medicine_order` VALUES (148, 5, 1, 66, '2025-08-08 11:31:44', 1);
INSERT INTO `medicine_order` VALUES (150, 3, 1, 67, '2025-08-08 11:31:47', 1);
INSERT INTO `medicine_order` VALUES (151, 5, 1, 67, '2025-08-08 11:31:47', 1);
INSERT INTO `medicine_order` VALUES (153, 2, 1, 68, '2025-08-08 12:37:32', 0);
INSERT INTO `medicine_order` VALUES (154, 6, 2, 69, '2025-08-08 12:37:56', 0);
INSERT INTO `medicine_order` VALUES (155, 7, 2, 69, '2025-08-08 12:37:56', 0);
INSERT INTO `medicine_order` VALUES (156, 8, 2, 69, '2025-08-08 12:37:56', 0);
INSERT INTO `medicine_order` VALUES (157, 2, 3, 70, '2025-08-08 13:46:18', 1);
INSERT INTO `medicine_order` VALUES (158, 6, 1, 71, '2025-08-08 14:15:41', 0);
INSERT INTO `medicine_order` VALUES (159, 7, 1, 71, '2025-08-08 14:15:42', 0);
INSERT INTO `medicine_order` VALUES (160, 8, 1, 71, '2025-08-08 14:15:42', 0);
INSERT INTO `medicine_order` VALUES (161, 9, 1, 71, '2025-08-08 14:15:42', 0);
INSERT INTO `medicine_order` VALUES (162, 2, 1, 72, '2025-08-08 14:17:09', 1);
INSERT INTO `medicine_order` VALUES (163, 1, 3, 73, '2025-08-08 14:56:27', 0);
INSERT INTO `medicine_order` VALUES (164, 3, 2, 73, '2025-08-08 14:56:27', 0);
INSERT INTO `medicine_order` VALUES (166, 6, 1, 74, '2025-08-09 18:15:23', 0);
INSERT INTO `medicine_order` VALUES (167, 7, 1, 74, '2025-08-09 18:15:23', 0);
INSERT INTO `medicine_order` VALUES (168, 9, 1, 74, '2025-08-09 18:15:23', 0);

-- ----------------------------
-- Table structure for appointment_payment_record
-- ----------------------------
DROP TABLE IF EXISTS `appointment_payment_record`;
CREATE TABLE `appointment_payment_record`  (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `payer_id` bigint NULL DEFAULT NULL,
  `payment_status` int NULL DEFAULT 0,
  `payment_amount` decimal(10, 2) NULL DEFAULT NULL,
  `payment_time` timestamp NULL DEFAULT NULL,
  `payment_method` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL,
  `create_time` timestamp NULL DEFAULT CURRENT_TIMESTAMP,
  `registration_id` bigint NULL DEFAULT NULL,
  `payment_gateway` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL,
  `is_deleted` int NULL DEFAULT 0,
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `payer_id`(`payer_id` ASC) USING BTREE,
  INDEX `registration_id`(`registration_id` ASC) USING BTREE,
  CONSTRAINT `appointment_payment_record_ibfk_1` FOREIGN KEY (`payer_id`) REFERENCES `system_user` (`id`) ON DELETE RESTRICT ON UPDATE RESTRICT,
  CONSTRAINT `appointment_payment_record_ibfk_2` FOREIGN KEY (`registration_id`) REFERENCES `registration` (`id`) ON DELETE RESTRICT ON UPDATE RESTRICT
) ENGINE = InnoDB AUTO_INCREMENT = 73 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci ROW_FORMAT = DYNAMIC;

-- ----------------------------
-- Records of appointment_payment_record
-- ----------------------------
INSERT INTO `appointment_payment_record` VALUES (67, 3, 1, 30.00, '2025-08-08 10:24:03', '1', '2025-08-08 10:23:53', 90, 'wechat', 0);
INSERT INTO `appointment_payment_record` VALUES (68, 3, 0, 30.00, NULL, NULL, '2025-08-08 10:33:51', 91, NULL, 1);
INSERT INTO `appointment_payment_record` VALUES (69, 3, 2, 30.00, '2025-08-08 10:35:05', '2', '2025-08-08 10:34:59', 92, 'wechat', 0);
INSERT INTO `appointment_payment_record` VALUES (70, 3, 1, 30.00, '2025-08-08 10:38:06', '1', '2025-08-08 10:37:30', 93, 'wechat', 0);
INSERT INTO `appointment_payment_record` VALUES (71, 3, 2, 100.00, '2025-08-08 14:13:50', '1', '2025-08-08 14:13:22', 94, 'wechat', 0);
INSERT INTO `appointment_payment_record` VALUES (72, 3, 2, 100.00, '2025-08-09 18:14:23', '1', '2025-08-09 18:14:16', 95, 'wechat', 0);

-- ----------------------------
-- Table structure for notification_message
-- ----------------------------
DROP TABLE IF EXISTS `notification_message`;
CREATE TABLE `notification_message`  (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `patient_id` bigint NOT NULL,
  `message_type` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL,
  `is_read` tinyint(1) NULL DEFAULT 0,
  `title` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL,
  `content` text CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL,
  `is_deleted` int NULL DEFAULT 0,
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `patient_id`(`patient_id` ASC) USING BTREE,
  CONSTRAINT `notification_message_ibfk_1` FOREIGN KEY (`patient_id`) REFERENCES `patient_attendant` (`id`) ON DELETE RESTRICT ON UPDATE RESTRICT
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci ROW_FORMAT = DYNAMIC;

-- ----------------------------
-- Records of notification_message
-- ----------------------------

SET FOREIGN_KEY_CHECKS = 1;
