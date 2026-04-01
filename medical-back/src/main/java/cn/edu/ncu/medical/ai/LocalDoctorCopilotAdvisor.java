package cn.edu.ncu.medical.ai;

import cn.edu.ncu.medical.entity.dto.DoctorAiRequest;
import cn.edu.ncu.medical.entity.dto.DoctorAiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * 医生端协作 Agent 的本地回退实现。
 *
 * 这层的目标不是取代大模型，而是在以下场景下兜底：
 * 1. 没有配置在线模型 Key
 * 2. 在线 Agent 调用失败
 * 3. 需要在本地联调、离线演示、答辩环境中保持功能可用
 *
 * 它重点做四件事：
 * - 结构化提取当前聊天里的关键病史
 * - 检查缺失项
 * - 提醒风险点
 * - 输出可直接回填的病历草稿
 */
@Component
@RequiredArgsConstructor
public class LocalDoctorCopilotAdvisor {

    private static final Pattern DURATION_PATTERN = Pattern.compile("(\\d+|一|二|三|四|五|六|七|八|九|十|半)(分钟|小时|天|周|月|年)");

    private static final List<String> SITE_KEYWORDS = List.of(
            "头", "头痛", "胸", "胸口", "咽", "喉咙", "鼻", "眼", "耳", "腹", "胃", "肚子",
            "腰", "背", "关节", "腿", "手", "皮肤", "外阴", "阴道", "尿道"
    );
    private static final List<String> SEVERITY_KEYWORDS = List.of(
            "剧烈", "明显", "严重", "加重", "无法", "频繁", "持续", "反复", "越来越"
    );
    private static final List<String> HISTORY_KEYWORDS = List.of(
            "既往", "以前", "平时", "慢性", "高血压", "糖尿病", "冠心病", "哮喘", "手术", "住院"
    );
    private static final List<String> ALLERGY_KEYWORDS = List.of("过敏", "青霉素过敏", "药物过敏", "食物过敏");
    private static final List<String> MEDICATION_KEYWORDS = List.of("吃了", "服用", "用药", "打针", "输液", "布洛芬", "阿莫西林");
    private static final List<String> VITALS_KEYWORDS = List.of("体温", "血压", "心率", "脉搏", "血氧");

    private static final Map<String, List<String>> FOCUS_KEYWORDS = Map.of(
            "胸痛胸闷", List.of("胸痛", "胸闷", "心悸", "气短", "呼吸困难"),
            "呼吸道症状", List.of("咳嗽", "咳痰", "发热", "咽痛", "鼻塞", "低热"),
            "腹痛消化道症状", List.of("腹痛", "胃痛", "恶心", "呕吐", "腹泻", "便血", "黑便"),
            "神经系统症状", List.of("头痛", "头晕", "眩晕", "抽搐", "麻木", "失眠"),
            "妇产相关症状", List.of("月经", "痛经", "白带", "怀孕", "妊娠", "阴道出血"),
            "泌尿系统症状", List.of("尿频", "尿急", "尿痛", "血尿", "腰痛")
    );

    private final DoctorCopilotTools doctorCopilotTools;

