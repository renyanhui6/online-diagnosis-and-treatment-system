package cn.edu.ncu.medical.entity;

import com.baomidou.mybatisplus.annotation.*;

import java.util.Date;
import lombok.Data;

/**
 * @TableName medical_record
 */
@TableName(value ="medical_record")
@Data
public class MedicalRecord {
    @TableId(type = IdType.AUTO)
    private Long id;

    private Long patientId;

    private Long doctorId;

    private String doctorDescription;

    private Integer isPurchasable;
    @TableField(fill = FieldFill.INSERT)
    private Date createTime;
    @TableLogic
    private Integer isDeleted;
}