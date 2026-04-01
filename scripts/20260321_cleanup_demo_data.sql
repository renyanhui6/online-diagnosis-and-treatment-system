USE onlinetreat;

START TRANSACTION;

-- 1) 清理 2026-03 联调生成、已失去业务价值的测试挂号与支付残留
DELETE FROM registration_payment_order
WHERE registration_id IN (96, 97, 98, 99, 100);

DELETE FROM registration_person_lock
WHERE registration_id IN (96, 97, 98, 99, 100);

DELETE FROM registration
WHERE id IN (96, 97, 98, 99, 100);

-- 2) 回收对应排班的已挂号计数，避免删除测试挂号后号源统计不一致
UPDATE schedule s
LEFT JOIN (
    SELECT schedule_id, COUNT(*) AS used_count
    FROM registration
    WHERE is_deleted = 0
      AND registration_status NOT IN (8)
      AND schedule_id IS NOT NULL
    GROUP BY schedule_id
) r ON r.schedule_id = s.id
SET s.current_appointment_count = COALESCE(r.used_count, 0)
WHERE s.id IN (1810, 1866, 1868, 2275, 2276);

-- 3) 将明显的联调占位聊天内容替换为可展示的真实问诊样例
UPDATE chat_message
SET content = CASE id
    WHEN 247 THEN '医生您好，我这两天咽痛比较明显。'
    WHEN 251 THEN '伴少量咳嗽，没有明显高热。'
    WHEN 252 THEN '医生您好，我这两天胸闷，活动后会更明显。'
    WHEN 253 THEN '偶尔伴有轻微咳嗽，没有明显胸痛。'
    WHEN 254 THEN '请补充症状持续时间、是否发热，以及既往心肺疾病史。'
    WHEN 255 THEN '最近月经推迟一周，同时下腹偶有隐痛。'
    WHEN 256 THEN '目前没有明显发热，也没有大量阴道出血。'
    WHEN 257 THEN '医生您好，昨天起咽痛伴低热。'
    WHEN 258 THEN '先补充体温最高多少度，是否伴有咳嗽或流涕。'
    WHEN 259 THEN '体温最高37.8度，伴轻微咳嗽，没有气促。'
    WHEN 260 THEN '您好医生，我从今天上午开始头晕并伴恶心。'
    WHEN 261 THEN '请补充是否有头痛、呕吐、肢体麻木或血压升高。'
    WHEN 262 THEN '有轻微头痛，没有肢体麻木，也没有呕吐。'
    WHEN 263 THEN '医生您好，孩子咳嗽两天并伴低热。'
    WHEN 264 THEN '请问最高体温多少度？是否伴有咳痰、喘息或精神差？'
    WHEN 265 THEN '最高38度，偶有少量白痰，精神尚可。'
    ELSE content
END
WHERE id IN (247, 251, 252, 253, 254, 255, 256, 257, 258, 259, 260, 261, 262, 263, 264, 265);

-- 4) 将历史病历中的假文本、空文本改成可用于展示和回溯的真实记录摘要
UPDATE medical_record
SET doctor_description = CASE id
    WHEN 37 THEN '主诉：反复头痛伴失眠3天。建议完善血压监测，规律作息，必要时神经内科复诊。'
    WHEN 38 THEN '主诉：腰背酸痛伴活动后加重1周。考虑肌肉劳损，建议注意休息并避免负重。'
    WHEN 39 THEN '主诉：情绪低落伴睡眠差2周，建议精神心理专科进一步评估并持续随访。'
    WHEN 40 THEN '主诉：咽痛伴咳嗽2天，无明显高热，考虑上呼吸道感染，建议对症处理。'
    WHEN 41 THEN '主诉：腹痛伴腹泻1天，注意清淡饮食与补液，若症状加重及时复诊。'
    WHEN 42 THEN '主诉：皮肤瘙痒伴红疹3天，考虑过敏相关表现，建议回避可疑诱因并观察变化。'
    WHEN 43 THEN '主诉：右膝关节疼痛反复发作1周，活动后加重，建议骨科进一步评估。'
    WHEN 44 THEN '主诉：头晕伴乏力2天，建议完善血压及血常规检查，注意休息。'
    WHEN 45 THEN '主诉：反酸伴上腹不适3天，建议规律饮食并观察是否与进食相关。'
    WHEN 47 THEN '主诉：发热伴咳痰2天，考虑呼吸道感染，建议监测体温及症状变化。'
    WHEN 48 THEN '主诉：鼻塞流涕伴咽痛2天，考虑普通感冒，建议对症治疗。'
    WHEN 49 THEN '主诉：咳嗽伴流涕3天，考虑上呼吸道感染，建议多饮水、清淡饮食并观察。'
    WHEN 50 THEN '主诉：胸闷伴心悸反复1周，活动后略加重，建议进一步完善心电图及相关检查。'
    WHEN 51 THEN '主诉：孕晚期胎动减少半天，建议尽快线下产检评估胎心及胎动情况。'
    WHEN 52 THEN '主诉：患儿咳嗽伴低热2天，精神尚可。查体后考虑上呼吸道感染，建议继续观察体温、清淡饮食、多饮水。'
    ELSE doctor_description
END
WHERE id IN (37, 38, 39, 40, 41, 42, 43, 44, 45, 47, 48, 49, 50, 51, 52);

COMMIT;
