package com.dagdockersim.common;

public final class ResultUtils {
    private ResultUtils() {
    }

    public static <T> BaseResponse<T> success(T data) {
        return new BaseResponse<T>(ErrorCode.SUCCESS.getCode(), data, ErrorCode.SUCCESS.getMessage());
    }

    public static <T> BaseResponse<T> error(ErrorCode errorCode) {
        return new BaseResponse<T>(errorCode.getCode(), null, errorCode.getMessage());
    }

    public static <T> BaseResponse<T> error(int code, String message) {
        return new BaseResponse<T>(code, null, message);
    }
}
