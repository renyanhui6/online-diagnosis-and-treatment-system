package cn.edu.ncu.medical.entity.dto;

import lombok.Data;

import java.util.List;

@Data
public class TriageResponse {
    private List<String> recommendedDepartments;
    private String rationale;
    private String disclaimer = "AI 推荐仅供参考，请结合实际病情选择科室";
}
