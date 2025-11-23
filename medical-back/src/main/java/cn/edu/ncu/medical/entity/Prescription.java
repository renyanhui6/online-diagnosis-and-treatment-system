package cn.edu.ncu.medical.entity;

import com.baomidou.mybatisplus.annotation.*;

import java.io.Serializable;
import java.util.Date;
import lombok.Data;

/**
 * @TableName prescription
 */
@TableName(value ="prescription")
@Data
public class Prescription implements Serializable {
    @TableId(type = IdType.AUTO)
    private Long id;

    private Long drugId;

    private Integer drugQuantity;

    private Long medicalRecordId;
    @TableField(fill = FieldFill.INSERT)
    private Date createTime;
    @TableLogic
    private Integer isDeleted;
}