package cn.edu.ncu.medical.exception;

import cn.edu.ncu.medical.result.ResultCodeEnum;

public class SuspendedStatusException extends MyRuntimeException{
    public SuspendedStatusException() {
        super(ResultCodeEnum.NOT_SUSPENDED);
    }
}
