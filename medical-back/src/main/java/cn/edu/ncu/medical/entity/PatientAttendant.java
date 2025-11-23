package cn.edu.ncu.medical.entity;

import com.baomidou.mybatisplus.annotation.*;

import java.util.Date;
import lombok.Data;

/**
 * @TableName patient_attendant
 */
@TableName(value ="patient_attendant")
@Data
public class PatientAttendant {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long systemUserId;
    private String nickname;
    private String realName;

    private String idCard;

    private Integer gender;
    private String phone;
    private String homeAddress;

    private Integer isMaster;
    @TableField(fill = FieldFill.INSERT)
    private Date createTime;
    @TableField(fill = FieldFill.UPDATE)
    private Date updateTime;

    @TableLogic
    private Integer isDeleted;
}