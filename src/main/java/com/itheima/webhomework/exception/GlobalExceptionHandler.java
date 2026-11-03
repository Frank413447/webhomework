package com.itheima.webhomework.exception;

import com.itheima.webhomework.entity.Result;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {
    //处理业务异常
    @ExceptionHandler(RuntimeException.class)
    public Result handleRuntimeException(RuntimeException e) {
        log.error("运行时异常: {}", e.getMessage());
        return new Result(false, e.getMessage());
    }
    //处理参数校验异常
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public Result handleValidException(MethodArgumentNotValidException e) {
        String msg = e.getBindingResult().getFieldError().getDefaultMessage();
        log.error("参数校验失败：{}", msg);
        return new Result(false, msg);
    }
    //处理其他未知异常
    @ExceptionHandler(Exception.class)
    public Result handleException(Exception e) {
        log.error("未知异常: {}", e.getMessage());
        return new Result(false, "服务器异常");
    }
}
