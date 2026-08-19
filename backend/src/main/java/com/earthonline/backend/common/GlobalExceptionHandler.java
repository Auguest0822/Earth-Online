package com.earthonline.backend.common;

import jakarta.validation.ConstraintViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;

@RestControllerAdvice
public class GlobalExceptionHandler {
    @ExceptionHandler(ResponseStatusException.class)
    public ResponseEntity<ApiErrorResponse> handleStatus(ResponseStatusException exception) {
        int status = exception.getStatusCode().value();
        String message = exception.getReason() == null ? defaultMessage(status) : exception.getReason();
        return ResponseEntity.status(status).body(error(status, "REQUEST_FAILED", message));
    }

    @ExceptionHandler({MethodArgumentNotValidException.class, ConstraintViolationException.class})
    public ResponseEntity<ApiErrorResponse> handleValidation(Exception exception) {
        return ResponseEntity.badRequest().body(error(400, "VALIDATION_FAILED", "请求参数不正确"));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiErrorResponse> handleUnexpected(Exception exception) {
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(error(500, "INTERNAL_ERROR", "服务器暂时无法处理请求，请稍后重试"));
    }

    private ApiErrorResponse error(int status, String code, String message) {
        return new ApiErrorResponse(status, code, message, LocalDateTime.now());
    }

    private String defaultMessage(int status) {
        return switch (status) {
            case 400 -> "请求参数不正确";
            case 401 -> "登录状态已过期，请重新登录";
            case 403 -> "没有权限执行此操作";
            case 404 -> "请求的内容不存在";
            case 409 -> "当前操作存在冲突";
            case 429 -> "请求过于频繁，请稍后重试";
            default -> "请求失败，请稍后重试";
        };
    }
}
