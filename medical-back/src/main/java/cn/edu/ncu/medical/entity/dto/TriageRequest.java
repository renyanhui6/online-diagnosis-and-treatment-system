package cn.edu.ncu.medical.entity.dto;

import lombok.Data;

import java.util.List;

@Data
public class TriageRequest {
    private String description;
    private Integer age;
    private String gender;
    private List<String> symptoms;
}
