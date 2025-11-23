package cn.edu.ncu.medical.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.math.BigDecimal;
import java.util.Date;

/**
 * @TableName doctor_detail
 */
@TableName(value ="doctor_detail")
@Data
public class DoctorDetail {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long systemUserId;
    private String realName;
    private String idCard;
    private String introduction;
    private BigDecimal price;
    @TableField(fill = FieldFill.INSERT)
    private Date createTime;
    @TableField(fill = FieldFill.UPDATE)
    private Date updateTime;

    private String title;

    private Long subDepartmentId;

    private String professionalLicenseNumber;

    @TableLogic
    private Integer isDeleted;
}