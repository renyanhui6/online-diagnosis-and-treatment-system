package cn.edu.ncu.medical.ai;

import cn.edu.ncu.medical.entity.Department;
import cn.edu.ncu.medical.entity.DoctorDetail;
import cn.edu.ncu.medical.entity.Schedule;
import cn.edu.ncu.medical.entity.SubDepartment;
import cn.edu.ncu.medical.entity.dto.TriageChatRequest;
import cn.edu.ncu.medical.entity.dto.TriageChatResponse;
import cn.edu.ncu.medical.entity.dto.TriageRequest;
import cn.edu.ncu.medical.entity.dto.TriageResponse;
import cn.edu.ncu.medical.mapper.ScheduleMapper;
import cn.edu.ncu.medical.service.DepartmentService;
import cn.edu.ncu.medical.service.DoctorDetailService;
import cn.edu.ncu.medical.service.SubDepartmentService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.sql.Date;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Deque;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.function.Function;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
/**
 * 本地分诊回退引擎。
 *
 * 这个类存在的目的不是替代大模型，而是在以下场景下兜底：
 * 1. 本地开发环境没有配置 DeepSeek Key
 * 2. 在线模型调用失败
 * 3. 需要在答辩、联调、离线演示时保证 AI 助手仍然可用
 *
 * 它的核心思路是：
 * - 从数据库读取院内真实科室、子科室、医生和未来排班
 * - 用规则和打分机制做“轻量分诊”
 * - 输出结构与在线 Agent 保持一致
 *
 * 这样上层前端和 Controller 不需要关心当前到底走的是在线 Agent 还是本地回退。
 */
public class LocalTriageAdvisor {

    private static final Duration SESSION_TTL = Duration.ofHours(6);
    private static final int SESSION_MESSAGE_LIMIT = 8;
    private static final int FUTURE_DAYS = 7;
    private static final List<String> EMERGENCY_KEYWORDS = List.of(
            "呼吸困难", "喘不上气", "胸痛加重", "持续胸痛", "大出血", "意识不清", "意识障碍",
            "昏迷", "抽搐", "偏瘫", "口角歪斜", "突发失语", "剧烈头痛", "外伤昏迷",
            "孕妇出血", "胎动明显减少"
    );
    private static final List<String> BODY_PART_HINTS = List.of(
            "头", "胸", "心", "肺", "胃", "腹", "肠", "腰", "背", "关节", "眼", "月经",
            "阴道", "子宫", "卵巢", "怀孕", "宝宝", "儿童", "新生儿"
    );
    private static final List<String> CHILD_CONTEXT_HINTS = List.of("儿童", "小孩", "孩子", "宝宝", "婴儿", "新生儿");
    private static final List<String> GENERIC_FOLLOW_UP_QUESTIONS = List.of(
            "最不舒服的部位具体在哪里？",
            "症状大概持续了多久，是突然出现还是逐渐加重？",
            "除了当前症状外，是否还伴有发热、疼痛、咳嗽、恶心、出血等情况？"
    );
    private static final Map<String, List<String>> SUB_DEPARTMENT_HINTS = createSubDepartmentHints();

    private final DepartmentService departmentService;
    private final SubDepartmentService subDepartmentService;
    private final DoctorDetailService doctorDetailService;
    private final ScheduleMapper scheduleMapper;
    private final TriageExternalSearchTool triageExternalSearchTool;

    private final ConcurrentMap<String, SessionMemory> sessions = new ConcurrentHashMap<>();

