package cn.edu.ncu.medical.entity.dto;

import lombok.Data;

import java.util.List;

@Data
public class DoctorAiResponse {
    private String suggestion;
    private List<String> followUpQuestions;
    private String caution;
    private String source = "deepseek";
}
