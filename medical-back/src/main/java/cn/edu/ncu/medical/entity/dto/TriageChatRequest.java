package cn.edu.ncu.medical.entity.dto;

import lombok.Data;

@Data
public class TriageChatRequest {
    private String sessionId;
    private String message;
    private Integer age;
    private String gender;
}
