package cn.edu.ncu.medical.ai;

import cn.edu.ncu.medical.entity.ChatMessage;
import cn.edu.ncu.medical.entity.Department;
import cn.edu.ncu.medical.entity.DoctorDetail;
import cn.edu.ncu.medical.entity.Drug;
import cn.edu.ncu.medical.entity.MedicalRecord;
import cn.edu.ncu.medical.entity.Prescription;
import cn.edu.ncu.medical.entity.Registration;
import cn.edu.ncu.medical.entity.Room;
import cn.edu.ncu.medical.entity.SubDepartment;
import cn.edu.ncu.medical.entity.vo.RegistrationInfo;
import cn.edu.ncu.medical.exception.MyRuntimeException;
import cn.edu.ncu.medical.inteceptor.login.LoginUserHolder;
import cn.edu.ncu.medical.result.ResultCodeEnum;
import cn.edu.ncu.medical.service.ChatMessageService;
import cn.edu.ncu.medical.service.DepartmentService;
import cn.edu.ncu.medical.service.DoctorDetailService;
import cn.edu.ncu.medical.service.DrugService;
import cn.edu.ncu.medical.service.MedicalRecordService;
import cn.edu.ncu.medical.service.PrescriptionService;
import cn.edu.ncu.medical.service.RegistrationService;
import cn.edu.ncu.medical.service.RoomService;
import cn.edu.ncu.medical.service.SubDepartmentService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import dev.langchain4j.agent.tool.Tool;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 医生端协作 Agent 的工具集合。
 *
 * 设计目的有两个：
 * 1. 给在线 Agent 提供可调用的院内工具，而不是让模型只靠聊天记录“猜”；
 * 2. 给本地回退引擎复用同一套真实数据读取逻辑，保证线上/离线输出口径一致。
 */
@Component
@RequiredArgsConstructor
public class DoctorCopilotTools {

