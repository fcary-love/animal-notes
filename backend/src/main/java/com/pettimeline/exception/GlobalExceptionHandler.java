package com.pettimeline.exception;

import com.pettimeline.model.vo.ApiResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(BusinessException.class)
    public ApiResponse<?> handleBusiness(BusinessException e, jakarta.servlet.http.HttpServletResponse response) {
        response.setStatus(mapStatus(e.getCode()));
        return ApiResponse.fail(e.getCode(), e.getMessage());
    }

    private int mapStatus(int code) {
        return switch (code) {
            case 400, 401, 403, 404, 409, 413, 415, 500 -> code;
            default -> 500;
        };
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ApiResponse<?> handleValidation(MethodArgumentNotValidException e) {
        String msg = e.getBindingResult().getFieldErrors().stream()
                .map(f -> f.getField() + ": " + f.getDefaultMessage())
                .findFirst()
                .orElse("参数校验失败");
        return ApiResponse.fail(400, msg);
    }

    @ExceptionHandler(Exception.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public ApiResponse<?> handleOther(Exception e) {
        log.error("Unexpected error", e);
        return ApiResponse.fail(500, "服务器内部错误");
    }
}
