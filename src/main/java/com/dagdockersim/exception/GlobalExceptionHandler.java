package com.dagdockersim.exception;

import com.dagdockersim.common.BaseResponse;
import com.dagdockersim.common.ErrorCode;
import com.dagdockersim.common.ResultUtils;
import org.springframework.validation.BindException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.server.ResponseStatusException;

@RestControllerAdvice
public class GlobalExceptionHandler {
    @ExceptionHandler(BusinessException.class)
    public BaseResponse<Object> businessExceptionHandler(BusinessException exception) {
        return ResultUtils.error(exception.getCode(), exception.getMessage());
    }

    @ExceptionHandler(ResponseStatusException.class)
    public BaseResponse<Object> responseStatusExceptionHandler(ResponseStatusException exception) {
        ErrorCode errorCode = ErrorCode.OPERATION_ERROR;
        int status = exception.getStatus().value();
        if (status == 400) {
            errorCode = ErrorCode.PARAMS_ERROR;
        } else if (status == 401) {
            errorCode = ErrorCode.NO_AUTH_ERROR;
        } else if (status == 403) {
            errorCode = ErrorCode.FORBIDDEN_ERROR;
        } else if (status == 404) {
            errorCode = ErrorCode.NOT_FOUND_ERROR;
        }
        return ResultUtils.error(errorCode.getCode(), exception.getReason());
    }

    @ExceptionHandler({MethodArgumentNotValidException.class, BindException.class})
    public BaseResponse<Object> validationExceptionHandler(Exception exception) {
        return ResultUtils.error(ErrorCode.PARAMS_ERROR.getCode(), "请求参数校验失败");
    }

    @ExceptionHandler(Exception.class)
    public BaseResponse<Object> exceptionHandler(Exception exception) {
        return ResultUtils.error(ErrorCode.SYSTEM_ERROR.getCode(), exception.getMessage());
    }
}
