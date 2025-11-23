package cn.edu.ncu.medical.entity;

import com.baomidou.mybatisplus.annotation.*;

import java.util.Date;
import lombok.Data;

/**
 * @TableName sub_department
 */
@TableName(value ="sub_department")
@Data
public class SubDepartment {
    @TableId(type = IdType.AUTO)
    private Long id;

    private Long parentDepartmentId;

    private String departmentName;

    private String description;

    private String treatmentScope;

    private String departmentFeatures;

    private String imagePath;
    @TableField(fill = FieldFill.INSERT)
    private Date createTime;
    @TableLogic
    private Integer isDeleted;
}