    /**
     * 单轮分诊入口。
     *
     * 单轮接口本质上复用多轮分诊逻辑：先把请求转成一条聊天消息，再走 chat。
     * 这样可以避免维护两套不同的推荐逻辑，保证单轮和多轮输出口径一致。
     */
    public TriageResponse triage(TriageRequest request) {
        TriageChatRequest chatRequest = new TriageChatRequest();
        chatRequest.setSessionId("triage-" + UUID.randomUUID());
        chatRequest.setAge(request.getAge());
        chatRequest.setGender(request.getGender());
        chatRequest.setMessage(buildSingleShotMessage(request));

        TriageChatResponse chatResponse = chat(chatRequest);
        TriageResponse response = new TriageResponse();
        response.setRecommendedDepartments(chatResponse.getRecommendedSubDepartments().isEmpty()
                ? chatResponse.getRecommendedDepartments()
                : chatResponse.getRecommendedSubDepartments());
        response.setRationale(chatResponse.getAssistantMessage());
        response.setDisclaimer(chatResponse.getDisclaimer());
        response.setSource(chatResponse.getSource());
        return response;
    }

    /**
     * 多轮分诊入口。
     *
     * 处理顺序：
     * 1. 清理过期会话
     * 2. 根据 sessionId 找到或创建当前会话记忆
     * 3. 追加本轮消息和年龄/性别等元信息
     * 4. 做院内匹配、急症识别、追问或推荐
     * 5. 返回和在线 Agent 一致的数据结构
     */
    public TriageChatResponse chat(TriageChatRequest request) {
        cleanupExpiredSessions();

        String sessionId = StringUtils.hasText(request.getSessionId())
                ? request.getSessionId()
                : "triage-" + UUID.randomUUID();
        SessionMemory memory = sessions.computeIfAbsent(sessionId, key -> new SessionMemory());
        memory.appendMessage(request.getMessage());
        memory.mergeMeta(request.getAge(), request.getGender());

        Decision decision = analyze(memory);

        TriageChatResponse response = new TriageChatResponse();
        response.setSessionId(sessionId);
        response.setAssistantMessage(decision.assistantMessage());
        response.setNeedMoreInfo(decision.needMoreInfo());
        response.setEmergency(decision.emergency());
        response.setUsedExternalKnowledge(decision.usedExternalKnowledge());
        response.setConfidence(decision.confidence());
        response.setRationale(decision.rationale());
        response.setFollowUpQuestions(decision.followUpQuestions());
        response.setRecommendedDepartments(decision.recommendedDepartments());
        response.setRecommendedSubDepartments(decision.recommendedSubDepartments());
        response.setSource("local-fallback");
        return response;
    }

    /**
     * 本地分诊的核心决策过程。
     *
     * 这里不做“医学诊断”，只做“挂号方向判断”：
     * - 先识别急症
     * - 再生成院内科室候选
     * - 信息不足就追问
     * - 信息足够就给推荐
     * - 必要时补充外部公开知识，但外部知识只作为辅助，不作为主数据源
     */
    private Decision analyze(SessionMemory memory) {
        String fullText = memory.joinedText();
        String normalizedText = normalize(fullText);
        boolean emergency = containsAny(normalizedText, EMERGENCY_KEYWORDS);

        HospitalSnapshot snapshot = loadHospitalSnapshot();
        List<Candidate> rankedCandidates = rankCandidates(snapshot, normalizedText, memory.age, memory.gender);
        List<Candidate> matchedCandidates = rankedCandidates.stream()
                .filter(candidate -> candidate.score() > 0)
                .toList();

        Candidate top = matchedCandidates.isEmpty() ? null : matchedCandidates.get(0);
        Candidate second = matchedCandidates.size() > 1 ? matchedCandidates.get(1) : null;

        boolean insufficientInput = normalizedText.length() < 6 || !containsAny(normalizedText, BODY_PART_HINTS);
        boolean lowConfidence = top == null || top.score() < 7;
        boolean ambiguous = top != null && second != null && top.score() - second.score() <= 2;
        boolean needMoreInfo = !emergency && (insufficientInput || lowConfidence || ambiguous);

        boolean shouldUseExternal = normalizedText.length() >= 8 && (lowConfidence || ambiguous);
        String externalSummary = shouldUseExternal ? lookupExternalKnowledge(fullText) : null;
        boolean usedExternalKnowledge = StringUtils.hasText(externalSummary);

        List<String> recommendedSubDepartments = matchedCandidates.stream()
                .limit(needMoreInfo ? 2 : 3)
                .map(candidate -> candidate.subDepartment().getDepartmentName())
                .toList();
        List<String> recommendedDepartments = matchedCandidates.stream()
                .map(candidate -> candidate.parentDepartment().getDepartmentName())
                .filter(StringUtils::hasText)
                .distinct()
                .limit(3)
                .toList();

        List<String> followUpQuestions = needMoreInfo
                ? buildFollowUpQuestions(normalizedText, memory, top)
                : List.of();
        int confidence = calculateConfidence(top, second, needMoreInfo, emergency);
        String assistantMessage = buildAssistantMessage(top, recommendedDepartments, recommendedSubDepartments,
                emergency, needMoreInfo, followUpQuestions, usedExternalKnowledge);
        String rationale = buildRationale(top, second, emergency, usedExternalKnowledge, externalSummary);

        return new Decision(
                emergency,
                needMoreInfo,
                usedExternalKnowledge,
                confidence,
                assistantMessage,
                rationale,
                followUpQuestions,
                recommendedDepartments,
                recommendedSubDepartments
        );
    }

