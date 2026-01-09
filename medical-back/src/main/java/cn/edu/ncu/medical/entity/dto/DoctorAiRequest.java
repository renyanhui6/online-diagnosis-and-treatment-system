package cn.edu.ncu.medical.entity.dto;

import lombok.Data;

import java.util.List;

@Data
public class DoctorAiRequest {
    private Long roomId;
    private Long registrationId;
    private String summary;
    private String conversationSnippet;
    private List<String> symptoms;
}
