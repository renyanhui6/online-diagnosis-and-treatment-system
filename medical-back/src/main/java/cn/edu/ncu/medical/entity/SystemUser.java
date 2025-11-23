package cn.edu.ncu.medical.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.util.Date;

/**
 * @TableName system_user
 */
@TableName(value ="system_user")
@Data
public class SystemUser {
    @TableId(type = IdType.AUTO)
    private Long id;

    private String username;

    private String password;

    private Integer type;
    @TableField(value = "create_time",fill = FieldFill.INSERT)
    private Date createTime;

    private String email;
    @TableField(value = "update_time",fill = FieldFill.UPDATE)
    private Date updateTime;

    private Integer registerType;

    private Integer status;
    @TableLogic
    private Integer isDeleted;
}