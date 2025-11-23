package cn.edu.ncu.medical.entity;

import com.baomidou.mybatisplus.annotation.*;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.Date;
import lombok.Data;

/**
 * @TableName appointment_payment_record
 */
@TableName(value ="appointment_payment_record")
@Data
public class AppointmentPaymentRecord implements Serializable {
    @TableId(type = IdType.AUTO)
    private Long id;

    private Long payerId;

    private Integer paymentStatus;

    private BigDecimal paymentAmount;

    private Date paymentTime;

    private String paymentMethod;
    @TableField(fill = FieldFill.INSERT)
    private Date createTime;

    private Long registrationId;

    private String paymentGateway;
    @TableLogic
    private Integer isDeleted;
}