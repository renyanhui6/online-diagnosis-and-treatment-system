package cn.edu.ncu.medical.entity;

import com.baomidou.mybatisplus.annotation.*;

import java.util.Date;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;

/**
 * 聊天室实体类
 */


import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.util.Date;

/**
 * 聊天房间实体类
 */
@Data
@TableName("room")
public class Room {

    /**
     * 房间ID（主键，自增）
     */
    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 关联的预约ID
     */
    private Long registrationId;

    /**
     * 医生ID
     */
    private Long doctorId;

    /**
     * 患者ID
     */
    private Long patientId;

    /**
     * 患者姓名
     */
    private String patientName;

    /**
     * 房间状态（1-等待患者确认，2-问诊中，3-已结束，4-超时，5-患者拒绝）
     */
    private Integer roomStatus;

    /**
     * 创建时间
     */
    @TableField(fill = FieldFill.INSERT)
    private Date createTime;

    /**
     * 更新时间
     */
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private Date updateTime;

    /**
     * 软删除标记（0-未删除，1-已删除）
     */
    @TableLogic
    private Integer isDeleted;
}