    public DoctorAiResponse assist(DoctorAiRequest request) {
        DoctorCopilotTools.ConsultationContext context = doctorCopilotTools.loadConsultationContext(
                request.getRoomId(), request.getRegistrationId()
        );

        String patientText = context.patientTranscriptText();
        String fullText = context.fullTranscriptText();
        String normalizedPatientText = normalize(patientText);

        List<String> riskAlerts = doctorCopilotTools.detectClinicalRiskSignals(
                fullText, context.getPatientAge(), context.getPatientGender()
        );
        boolean highRisk = !riskAlerts.isEmpty();

        List<String> assessmentFocuses = buildAssessmentFocuses(normalizedPatientText, highRisk);
        List<String> missingInfoItems = buildMissingInfoItems(normalizedPatientText, context, assessmentFocuses);
        List<String> followUpQuestions = buildFollowUpQuestions(normalizedPatientText, context, missingInfoItems, assessmentFocuses);
        List<String> recommendedActions = buildRecommendedActions(missingInfoItems, riskAlerts, context);
        List<String> prescriptionSafetyHints = buildPrescriptionSafetyHints(normalizedPatientText, context, highRisk);
        List<String> historicalRecordHighlights = buildHistoryHighlights(context.getHistoryRecords());
        List<String> usedDataSources = buildDataSources(context);

        String chiefComplaintDraft = buildChiefComplaintDraft(patientText);
        String presentIllnessDraft = buildPresentIllnessDraft(patientText, missingInfoItems, riskAlerts, context);
        String structuredRecordDraft = buildStructuredRecordDraft(
                chiefComplaintDraft,
                presentIllnessDraft,
                missingInfoItems,
                assessmentFocuses,
                riskAlerts,
                prescriptionSafetyHints
        );

        boolean needMoreInfo = !missingInfoItems.isEmpty();
        int confidence = calculateConfidence(missingInfoItems, riskAlerts, historicalRecordHighlights, chiefComplaintDraft, assessmentFocuses);

        DoctorAiResponse response = new DoctorAiResponse();
        response.setSuggestion(buildSuggestion(needMoreInfo, highRisk, assessmentFocuses));
        response.setFollowUpQuestions(limit(followUpQuestions, 5));
        response.setMissingInfoItems(limit(missingInfoItems, 6));
        response.setRiskAlerts(limit(riskAlerts, 5));
        response.setRecommendedActions(limit(recommendedActions, 5));
        response.setAssessmentFocuses(limit(assessmentFocuses, 4));
        response.setPrescriptionSafetyHints(limit(prescriptionSafetyHints, 5));
        response.setHistoricalRecordHighlights(limit(historicalRecordHighlights, 4));
        response.setUsedDataSources(usedDataSources);
        response.setChiefComplaintDraft(chiefComplaintDraft);
        response.setPresentIllnessDraft(presentIllnessDraft);
        response.setStructuredRecordDraft(structuredRecordDraft);
        response.setConfidence(confidence);
        response.setNeedMoreInfo(needMoreInfo);
        response.setHighRisk(highRisk);
        response.setSource("local-doctor-copilot");
        return response;
    }

    private List<String> buildMissingInfoItems(String normalizedPatientText,
                                               DoctorCopilotTools.ConsultationContext context,
                                               List<String> assessmentFocuses) {
        LinkedHashSet<String> missing = new LinkedHashSet<>();

        if (!DURATION_PATTERN.matcher(normalizedPatientText).find()) {
            missing.add("症状起始时间或持续时长");
        }
        if (!containsAny(normalizedPatientText, SITE_KEYWORDS)) {
            missing.add("最不适的具体部位");
        }
        if (!containsAny(normalizedPatientText, SEVERITY_KEYWORDS)) {
            missing.add("症状严重程度或变化趋势");
        }
        if (!containsAny(normalizedPatientText, HISTORY_KEYWORDS)) {
            missing.add("既往病史或基础疾病");
        }
        if (!containsAny(normalizedPatientText, ALLERGY_KEYWORDS)) {
            missing.add("药物或食物过敏史");
        }
        if (!containsAny(normalizedPatientText, MEDICATION_KEYWORDS)) {
            missing.add("就诊前是否已自行用药或处理");
        }
        if (assessmentFocuses.stream().anyMatch(item -> item.contains("呼吸")) && !containsAny(normalizedPatientText, VITALS_KEYWORDS)) {
            missing.add("体温或呼吸相关生命体征");
        }
        if ("女".equals(context.getPatientGender())
                && context.getPatientAge() != null
                && context.getPatientAge() >= 12
                && context.getPatientAge() <= 55
                && assessmentFocuses.stream().anyMatch(item -> item.contains("妇产"))) {
            if (!containsAny(normalizedPatientText, List.of("末次月经", "停经", "怀孕", "妊娠", "月经"))) {
                missing.add("妊娠情况或末次月经信息");
            }
        }
        return new ArrayList<>(missing);
    }

