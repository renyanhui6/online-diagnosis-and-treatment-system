package cn.edu.ncu.medical.entity.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class SystemUserPatientDetail {
	@NotNull
	private String username;
	@NotNull
	private String password;
	@NotNull
	private String email;
	private Integer registerType;
	private Integer status;
	private String nickname;
	private String realName;
	private String idCard;
	private Integer gender;
	private String phone;
	private String homeAddress;
}
