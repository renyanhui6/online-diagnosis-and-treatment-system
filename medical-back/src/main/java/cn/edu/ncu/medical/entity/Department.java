package cn.edu.ncu.medical.entity;

import com.baomidou.mybatisplus.annotation.*;

import java.util.Date;
import lombok.Data;

/**
 * @TableName department
 */
@TableName(value ="department")
@Data
public class Department {
    @TableId(type = IdType.AUTO)
    private Long id;

    private String departmentName;

    private String description;
    @TableField(fill = FieldFill.INSERT)
    private Date createTime;
    @TableLogic
    private Integer isDeleted;
}