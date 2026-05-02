package cn.edu.ncu.medical.entity.vo;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import lombok.Data;

@Data
public class AiTriageMessageVo {
    private String sessionId;
    private String role;
    private String content;
    private String source;
    private List<String> recommendedDepartments = new ArrayList<>();
    private List<String> recommendedSubDepartments = new ArrayList<>();
    private Integer needMoreInfo;
    private Integer emergency;
    private Integer confidence;
    private Date createTime;
}
