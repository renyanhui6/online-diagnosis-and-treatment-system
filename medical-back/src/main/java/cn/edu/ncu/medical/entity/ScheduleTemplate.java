package cn.edu.ncu.medical.entity;

import com.baomidou.mybatisplus.annotation.*;

import java.util.Date;

import lombok.Data;

/**
 * @TableName schedule_template
 */
@TableName(value = "schedule_template")
@Data
public class ScheduleTemplate {
	@TableId(type = IdType.AUTO)
	private Long id;

	private Long doctorId;

	private Integer weekDay;

	private Integer morningLimit;

	private Integer afternoonLimit;

	private Integer isActive;
	@TableLogic
	private Integer isDeleted;
	@TableField(fill = FieldFill.INSERT)
	private Date createTime;
}