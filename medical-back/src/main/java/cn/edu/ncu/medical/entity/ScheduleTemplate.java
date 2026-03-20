package cn.edu.ncu.medical.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
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

	/**
	 * 1-7(周一到周日)
	 */
	private Integer weekDay;

	/**
	 * 上午预约上限
	 */
	private Integer morningLimit;

	/**
	 * 下午预约上限
	 */
	private Integer afternoonLimit;

	/**
	 * 是否生效
	 */
	private Integer isActive;

	@TableField(fill = FieldFill.INSERT)
	private Date createTime;

	@TableLogic
	private Integer isDeleted;
}