    /**
     * 读取当前医院真实可用的分诊基础数据。
     *
     * 这里查的是实时数据，不是写死枚举：
     * - 一级科室
     * - 子科室
     * - 医生详情
     * - 未来 7 天排班
     *
     * 后面所有推荐，都是基于这批真实数据做的。
     */
    private HospitalSnapshot loadHospitalSnapshot() {
        List<Department> departments = departmentService.list();
        List<SubDepartment> subDepartments = subDepartmentService.list();
        List<DoctorDetail> doctors = doctorDetailService.list();
        List<Schedule> futureSchedules = scheduleMapper.selectList(new LambdaQueryWrapper<Schedule>()
                .ge(Schedule::getScheduleDate, Date.valueOf(LocalDate.now()))
                .le(Schedule::getScheduleDate, Date.valueOf(LocalDate.now().plusDays(FUTURE_DAYS))));

        Map<Long, Department> departmentMap = departments.stream()
                .collect(Collectors.toMap(Department::getId, Function.identity(), (a, b) -> a));
        Map<Long, Long> doctorCountMap = doctors.stream()
                .filter(doctor -> doctor.getSubDepartmentId() != null)
                .collect(Collectors.groupingBy(DoctorDetail::getSubDepartmentId, Collectors.counting()));
        Map<Long, Long> scheduleCountMap = futureSchedules.stream()
                .filter(schedule -> schedule.getSubDepartmentId() != null)
                .collect(Collectors.groupingBy(Schedule::getSubDepartmentId, Collectors.counting()));

        return new HospitalSnapshot(subDepartments, departmentMap, doctorCountMap, scheduleCountMap);
    }

