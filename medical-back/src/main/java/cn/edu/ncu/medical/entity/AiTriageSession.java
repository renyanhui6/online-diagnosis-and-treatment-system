package cn.edu.ncu.medical.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import java.util.Date;
import lombok.Data;

@Data
@TableName("ai_triage_session")
public class AiTriageSession {
    @TableId(type = IdType.AUTO)
    private Long id;

    private String sessionId;

    private Long userId;

    private Long patientAttendantId;

    private String status;

    private String source;

    private String recommendedDepartments;

    private String recommendedSubDepartments;

    private String summary;

    private Date createdTime;

    private Date updatedTime;

    private Date closedTime;

    @TableLogic
    private Integer isDeleted;
}
