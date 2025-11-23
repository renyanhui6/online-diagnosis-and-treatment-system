package cn.edu.ncu.medical.exception;

import cn.edu.ncu.medical.result.ResultCodeEnum;
import lombok.Getter;

@Getter
public class MyRuntimeException extends RuntimeException {
	private Integer code;
	public MyRuntimeException(String message) {
		super(message);
	}
	public MyRuntimeException(ResultCodeEnum resultCodeEnum) {
		super(resultCodeEnum.getMessage());
		this.code = resultCodeEnum.getCode();
	}

}