    /**
     * 为每个子科室计算一个匹配分数，并产出排序后的候选列表。
     *
     * 打分依据主要包括：
     * - 用户是否直接提到科室名称
     * - 症状关键词是否和该子科室的常见场景匹配
     * - 用户描述是否命中院内诊疗范围和科室特色
     * - 年龄/性别是否与该科室合理匹配
     * - 该科室是否存在真实医生和未来排班
     */
    private List<Candidate> rankCandidates(HospitalSnapshot snapshot, String normalizedText, Integer age, String gender) {
        List<Candidate> candidates = new ArrayList<>();
        for (SubDepartment subDepartment : snapshot.subDepartments()) {
            Department parentDepartment = snapshot.departmentMap().get(subDepartment.getParentDepartmentId());
            if (parentDepartment == null) {
                continue;
            }

            LinkedHashSet<String> reasons = new LinkedHashSet<>();
            int score = 0;
            String parentName = defaultText(parentDepartment.getDepartmentName(), "");
            String subName = defaultText(subDepartment.getDepartmentName(), "");
            String searchable = normalize(parentName + " " + subName + " " + defaultText(subDepartment.getTreatmentScope(), "")
                    + " " + defaultText(subDepartment.getDepartmentFeatures(), "") + " " + defaultText(subDepartment.getDescription(), ""));

            if (normalizedText.contains(normalize(parentName))) {
                score += 8;
                reasons.add("用户直接提到了“" + parentName + "”");
            }
            if (normalizedText.contains(normalize(subName))) {
                score += 12;
                reasons.add("用户直接提到了“" + subName + "”");
            }

            for (String keyword : SUB_DEPARTMENT_HINTS.getOrDefault(subName, List.of())) {
                if (normalizedText.contains(normalize(keyword))) {
                    score += 8;
                    reasons.add("症状与“" + subName + "”常见场景匹配：" + keyword);
                }
            }

            for (String token : extractDynamicTokens(normalizedText, subDepartment, parentDepartment)) {
                if (searchable.contains(token)) {
                    score += 5;
                    reasons.add("院内诊疗范围匹配：" + token);
                }
            }

            boolean childContext = containsAny(normalizedText, CHILD_CONTEXT_HINTS);
            if (age != null) {
                if (age <= 1 && subName.contains("新生儿")) {
                    score += 12;
                    reasons.add("年龄更符合新生儿就诊");
                } else if (age <= 14 && parentName.contains("儿科")) {
                    score += 8;
                    reasons.add("年龄更符合儿科就诊");
                } else if (age > 14 && parentName.contains("儿科") && !childContext) {
                    score -= 12;
                }
            }

            if (containsAny(normalizedText, List.of("怀孕", "妊娠", "孕", "产检", "宫缩", "胎动", "见红")) && subName.contains("产科")) {
                score += 12;
                reasons.add("内容包含孕产相关信息");
            }
            if (containsAny(normalizedText, List.of("月经", "痛经", "白带", "阴道", "妇科", "子宫", "卵巢")) && subName.contains("妇科")) {
                score += 12;
                reasons.add("内容包含妇科相关信息");
            }
            if ("男".equals(gender) && (subName.contains("妇科") || subName.contains("产科"))) {
                score -= 12;
            }

            long doctorCount = snapshot.doctorCountMap().getOrDefault(subDepartment.getId(), 0L);
            long futureScheduleCount = snapshot.scheduleCountMap().getOrDefault(subDepartment.getId(), 0L);
            if (score > 0 && doctorCount > 0) {
                score += 1;
            }
            if (score > 0 && futureScheduleCount > 0) {
                score += 2;
                reasons.add("院内未来7天有真实排班");
            }

            candidates.add(new Candidate(parentDepartment, subDepartment, score, List.copyOf(reasons), doctorCount, futureScheduleCount));
        }

        return candidates.stream()
                .sorted(Comparator.comparingInt(Candidate::score).reversed()
                        .thenComparingLong(Candidate::futureScheduleCount).reversed()
                        .thenComparingLong(Candidate::doctorCount).reversed()
                        .thenComparing(candidate -> candidate.subDepartment().getDepartmentName(), Comparator.nullsLast(String::compareTo)))
                .toList();
    }

    private List<String> buildFollowUpQuestions(String normalizedText, SessionMemory memory, Candidate top) {
        LinkedHashSet<String> questions = new LinkedHashSet<>();
        if (!containsAny(normalizedText, BODY_PART_HINTS)) {
            questions.add("最不舒服的部位具体在哪里，比如胸口、咽喉、腹部、腰背、眼部还是妇科相关部位？");
        }
        if (!containsDuration(normalizedText)) {
            questions.add("症状持续了多久，是今天刚开始，还是已经几天/几周了？");
        }
        if (!containsAny(normalizedText, List.of("发热", "咳嗽", "疼", "恶心", "呕吐", "腹泻", "出血", "头晕", "胸闷", "心慌"))) {
            questions.add("还伴有哪些症状，比如发热、疼痛、咳嗽、恶心、出血、头晕或胸闷？");
        }
        if (memory.age == null && containsAny(normalizedText, List.of("宝宝", "孩子", "儿童", "新生儿"))) {
            questions.add("患者大概多大？是新生儿、儿童还是成人？");
        }
        if (top != null && top.parentDepartment().getDepartmentName().contains("眼科")) {
            questions.add("主要是视力问题（近视、散光、视物模糊），还是飞蚊、视物变形、眼底类问题？");
        }
        if (top != null && top.parentDepartment().getDepartmentName().contains("外科") && !containsAny(normalizedText, List.of("外伤", "摔伤", "扭伤", "骨折", "包块"))) {
            questions.add("是外伤、骨关节问题，还是腹部包块/阑尾一类急腹症表现？");
        }
        if (questions.isEmpty()) {
            questions.addAll(GENERIC_FOLLOW_UP_QUESTIONS);
        }
        return questions.stream().limit(3).toList();
    }

