package cn.edu.ncu.medical.entity.dto;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
public class DoctorAiResponse {
    private String suggestion;
    private List<String> followUpQuestions = new ArrayList<>();
    private List<String> missingInfoItems = new ArrayList<>();
    private List<String> riskAlerts = new ArrayList<>();
    private List<String> recommendedActions = new ArrayList<>();
    private List<String> assessmentFocuses = new ArrayList<>();
    private List<String> prescriptionSafetyHints = new ArrayList<>();
    private List<String> historicalRecordHighlights = new ArrayList<>();
    private List<String> usedDataSources = new ArrayList<>();
    private String chiefComplaintDraft;
    private String presentIllnessDraft;
    private String structuredRecordDraft;
    private Integer confidence;
    private boolean needMoreInfo;
    private boolean highRisk;
    private String caution = "AI 生成内容仅供医生参考，最终病历、诊断与处方必须由医生确认。";
    private String source = "local-doctor-copilot";
}
