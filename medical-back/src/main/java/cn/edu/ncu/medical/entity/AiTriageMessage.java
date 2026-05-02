package cn.edu.ncu.medical.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import java.util.Date;
import lombok.Data;

@Data
@TableName("ai_triage_message")
public class AiTriageMessage {
    @TableId(type = IdType.AUTO)
    private Long id;

    private String sessionId;

    private Long userId;

    private String role;

    private String content;

    private String source;

    private String recommendedDepartments;

    private String recommendedSubDepartments;

    private Integer needMoreInfo;

    private Integer emergency;

    private Integer confidence;

    private Date createTime;

    @TableLogic
    private Integer isDeleted;
}
