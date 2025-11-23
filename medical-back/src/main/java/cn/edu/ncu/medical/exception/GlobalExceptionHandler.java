package cn.edu.ncu.medical.exception;

import cn.edu.ncu.medical.result.Result;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestController;

@ControllerAdvice
@RestController
public class GlobalExceptionHandler {
	@ExceptionHandler(Throwable.class)
	public Result handlerAll(Throwable throwable){
		throwable.printStackTrace();
		return Result.fail();
	}
	@ExceptionHandler(MyRuntimeException.class)
	public Result handleMyRuntimeException(MyRuntimeException e){
		e.printStackTrace();
		return Result.fail(e.getCode(),e.getMessage());
	}
}