    private List<String> buildFollowUpQuestions(String normalizedPatientText,
                                                DoctorCopilotTools.ConsultationContext context,
                                                List<String> missingInfoItems,
                                                List<String> assessmentFocuses) {
        LinkedHashSet<String> questions = new LinkedHashSet<>();

        for (String item : missingInfoItems) {
            switch (item) {
                case "症状起始时间或持续时长" -> questions.add("这次不适是从什么时候开始的？是突然出现还是逐渐加重？");
                case "最不适的具体部位" -> questions.add("目前最难受的部位具体在哪里？有没有向周围放射？");
                case "症状严重程度或变化趋势" -> questions.add("症状现在比刚开始更重还是更轻？是否已经影响睡眠、活动或进食？");
                case "既往病史或基础疾病" -> questions.add("既往有没有高血压、糖尿病、冠心病、哮喘、手术或长期慢病史？");
                case "药物或食物过敏史" -> questions.add("有没有明确的药物过敏史，尤其是抗生素、止痛药或其他常用药？");
                case "就诊前是否已自行用药或处理" -> questions.add("本次发病前后有没有自己吃药、打针、输液或做过其他处理？效果怎么样？");
                case "体温或呼吸相关生命体征" -> questions.add("有没有量过体温、血氧或心率？最高体温大概多少？");
                case "妊娠情况或末次月经信息" -> questions.add("请补充最近一次月经时间，是否存在怀孕可能或已知妊娠情况？");
                default -> {
                }
            }
        }

        if (assessmentFocuses.stream().anyMatch(item -> item.contains("胸痛胸闷"))) {
            questions.add("胸痛/胸闷和活动、呼吸或体位变化有没有关系？是否伴有心悸、出汗或呼吸困难？");
        }
        if (assessmentFocuses.stream().anyMatch(item -> item.contains("呼吸道"))) {
            questions.add("是否伴有咳痰、咽痛、鼻塞或接触发热病人？痰液颜色和量怎么样？");
        }
        if (assessmentFocuses.stream().anyMatch(item -> item.contains("腹痛"))) {
            questions.add("腹痛主要在上腹、脐周还是右下腹？是否伴恶心、呕吐、腹泻或黑便？");
        }
        if (assessmentFocuses.stream().anyMatch(item -> item.contains("神经系统"))) {
            questions.add("头痛/头晕是突然发生还是逐渐加重？有没有视物模糊、肢体无力、言语异常或呕吐？");
        }

        if (questions.isEmpty()) {
            questions.add("请再补充最不舒服的部位、持续时间以及是否有伴随症状。");
        }
        return new ArrayList<>(questions);
    }

    private List<String> buildAssessmentFocuses(String normalizedPatientText, boolean highRisk) {
        LinkedHashSet<String> focuses = new LinkedHashSet<>();
        for (Map.Entry<String, List<String>> entry : FOCUS_KEYWORDS.entrySet()) {
            if (containsAny(normalizedPatientText, entry.getValue())) {
                switch (entry.getKey()) {
                    case "胸痛胸闷" -> focuses.add("胸痛胸闷：优先区分心源性、呼吸系统和肌骨性原因");
                    case "呼吸道症状" -> focuses.add("呼吸道症状：需要区分上呼吸道感染、支气管/肺部受累及低氧风险");
                    case "腹痛消化道症状" -> focuses.add("腹痛消化道症状：建议明确疼痛定位、伴随呕吐腹泻和出血情况");
                    case "神经系统症状" -> focuses.add("神经系统症状：需关注突发性、神经定位体征和意识状态");
                    case "妇产相关症状" -> focuses.add("妇产相关症状：需优先核对月经/妊娠情况及异常出血信息");
                    case "泌尿系统症状" -> focuses.add("泌尿系统症状：需明确尿频尿急尿痛、发热及腰痛情况");
                    default -> {
                    }
                }
            }
        }
        if (highRisk) {
            focuses.add("高风险信号：建议先完成急症排查，再决定是否继续线上问诊流程");
        }
        if (focuses.isEmpty()) {
            focuses.add("当前信息较少，建议先完成主诉、时长、部位和伴随症状的基础采集");
        }
        return new ArrayList<>(focuses);
    }

    private List<String> buildRecommendedActions(List<String> missingInfoItems,
                                                 List<String> riskAlerts,
                                                 DoctorCopilotTools.ConsultationContext context) {
        LinkedHashSet<String> actions = new LinkedHashSet<>();
        if (!riskAlerts.isEmpty()) {
            actions.add("优先根据风险提示补充急症相关病史，必要时建议线下急诊或进一步检查。");
        }
        if (!missingInfoItems.isEmpty()) {
            actions.add("先补齐缺失项，再提交病历或开具处方，避免病历信息不完整。");
        }
        if (!context.getHistoryRecords().isEmpty()) {
            actions.add("结合患者近期历史病历核对是否为复发、迁延或同类症状再次就诊。");
        }
        actions.add("在最终形成病历前，确认既往史、过敏史和当前用药情况。");
        return new ArrayList<>(actions);
    }

