package cn.edu.ncu.medical.entity;

import com.baomidou.mybatisplus.annotation.*;

import java.util.Date;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

/**
 * @TableName schedule
 */
@TableName(value ="schedule")
@Data
public class Schedule {
    @TableId(type = IdType.AUTO)
    private Long id;

    private Long templateId;
    private Long subDepartmentId;

    private String departmentName;

    private Long doctorId;

    private String doctorName;
    @JsonFormat(pattern = "yyyy-MM-dd", timezone = "Asia/Shanghai")
    private Date scheduleDate;

    private Integer isMorning;

    private Integer isAfternoon;

    private Integer status;

    private Integer currentAppointmentCount;

    private Integer appointmentLimit;
    @TableField(fill = FieldFill.INSERT)
    private Date createTime;
    @TableLogic
    private Integer isDeleted;
}