    private static final int DEFAULT_HISTORY_LIMIT = 5;
    private static final int DEFAULT_DRUG_LIMIT = 6;
    private static final SimpleDateFormat DATE_TIME_FORMAT = new SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.CHINA);

    private static final List<RiskRule> RISK_RULES = List.of(
            new RiskRule(List.of("胸痛", "胸闷", "呼吸困难", "喘不上气"), "出现胸痛/胸闷合并呼吸困难，需优先排查心肺急症。"),
            new RiskRule(List.of("大出血", "便血", "黑便", "呕血", "咯血"), "存在活动性出血相关描述，需优先评估失血和急诊处理必要性。"),
            new RiskRule(List.of("意识不清", "昏迷", "抽搐", "偏瘫", "口角歪斜", "失语"), "存在神经系统高风险信号，建议优先排查卒中、癫痫或中枢事件。"),
            new RiskRule(List.of("高热", "39", "40", "持续发热"), "存在高热或持续发热描述，需尽快补充体温、寒战、意识状态和感染风险。"),
            new RiskRule(List.of("怀孕", "妊娠", "见红", "孕妇出血", "胎动减少"), "出现妊娠相关异常描述，需优先排查产科急症。")
    );

    private final RoomService roomService;
    private final RegistrationService registrationService;
    private final ChatMessageService chatMessageService;
    private final MedicalRecordService medicalRecordService;
    private final PrescriptionService prescriptionService;
    private final DrugService drugService;
    private final DoctorDetailService doctorDetailService;
    private final SubDepartmentService subDepartmentService;
    private final DepartmentService departmentService;

    /**
     * 内部复用：加载当前问诊房间、挂号、患者、聊天和历史病历的完整上下文。
     */
    public ConsultationContext loadConsultationContext(Long roomId, Long registrationId) {
        Long doctorUserId = resolveDoctorUserId();

        Room room = resolveRoom(roomId, registrationId);
        Long resolvedRegistrationId = registrationId != null
                ? registrationId
                : (room != null ? room.getRegistrationId() : null);
        if (resolvedRegistrationId == null) {
            throw new MyRuntimeException(ResultCodeEnum.PARAM_ERROR.getCode(), "缺少有效的挂号ID");
        }

        Registration registration = registrationService.getById(resolvedRegistrationId);
        if (registration == null || (registration.getIsDeleted() != null && registration.getIsDeleted() == 1)) {
            throw new MyRuntimeException(404, "挂号记录不存在");
        }

        DoctorDetail doctorDetail = doctorDetailService.getById(registration.getDoctorId());
        if (doctorDetail == null || !Objects.equals(doctorDetail.getSystemUserId(), doctorUserId)) {
            throw new MyRuntimeException(403, "当前医生无权访问该问诊上下文");
        }

        if (room != null && room.getDoctorId() != null && !Objects.equals(room.getDoctorId(), doctorUserId)) {
            throw new MyRuntimeException(403, "当前医生无权访问该问诊房间");
        }

        RegistrationInfo registrationInfo = registrationService.getRegistrationById(resolvedRegistrationId);
        if (registrationInfo == null) {
            throw new MyRuntimeException(404, "挂号详情不存在");
        }

        SubDepartment subDepartment = doctorDetail.getSubDepartmentId() == null
                ? null
                : subDepartmentService.getById(doctorDetail.getSubDepartmentId());
        Department department = subDepartment == null
                ? null
                : departmentService.getById(subDepartment.getParentDepartmentId());

        List<MessageLine> messages = room == null
                ? List.of()
                : chatMessageService.getMessagesByRoomId(String.valueOf(room.getId())).stream()
                .sorted(Comparator.comparing(ChatMessage::getCreateTime, Comparator.nullsLast(Date::compareTo)))
                .map(this::toMessageLine)
                .toList();

        List<HistoryRecord> historyRecords = loadPatientHistory(registration.getPatientId(), DEFAULT_HISTORY_LIMIT);

        ConsultationContext context = new ConsultationContext();
        context.setRoomId(room == null ? null : room.getId());
        context.setRoomStatus(room == null ? null : room.getRoomStatus());
        context.setRegistrationId(resolvedRegistrationId);
        context.setRegistrationStatus(registration.getRegistrationStatus());
        context.setPatientId(registration.getPatientId());
        context.setPatientName(registrationInfo.getPatientName());
        context.setPatientPhone(registrationInfo.getPatientPhone());
        context.setPatientGender(registrationInfo.getPatientGender());
        context.setPatientAge(registrationInfo.getPatientAge());
        context.setDoctorDetailId(doctorDetail.getId());
        context.setDoctorUserId(doctorUserId);
        context.setDoctorName(StringUtils.hasText(doctorDetail.getRealName()) ? doctorDetail.getRealName() : registrationInfo.getDoctorName());
        context.setDoctorTitle(doctorDetail.getTitle());
        context.setDepartmentName(department == null ? null : department.getDepartmentName());
        context.setSubDepartmentName(subDepartment == null ? null : subDepartment.getDepartmentName());
        context.setMessages(messages);
        context.setHistoryRecords(historyRecords);
        return context;
    }

    /**
     * 内部复用：查询患者近期历史病历，用于发现重复主诉、既往用药或既往诊疗方向。
     */
    public List<HistoryRecord> loadPatientHistory(Long patientId, int limit) {
        if (patientId == null) {
            return List.of();
        }
        int safeLimit = Math.max(1, Math.min(limit, 10));
        List<MedicalRecord> records = medicalRecordService.list(new LambdaQueryWrapper<MedicalRecord>()
                .eq(MedicalRecord::getPatientId, patientId)
                .orderByDesc(MedicalRecord::getCreateTime)
                .last("limit " + safeLimit));
        if (records.isEmpty()) {
            return List.of();
        }

        Map<Long, String> doctorNameMap = doctorDetailService.listByIds(records.stream()
                        .map(MedicalRecord::getDoctorId)
                        .filter(Objects::nonNull)
                        .collect(Collectors.toSet()))
                .stream()
                .collect(Collectors.toMap(DoctorDetail::getId, DoctorDetail::getRealName, (a, b) -> a));

        List<Long> medicalRecordIds = records.stream().map(MedicalRecord::getId).toList();
        List<Prescription> prescriptions = prescriptionService.list(new LambdaQueryWrapper<Prescription>()
                .in(Prescription::getMedicalRecordId, medicalRecordIds));
        Map<Long, List<Prescription>> prescriptionMap = prescriptions.stream()
                .collect(Collectors.groupingBy(Prescription::getMedicalRecordId));
        Map<Long, String> drugNameMap = drugService.listByIds(prescriptions.stream()
                        .map(Prescription::getDrugId)
                        .filter(Objects::nonNull)
                        .collect(Collectors.toSet()))
                .stream()
                .collect(Collectors.toMap(Drug::getId, Drug::getGenericName, (a, b) -> a));

        List<HistoryRecord> result = new ArrayList<>();
        for (MedicalRecord record : records) {
            List<String> drugNames = prescriptionMap.getOrDefault(record.getId(), List.of()).stream()
                    .map(Prescription::getDrugId)
                    .map(drugNameMap::get)
                    .filter(StringUtils::hasText)
                    .distinct()
                    .limit(3)
                    .toList();

            HistoryRecord historyRecord = new HistoryRecord();
            historyRecord.setMedicalRecordId(record.getId());
            historyRecord.setCreateTime(record.getCreateTime());
            historyRecord.setDoctorName(doctorNameMap.getOrDefault(record.getDoctorId(), "未知医生"));
            historyRecord.setDoctorDescription(record.getDoctorDescription());
            historyRecord.setHasPrescription(record.getIsPurchasable() != null && record.getIsPurchasable() != 2);
            historyRecord.setDrugNames(drugNames);
            result.add(historyRecord);
        }
        return result;
    }

    /**
     * 内部复用：根据症状摘要做硬规则风险扫描。
     *
     * 这部分和大模型无关，属于“必须命中的安全底线”。
     */
    public List<String> detectClinicalRiskSignals(String text, Integer age, String gender) {
        if (!StringUtils.hasText(text)) {
            return List.of();
        }
        String normalized = normalize(text);
        LinkedHashSet<String> matched = new LinkedHashSet<>();
        for (RiskRule rule : RISK_RULES) {
            if (rule.matches(normalized)) {
                matched.add(rule.message());
            }
        }
        if (age != null && age <= 3 && containsAny(normalized, List.of("高热", "抽搐", "精神差"))) {
            matched.add("婴幼儿存在高热/抽搐/精神差描述，建议优先排查儿科急症。");
        }
        if ("女".equals(gender) && containsAny(normalized, List.of("腹痛", "停经", "阴道出血"))) {
            matched.add("女性腹痛合并停经或阴道出血时，需警惕妇产科高风险情况。");
        }
        return new ArrayList<>(matched);
    }

    /**
     * 内部复用：从药品库里按关键字检索可用药品，供在线 Agent 生成“处方前注意点”时引用。
     */
    public List<DrugHint> searchDrugHints(String keyword, int limit) {
        if (!StringUtils.hasText(keyword)) {
            return List.of();
        }
        int safeLimit = Math.max(1, Math.min(limit, 10));
        List<Drug> drugs = drugService.list(new LambdaQueryWrapper<Drug>()
                .like(Drug::getGenericName, keyword)
                .gt(Drug::getQuantity, 0)
                .orderByAsc(Drug::getGenericName)
                .last("limit " + safeLimit));
        return drugs.stream().map(drug -> {
            DrugHint hint = new DrugHint();
            hint.setDrugId(drug.getId());
            hint.setDrugName(drug.getGenericName());
            hint.setSpecification(drug.getSpecification());
            hint.setUnit(drug.getMinimumSalesUnit());
            hint.setPrice(drug.getDrugPrice());
            hint.setPrescriptionDrug(drug.getIsPrescription() != null && drug.getIsPrescription() == 1);
            return hint;
        }).toList();
    }

    @Tool("""
            读取当前医生名下的问诊上下文，包括挂号信息、患者基础资料、房间状态和最近聊天记录。
            当需要生成问诊协作建议时，必须优先使用这个工具。
            """)
    public String queryConsultationContext(Long roomId, Long registrationId) {
        ConsultationContext context = loadConsultationContext(roomId, registrationId);
        StringBuilder builder = new StringBuilder();
        builder.append("当前问诊上下文：\n")
                .append("- 挂号ID：").append(context.getRegistrationId()).append("\n")
                .append("- 房间ID：").append(defaultText(context.getRoomId())).append("\n")
                .append("- 患者：").append(defaultText(context.getPatientName())).append("，")
                .append(defaultText(context.getPatientGender())).append("，")
                .append(defaultText(context.getPatientAge())).append("岁，电话：").append(defaultText(context.getPatientPhone())).append("\n")
                .append("- 医生：").append(defaultText(context.getDoctorName())).append("，")
                .append(defaultText(context.getDoctorTitle())).append("，科室：")
                .append(defaultText(context.getDepartmentName())).append("/")
                .append(defaultText(context.getSubDepartmentName())).append("\n")
                .append("- 挂号状态：").append(defaultText(context.getRegistrationStatus())).append("，房间状态：").append(defaultText(context.getRoomStatus())).append("\n")
                .append("- 最近聊天记录：\n");

        List<MessageLine> messages = context.getMessages();
        if (messages.isEmpty()) {
            builder.append("  无聊天记录。\n");
        } else {
            messages.stream()
                    .limit(20)
                    .forEach(message -> builder.append("  [")
                            .append(defaultText(message.getTimeText()))
                            .append("] ")
                            .append(defaultText(message.getSpeaker()))
                            .append("：")
                            .append(defaultText(message.getContent()))
                            .append("\n"));
        }
        return builder.toString();
    }

    @Tool("""
            查询当前患者最近的历史病历、既往处方和历史就诊摘要。
            当需要判断既往病史、重复主诉或参考历史用药时使用。
            """)
    public String queryPatientHistory(Long roomId, Long registrationId) {
        ConsultationContext context = loadConsultationContext(roomId, registrationId);
        List<HistoryRecord> historyRecords = context.getHistoryRecords();
        if (historyRecords.isEmpty()) {
            return "患者暂无历史病历记录。";
        }
        StringBuilder builder = new StringBuilder("患者近期历史病历：\n");
        historyRecords.forEach(record -> builder.append("- ")
                .append(formatTime(record.getCreateTime()))
                .append("，医生：").append(defaultText(record.getDoctorName()))
                .append("，摘要：").append(defaultText(record.getDoctorDescription()))
                .append("，开具处方：").append(record.isHasPrescription() ? "是" : "否")
                .append(record.getDrugNames().isEmpty() ? "" : "，相关药品：" + String.join("、", record.getDrugNames()))
                .append("\n"));
        return builder.toString();
    }

    @Tool("""
            根据问诊摘要做临床风险规则扫描。
            当出现胸痛、呼吸困难、高热、意识障碍、出血、妊娠异常等风险时，应调用这个工具。
            """)
    public String queryClinicalRiskSignals(String summary, Integer age, String gender) {
        List<String> risks = detectClinicalRiskSignals(summary, age, gender);
        if (risks.isEmpty()) {
            return "未命中本地高风险规则。";
        }
        return "命中的高风险规则：\n- " + String.join("\n- ", risks);
    }

    @Tool("""
            从院内药品库中检索药品信息，用于处方前安全核对或提示医生注意药物确认事项。
            仅用于辅助提醒，不直接替医生决定处方。
            """)
    public String queryDrugCatalog(String keyword) {
        List<DrugHint> drugs = searchDrugHints(keyword, DEFAULT_DRUG_LIMIT);
        if (drugs.isEmpty()) {
            return "药品库中未找到匹配药品。";
        }
        StringBuilder builder = new StringBuilder("药品库检索结果：\n");
        drugs.forEach(drug -> builder.append("- ")
                .append(defaultText(drug.getDrugName()))
                .append("，规格：").append(defaultText(drug.getSpecification()))
                .append("，单位：").append(defaultText(drug.getUnit()))
                .append("，价格：").append(drug.getPrice() == null ? "未录入" : drug.getPrice().toPlainString())
                .append("，类型：").append(drug.isPrescriptionDrug() ? "处方药" : "非处方药")
                .append("\n"));
        return builder.toString();
    }

    private Room resolveRoom(Long roomId, Long registrationId) {
        if (roomId != null) {
            Room room = roomService.getById(roomId);
            if (room == null) {
                throw new MyRuntimeException(404, "问诊房间不存在");
            }
            if (registrationId != null && !Objects.equals(room.getRegistrationId(), registrationId)) {
                throw new MyRuntimeException(ResultCodeEnum.PARAM_ERROR.getCode(), "房间ID与挂号ID不匹配");
            }
            return room;
        }
        if (registrationId != null) {
            return roomService.getRoomByRegistrationId(registrationId);
        }
        return null;
    }

    private Long resolveDoctorUserId() {
        if (LoginUserHolder.getLoginUser() == null || LoginUserHolder.getLoginUser().getUserId() == null) {
            throw new MyRuntimeException(ResultCodeEnum.USER_NOT_LOGIN);
        }
        return LoginUserHolder.getLoginUser().getUserId();
    }

    private MessageLine toMessageLine(ChatMessage message) {
        MessageLine line = new MessageLine();
        line.setSpeaker(message.getSenderType() != null && message.getSenderType() == 2 ? "医生" : "患者");
        line.setContent(message.getMessageType() != null && message.getMessageType() == 2
                ? "[图片] " + defaultText(message.getContent())
                : defaultText(message.getContent()));
        line.setTimeText(formatTime(message.getCreateTime()));
        return line;
    }

    private String formatTime(Date time) {
        return time == null ? "未知时间" : DATE_TIME_FORMAT.format(time);
    }

    private String defaultText(Object value) {
        if (value == null) {
            return "未填写";
        }
        String text = String.valueOf(value).trim();
        return StringUtils.hasText(text) ? text : "未填写";
    }

    private String normalize(String text) {
        return text == null ? "" : text.replace(" ", "").replace("　", "").toLowerCase(Locale.ROOT);
    }

    private boolean containsAny(String normalizedText, Collection<String> keywords) {
        for (String keyword : keywords) {
            if (normalizedText.contains(normalize(keyword))) {
                return true;
            }
        }
        return false;
    }

    private record RiskRule(List<String> keywords, String message) {
        boolean matches(String normalizedText) {
            for (String keyword : keywords) {
                if (normalizedText.contains(keyword.toLowerCase(Locale.ROOT).replace(" ", ""))) {
                    return true;
                }
            }
            return false;
        }
    }

    @Data
    public static class ConsultationContext {
        private Long roomId;
        private Integer roomStatus;
        private Long registrationId;
        private Integer registrationStatus;
        private Long patientId;
        private String patientName;
        private String patientPhone;
        private String patientGender;
        private Integer patientAge;
        private Long doctorDetailId;
        private Long doctorUserId;
        private String doctorName;
        private String doctorTitle;
        private String departmentName;
        private String subDepartmentName;
        private List<MessageLine> messages = List.of();
        private List<HistoryRecord> historyRecords = List.of();

        public String patientTranscriptText() {
            return messages.stream()
                    .filter(message -> "患者".equals(message.getSpeaker()))
                    .map(MessageLine::getContent)
                    .filter(this::isMeaningfulPatientContent)
                    .filter(StringUtils::hasText)
                    .collect(Collectors.joining(" "));
        }

        public String fullTranscriptText() {
            return messages.stream()
                    .filter(message -> !"系统".equals(message.getSpeaker()))
                    .map(message -> defaultStatic(message.getSpeaker()) + "：" + defaultStatic(message.getContent()))
                    .collect(Collectors.joining(" "));
        }

        private boolean isMeaningfulPatientContent(String content) {
            if (!StringUtils.hasText(content)) {
                return false;
            }
            String trimmed = content.trim();
            if (trimmed.contains("已同意开始问诊") || trimmed.startsWith("联调测试消息")) {
                return false;
            }
            return !trimmed.matches("^[A-Za-z0-9]{1,6}$");
        }

        private String defaultStatic(String value) {
            return StringUtils.hasText(value) ? value : "";
        }
    }

    @Data
    public static class MessageLine {
        private String speaker;
        private String content;
        private String timeText;
    }

    @Data
    public static class HistoryRecord {
        private Long medicalRecordId;
        private Date createTime;
        private String doctorName;
        private String doctorDescription;
        private boolean hasPrescription;
        private List<String> drugNames = List.of();
    }

    @Data
    public static class DrugHint {
        private Long drugId;
        private String drugName;
        private String specification;
        private String unit;
        private BigDecimal price;
        private boolean prescriptionDrug;
    }
}