    private List<String> buildPrescriptionSafetyHints(String normalizedPatientText,
                                                      DoctorCopilotTools.ConsultationContext context,
                                                      boolean highRisk) {
        LinkedHashSet<String> hints = new LinkedHashSet<>();
        hints.add("开具处方前应再次确认药物过敏史和既往不良反应。");
        if (context.getPatientAge() != null && context.getPatientAge() < 14) {
            hints.add("儿童用药需结合年龄或体重评估剂量，不宜直接沿用成人剂量。");
        }
        if ("女".equals(context.getPatientGender()) && context.getPatientAge() != null
                && context.getPatientAge() >= 12 && context.getPatientAge() <= 55) {
            hints.add("女性育龄患者开药前应确认是否妊娠或存在妊娠可能。");
        }
        if (containsAny(normalizedPatientText, List.of("肾", "肝", "肝炎", "肾病"))) {
            hints.add("若存在肝肾功能异常相关病史，处方前应关注代谢和剂量调整。");
        }
        if (highRisk) {
            hints.add("尚未排除高风险情况前，不建议直接以普通对症用药替代进一步评估。");
        }
        return new ArrayList<>(hints);
    }

    private List<String> buildHistoryHighlights(List<DoctorCopilotTools.HistoryRecord> historyRecords) {
        List<String> result = new ArrayList<>();
        historyRecords.stream().limit(4).forEach(record -> {
            StringBuilder builder = new StringBuilder();
            builder.append(record.getCreateTime() == null ? "未知时间" : new java.text.SimpleDateFormat("yyyy-MM-dd").format(record.getCreateTime()))
                    .append("，")
                    .append(record.getDoctorName())
                    .append("：")
                    .append(StringUtils.hasText(record.getDoctorDescription()) ? record.getDoctorDescription() : "无摘要");
            if (!record.getDrugNames().isEmpty()) {
                builder.append("；相关药品：").append(String.join("、", record.getDrugNames()));
            }
            result.add(builder.toString());
        });
        return result;
    }

    private List<String> buildDataSources(DoctorCopilotTools.ConsultationContext context) {
        List<String> sources = new ArrayList<>();
        sources.add("当前挂号与患者基础信息");
        if (!context.getMessages().isEmpty()) {
            sources.add("当前房间聊天记录");
        }
        if (!context.getHistoryRecords().isEmpty()) {
            sources.add("患者历史病历");
        }
        sources.add("本地风险规则");
        return sources;
    }

    private String buildChiefComplaintDraft(String patientText) {
        if (!StringUtils.hasText(patientText)) {
            return "患者本次主诉信息尚不完整，建议继续补充。";
        }
        String duration = extractDuration(patientText);
        List<String> symptoms = extractTopSymptoms(patientText);
        if (!symptoms.isEmpty()) {
            String base = String.join("、", symptoms);
            return StringUtils.hasText(duration) ? base + duration : base;
        }
        String trimmed = patientText.trim();
        return trimmed.length() > 32 ? trimmed.substring(0, 32) + "..." : trimmed;
    }

    private String buildPresentIllnessDraft(String patientText,
                                            List<String> missingInfoItems,
                                            List<String> riskAlerts,
                                            DoctorCopilotTools.ConsultationContext context) {
        StringBuilder builder = new StringBuilder();
        if (StringUtils.hasText(patientText)) {
            builder.append("患者在本次线上问诊中主要描述：").append(trimForSentence(patientText, 120)).append("。");
        } else {
            builder.append("当前患者自述较少，尚需进一步补充本次发病经过。");
        }
        if (!context.getHistoryRecords().isEmpty()) {
            builder.append(" 既往系统内可见相关病历 ").append(Math.min(context.getHistoryRecords().size(), 4)).append(" 条，可结合历史就诊记录进一步核对复发或迁延情况。");
        }
        if (!missingInfoItems.isEmpty()) {
            builder.append(" 当前仍缺少：").append(String.join("、", limit(missingInfoItems, 4))).append("。");
        }
        if (!riskAlerts.isEmpty()) {
            builder.append(" 本次问诊需重点关注：").append(String.join("；", limit(riskAlerts, 2))).append("。");
        }
        return builder.toString();
    }

