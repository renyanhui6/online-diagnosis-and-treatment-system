package cn.edu.ncu.medical.entity.vo;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.math.BigDecimal;
import java.util.Date;
@Data
public class ScheduleVo {
	@TableId(type = IdType.AUTO)
	private Long id;

	private Long templateId;
	private Long subDepartmentId;

	private String departmentName;

	private Long doctorId;

	private String doctorName;
	@JsonFormat(pattern = "yyyy-MM-dd")
	private Date scheduleDate;
	private BigDecimal price;
	private Integer isMorning;

	private Integer isAfternoon;

	private Integer status;

	private Integer currentAppointmentCount;

	private Integer appointmentLimit;

	/**
	 * 是否可预约（与后端创建挂号订单的时间规则保持一致）。
	 */
	private Boolean canBook;
}
