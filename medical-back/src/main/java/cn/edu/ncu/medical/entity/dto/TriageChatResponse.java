package cn.edu.ncu.medical.entity.dto;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
public class TriageChatResponse {
    private String sessionId;
    private String source;
    private String assistantMessage;
    private boolean needMoreInfo;
    private boolean emergency;
    private boolean usedExternalKnowledge;
    private Integer confidence;
    private String rationale;
    private String disclaimer = "AI 推荐仅供参考，如出现急危重症状请立即线下就医。";
    private List<String> followUpQuestions = new ArrayList<>();
    private List<String> recommendedDepartments = new ArrayList<>();
    private List<String> recommendedSubDepartments = new ArrayList<>();
}
