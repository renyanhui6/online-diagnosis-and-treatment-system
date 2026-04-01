package cn.edu.ncu.medical.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.Date;

@TableName("registration_payment_order")
@Data
public class RegistrationPaymentOrder implements Serializable {
    @TableId(type = IdType.AUTO)
    private Long id;

    private Long registrationId;

    private Long payerUserId;

    private String outTradeNo;

    private BigDecimal paymentAmount;

    private Integer paymentStatus;

    private String subject;

    private String paymentMethod;

    private String paymentGateway;

    private String gatewayTradeNo;

    private String buyerLogonId;

    private String statusRemark;

    private Date expireTime;

    private Date paymentTime;

    @TableField(fill = FieldFill.INSERT)
    private Date createTime;

    @TableField(fill = FieldFill.UPDATE)
    private Date updateTime;

    @TableLogic
    private Integer isDeleted;
}
