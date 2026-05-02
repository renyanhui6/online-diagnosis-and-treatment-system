package cn.edu.ncu.medical.entity.vo;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import lombok.Data;

@Data
public class AiTriageSessionVo {
    private String sessionId;
    private Long userId;
    private Long patientAttendantId;
    private String status;
    private String source;
    private List<String> recommendedDepartments = new ArrayList<>();
    private List<String> recommendedSubDepartments = new ArrayList<>();
    private String summary;
    private Date createdTime;
    private Date updatedTime;
    private Date closedTime;
}
