package cn.edu.ncu.medical.entity;

import com.baomidou.mybatisplus.annotation.*;

import java.io.Serializable;
import java.util.Date;
import lombok.Data;

/**
 * @TableName registration
 */
@TableName(value ="registration")
@Data
public class Registration implements Serializable {
    @TableId(type = IdType.AUTO)
    private Long id;

    private Long doctorId;

    private Long patientId;

    @TableField(fill = FieldFill.INSERT)
    private Date createTime;

    private Long scheduleId;

    private Integer registrationStatus;

    @TableField("person_key")
    private String personKey;

    @TableField("request_token")
    private String requestToken;
    @TableField(fill = FieldFill.UPDATE)
    private Date updateTime;
    @TableLogic
    private Integer isDeleted;
}
