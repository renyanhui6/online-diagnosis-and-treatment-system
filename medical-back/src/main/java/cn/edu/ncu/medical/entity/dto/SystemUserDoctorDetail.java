package cn.edu.ncu.medical.entity.dto;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.TableField;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;
import java.util.Date;
@Data
public class SystemUserDoctorDetail {
	@NotNull
	private String username;
	@NotNull
	private String password;
	@NotNull
	private Integer type;
	@NotNull
	private String email;
	@NotNull
	private Integer registerType;
	@NotNull
	private Integer status;
	@NotNull
	private String realName;
	@NotNull
	private String idCard;
	@NotNull
	private BigDecimal price;
	@NotNull
	private String title;
	@NotNull
	private Long subDepartmentId;
	@NotNull
	private String professionalLicenseNumber;
	@NotNull
	private String introduction;

}
