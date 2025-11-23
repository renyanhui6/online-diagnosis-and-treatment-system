package cn.edu.ncu.medical.exception;

import cn.edu.ncu.medical.result.ResultCodeEnum;
import lombok.Getter;

@Getter
public class MedicineOrderException extends MyRuntimeException{
    private Integer code;

    public MedicineOrderException(Integer code,String message) {
        super(message);
        this.code = code;

    }
    public MedicineOrderException(ResultCodeEnum resultCodeEnum) {
        this(resultCodeEnum.getCode(),resultCodeEnum.getMessage());
    }

}