    private int calculateConfidence(Candidate top, Candidate second, boolean needMoreInfo, boolean emergency) {
        if (emergency) {
            return 90;
        }
        if (top == null) {
            return 20;
        }
        int confidence = Math.min(92, 40 + top.score() * 4);
        if (second != null) {
            confidence -= Math.min(15, Math.max(0, 8 - (top.score() - second.score())) * 2);
        }
        if (needMoreInfo) {
            confidence -= 18;
        }
        return Math.max(25, confidence);
    }

    private String buildAssistantMessage(Candidate top,
                                         List<String> recommendedDepartments,
                                         List<String> recommendedSubDepartments,
                                         boolean emergency,
                                         boolean needMoreInfo,
                                         List<String> followUpQuestions,
                                         boolean usedExternalKnowledge) {
        if (emergency) {
            String recommendation = !recommendedSubDepartments.isEmpty()
                    ? recommendedSubDepartments.get(0)
                    : (!recommendedDepartments.isEmpty() ? recommendedDepartments.get(0) : "急诊/线下急救");
            return "你描述的情况里带有急危重症信号，建议先尽快线下急诊处理，不要只等待线上分诊。"
                    + "如果需要挂号方向，当前更接近“" + recommendation + "”。";
        }

        if (top == null) {
            return "目前仅凭这点信息还不足以判断挂号方向。请先补充症状部位、持续时间和伴随症状，我再继续缩小科室范围。";
        }

        if (needMoreInfo) {
            StringBuilder builder = new StringBuilder("目前更偏向“")
                    .append(top.subDepartment().getDepartmentName())
                    .append("”");
            if (!recommendedSubDepartments.isEmpty() && recommendedSubDepartments.size() > 1) {
                builder.append("，备选还有“").append(recommendedSubDepartments.get(1)).append("”");
            }
            builder.append("。不过信息还不够完整，我想再确认几点：");
            if (!followUpQuestions.isEmpty()) {
                builder.append(followUpQuestions.get(0));
            }
            if (usedExternalKnowledge) {
                builder.append(" 我也补充参考了院外公开资料，但最终仍以院内真实科室和排班为准。");
            }
            return builder.toString();
        }

        StringBuilder builder = new StringBuilder("结合你目前提供的信息，优先建议挂“")
                .append(top.subDepartment().getDepartmentName())
                .append("”");
        if (StringUtils.hasText(top.parentDepartment().getDepartmentName())) {
            builder.append("（").append(top.parentDepartment().getDepartmentName()).append("）");
        }
        if (top.futureScheduleCount() > 0) {
            builder.append("，系统里未来7天也查到了真实排班。");
        } else {
            builder.append("。");
        }
        if (recommendedSubDepartments.size() > 1) {
            builder.append("如果主诉并不典型，也可以把“")
                    .append(recommendedSubDepartments.get(1))
                    .append("”作为备选。");
        }
        if (usedExternalKnowledge) {
            builder.append(" 这次判断还参考了少量院外公开资料，但最终排序仍以内院科室信息为准。");
        }
        return builder.toString();
    }