    private String buildStructuredRecordDraft(String chiefComplaintDraft,
                                              String presentIllnessDraft,
                                              List<String> missingInfoItems,
                                              List<String> assessmentFocuses,
                                              List<String> riskAlerts,
                                              List<String> prescriptionSafetyHints) {
        StringBuilder builder = new StringBuilder();
        builder.append("【主诉】\n").append(StringUtils.hasText(chiefComplaintDraft) ? chiefComplaintDraft : "待补充").append("\n\n");
        builder.append("【现病史】\n").append(StringUtils.hasText(presentIllnessDraft) ? presentIllnessDraft : "待补充").append("\n\n");
        builder.append("【建议继续补充】\n");
        if (missingInfoItems.isEmpty()) {
            builder.append("当前关键病史采集相对完整，可结合医生判断继续完善细节。\n\n");
        } else {
            limit(missingInfoItems, 5).forEach(item -> builder.append("- ").append(item).append("\n"));
            builder.append("\n");
        }
        builder.append("【重点排查方向】\n");
        limit(assessmentFocuses, 4).forEach(item -> builder.append("- ").append(item).append("\n"));
        builder.append("\n");
        if (!riskAlerts.isEmpty()) {
            builder.append("【风险提醒】\n");
            limit(riskAlerts, 4).forEach(item -> builder.append("- ").append(item).append("\n"));
            builder.append("\n");
        }
        builder.append("【处方前注意】\n");
        limit(prescriptionSafetyHints, 4).forEach(item -> builder.append("- ").append(item).append("\n"));
        return builder.toString().trim();
    }

    private String buildSuggestion(boolean needMoreInfo, boolean highRisk, List<String> assessmentFocuses) {
        if (highRisk) {
            return "当前问诊命中高风险信号，建议优先补充急症相关病史并排除危险情况，再决定后续线上处理。";
        }
        if (needMoreInfo) {
            return "当前信息尚不完整，建议先按缺失项继续追问，再形成最终病历和处方。";
        }
        if (!assessmentFocuses.isEmpty()) {
            return "当前问诊信息已具备基础整理条件，可结合重点排查方向完善病历并谨慎评估处方。";
        }
        return "当前问诊可继续整理病历草稿，但最终结论仍需医生确认。";
    }

    private int calculateConfidence(List<String> missingInfoItems,
                                    List<String> riskAlerts,
                                    List<String> historicalRecordHighlights,
                                    String chiefComplaintDraft,
                                    List<String> assessmentFocuses) {
        int confidence = 84;
        confidence -= Math.min(missingInfoItems.size() * 8, 32);
        confidence -= Math.min(riskAlerts.size() * 5, 20);
        if (!StringUtils.hasText(chiefComplaintDraft) || chiefComplaintDraft.contains("待补充")) {
            confidence -= 15;
        }
        if (!historicalRecordHighlights.isEmpty()) {
            confidence += 4;
        }
        if (!assessmentFocuses.isEmpty()) {
            confidence += 4;
        }
        return Math.max(35, Math.min(96, confidence));
    }

    private String extractDuration(String text) {
        java.util.regex.Matcher matcher = DURATION_PATTERN.matcher(normalize(text));
        if (matcher.find()) {
            return matcher.group();
        }
        return null;
    }

    private List<String> extractTopSymptoms(String text) {
        LinkedHashSet<String> symptoms = new LinkedHashSet<>();
        for (List<String> values : FOCUS_KEYWORDS.values()) {
            for (String value : values) {
                if (normalize(text).contains(normalize(value))) {
                    symptoms.add(value);
                }
                if (symptoms.size() >= 3) {
                    return new ArrayList<>(symptoms);
                }
            }
        }
        return new ArrayList<>(symptoms);
    }

    private String trimForSentence(String text, int maxChars) {
        if (!StringUtils.hasText(text)) {
            return "";
        }
        String normalized = text.replaceAll("\\s+", " ").trim();
        return normalized.length() <= maxChars ? normalized : normalized.substring(0, maxChars) + "...";
    }

    private boolean containsAny(String text, List<String> keywords) {
        for (String keyword : keywords) {
            if (text.contains(normalize(keyword))) {
                return true;
            }
        }
        return false;
    }

    private String normalize(String text) {
        return text == null ? "" : text.replace(" ", "").replace("　", "").toLowerCase(Locale.ROOT);
    }

    private <T> List<T> limit(List<T> list, int maxSize) {
        if (list == null || list.isEmpty()) {
            return List.of();
        }
        return list.size() <= maxSize ? list : list.subList(0, maxSize);
    }
}
