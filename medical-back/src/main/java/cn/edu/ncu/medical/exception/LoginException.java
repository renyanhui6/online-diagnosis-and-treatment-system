package cn.edu.ncu.medical.exception;

import cn.edu.ncu.medical.result.ResultCodeEnum;
import lombok.Getter;

@Getter
public class LoginException extends MyRuntimeException {
	private Integer code;

	public LoginException(Integer code,String message) {
		super(message);
		this.code = code;

	}
	public LoginException(ResultCodeEnum resultCodeEnum) {
		this(resultCodeEnum.getCode(),resultCodeEnum.getMessage());
	}

}
