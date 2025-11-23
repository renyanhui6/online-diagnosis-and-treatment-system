package cn.edu.ncu.medical.exception;

import cn.edu.ncu.medical.result.ResultCodeEnum;
import lombok.Getter;

@Getter
public class AppointmentException extends MyRuntimeException{
    private Integer code;

    public AppointmentException(Integer code,String message) {
        super(message);
        this.code = code;

    }
    public AppointmentException(ResultCodeEnum resultCodeEnum) {
        this(resultCodeEnum.getCode(),resultCodeEnum.getMessage());
    }

}
