package cn.edu.ncu.medical.exception;

import cn.edu.ncu.medical.result.ResultCodeEnum;
import lombok.Getter;

@Getter
public class TypeException extends MyRuntimeException {
    private Integer code;

    public TypeException(Integer code,String message) {
        super(message);
        this.code = code;

    }
    public TypeException(ResultCodeEnum resultCodeEnum) {
        this(resultCodeEnum.getCode(),resultCodeEnum.getMessage());
    }
}
