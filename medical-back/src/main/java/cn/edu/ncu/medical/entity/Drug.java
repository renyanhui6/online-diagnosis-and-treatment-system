package cn.edu.ncu.medical.entity;

import com.baomidou.mybatisplus.annotation.*;

import java.math.BigDecimal;
import java.util.Date;
import lombok.Data;

/**
 * @TableName drug
 */
@TableName(value ="drug")
@Data
public class Drug {
    @TableId(type = IdType.AUTO)
    private Long id;

    private String genericName;

//    private String tradeName;

//    private String dosageForm;

    private String specification;

//    private String approvalNumber;
//
//    private String manufacturer;
//
//    private String drugCategory;

//    private Integer prescriptionType;

    private String minimumSalesUnit;

//    private String packagingSpecification;

    private BigDecimal drugPrice;

//    private Integer status;

    private Integer quantity;

    private Integer isPrescription;
    @TableField(fill = FieldFill.INSERT)
    private Date createTime;
    @TableLogic
    private Integer isDeleted;
}