    private String buildRationale(Candidate top, Candidate second, boolean emergency, boolean usedExternalKnowledge, String externalSummary) {
        StringBuilder builder = new StringBuilder();
        if (top == null) {
            builder.append("当前输入信息不足，尚未形成稳定的院内科室匹配结果。");
        } else {
            builder.append("本次推荐主要依据院内真实子科室的诊疗范围、科室特色、医生配置和未来7天排班进行匹配。");
            if (!top.reasons().isEmpty()) {
                builder.append("首选“").append(top.subDepartment().getDepartmentName()).append("”的原因：")
                        .append(String.join("；", top.reasons()));
            }
            if (second != null && second.score() > 0) {
                builder.append("；备选“").append(second.subDepartment().getDepartmentName())
                        .append("”也有一定匹配，但优先级略低。");
            }
        }
        if (emergency) {
            builder.append("同时检测到了急症关键词，因此优先建议线下急诊。");
        }
        if (usedExternalKnowledge && StringUtils.hasText(externalSummary)) {
            builder.append(" 外部知识仅作补充参考：").append(externalSummary);
        }
        return builder.toString();
    }

    private String lookupExternalKnowledge(String fullText) {
        String result = triageExternalSearchTool.searchMedicalKnowledge(fullText);
        if (!StringUtils.hasText(result)) {
            return null;
        }
        if (result.contains("未配置") || result.contains("不支持") || result.contains("失败") || result.contains("没有返回")) {
            return null;
        }
        String normalized = result.replace("\r", " ").replace("\n", " ").trim();
        return normalized.length() <= 220 ? normalized : normalized.substring(0, 220) + "...";
    }

    private Set<String> extractDynamicTokens(String normalizedText, SubDepartment subDepartment, Department parentDepartment) {
        Set<String> tokens = new LinkedHashSet<>();
        for (String token : splitChinesePhrases(normalizedText)) {
            if (token.length() >= 2) {
                tokens.add(token);
            }
        }
        for (String token : splitChinesePhrases(defaultText(subDepartment.getTreatmentScope(), ""))) {
            if (normalizedText.contains(normalize(token))) {
                tokens.add(normalize(token));
            }
        }
        for (String token : splitChinesePhrases(defaultText(subDepartment.getDepartmentFeatures(), ""))) {
            if (normalizedText.contains(normalize(token))) {
                tokens.add(normalize(token));
            }
        }
        if (StringUtils.hasText(parentDepartment.getDepartmentName()) && normalizedText.contains(normalize(parentDepartment.getDepartmentName()))) {
            tokens.add(normalize(parentDepartment.getDepartmentName()));
        }
        return tokens;
    }

    private boolean containsDuration(String normalizedText) {
        return containsAny(normalizedText, List.of(
                "小时", "天", "周", "月", "年", "今天", "昨天", "今早", "昨晚", "近期", "一直", "突然"
        ));
    }

    private String buildSingleShotMessage(TriageRequest request) {
        List<String> pieces = new ArrayList<>();
        if (StringUtils.hasText(request.getDescription())) {
            pieces.add(request.getDescription());
        }
        if (request.getSymptoms() != null && !request.getSymptoms().isEmpty()) {
            pieces.add(String.join("，", request.getSymptoms()));
        }
        return String.join("；", pieces);
    }

    private void cleanupExpiredSessions() {
        Instant expireBefore = Instant.now().minus(SESSION_TTL);
        sessions.entrySet().removeIf(entry -> entry.getValue().updatedAt.isBefore(expireBefore));
    }

    private boolean containsAny(String normalizedText, List<String> keywords) {
        return keywords.stream()
                .map(this::normalize)
                .filter(StringUtils::hasText)
                .anyMatch(normalizedText::contains);
    }

    private String normalize(String text) {
        if (!StringUtils.hasText(text)) {
            return "";
        }
        return text.replaceAll("\\s+", "")
                .replace("，", "")
                .replace(",", "")
                .replace("。", "")
                .replace("；", "")
                .replace(";", "")
                .replace("、", "")
                .toLowerCase();
    }

    private List<String> splitChinesePhrases(String text) {
        if (!StringUtils.hasText(text)) {
            return List.of();
        }
        return List.of(text.split("[，,。；;、\\s]+")).stream()
                .map(String::trim)
                .filter(StringUtils::hasText)
                .collect(Collectors.toList());
    }

