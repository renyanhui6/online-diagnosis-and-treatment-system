package cn.edu.ncu.medical.entity;

import com.baomidou.mybatisplus.annotation.*;

import java.io.Serializable;
import java.util.Date;
import lombok.Data;

/**
 * @TableName medicine_order
 */
@TableName(value ="medicine_order")
@Data
public class MedicineOrder implements Serializable {
    @TableId(type = IdType.AUTO)
    private Long id;

    private Long drugId;

    private Integer drugQuantity;

    private Long orderPaymentRecordId;

    @TableField(fill = FieldFill.INSERT)
    private Date createTime;
    @TableLogic
    private Integer isDeleted;
}