    private String defaultText(String value, String fallback) {
        return StringUtils.hasText(value) ? value : fallback;
    }

    private static Map<String, List<String>> createSubDepartmentHints() {
        Map<String, List<String>> hints = new HashMap<>();
        hints.put("心血管内科", List.of("胸痛", "胸闷", "心悸", "心慌", "高血压", "血压高", "冠心病", "心律失常"));
        hints.put("呼吸内科", List.of("咳嗽", "咳痰", "气喘", "哮喘", "呼吸困难", "发热", "低热", "胸闷", "肺炎"));
        hints.put("消化内科", List.of("腹痛", "胃痛", "反酸", "腹胀", "腹泻", "便秘", "恶心", "呕吐", "肝区", "消化不良"));
        hints.put("骨科", List.of("骨折", "关节痛", "腰痛", "颈椎", "扭伤", "摔伤", "肢体疼痛", "脊柱", "关节炎"));
        hints.put("神经外科", List.of("脑外伤", "头部外伤", "抽搐", "意识障碍", "昏迷", "脑出血", "脑肿瘤", "肢体无力"));
        hints.put("普外科", List.of("阑尾", "急腹症", "疝气", "包块", "胆囊", "胃肠道肿瘤", "腹部包块"));
        hints.put("妇科", List.of("月经", "痛经", "白带", "阴道", "妇科", "子宫", "卵巢", "盆腔"));
        hints.put("产科", List.of("怀孕", "妊娠", "产检", "分娩", "宫缩", "胎动", "见红", "孕吐"));
        hints.put("新生儿科", List.of("新生儿", "黄疸", "早产", "喂养困难", "出生后", "呼吸窘迫"));
        hints.put("小儿内科", List.of("儿童", "小孩", "宝宝", "发育", "腹泻", "肺炎", "发热", "咳嗽"));
        hints.put("小儿外科", List.of("先天性", "畸形", "儿童外伤", "儿童急腹症", "小儿外科"));
        hints.put("屈光矫正科", List.of("近视", "远视", "散光", "视力下降", "配镜", "激光"));
        hints.put("眼底病科", List.of("眼底", "飞蚊", "黄斑", "视网膜", "视物变形", "糖尿病视网膜病变"));
        return hints;
    }

    private record HospitalSnapshot(List<SubDepartment> subDepartments,
                                    Map<Long, Department> departmentMap,
                                    Map<Long, Long> doctorCountMap,
                                    Map<Long, Long> scheduleCountMap) {
    }

    private record Candidate(Department parentDepartment,
                             SubDepartment subDepartment,
                             int score,
                             List<String> reasons,
                             long doctorCount,
                             long futureScheduleCount) {
    }

    private record Decision(boolean emergency,
                            boolean needMoreInfo,
                            boolean usedExternalKnowledge,
                            int confidence,
                            String assistantMessage,
                            String rationale,
                            List<String> followUpQuestions,
                            List<String> recommendedDepartments,
                            List<String> recommendedSubDepartments) {
    }

    private static final class SessionMemory {
        private final Deque<String> userMessages = new ArrayDeque<>();
        private Instant updatedAt = Instant.now();
        private Integer age;
        private String gender;

        private void appendMessage(String message) {
            if (!StringUtils.hasText(message)) {
                return;
            }
            userMessages.addLast(message.trim());
            while (userMessages.size() > SESSION_MESSAGE_LIMIT) {
                userMessages.removeFirst();
            }
            updatedAt = Instant.now();
        }

        private void mergeMeta(Integer age, String gender) {
            if (age != null) {
                this.age = age;
            }
            if (StringUtils.hasText(gender)) {
                this.gender = gender.trim();
            }
            updatedAt = Instant.now();
        }

        private String joinedText() {
            return userMessages.stream()
                    .filter(Objects::nonNull)
                    .collect(Collectors.joining("；"));
        }
    }
}
