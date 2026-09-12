package com.earthonline.backend.common;

import jakarta.validation.ConstraintViolationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;

@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(ResponseStatusException.class)
    public ResponseEntity<ApiErrorResponse> handleStatus(ResponseStatusException exception) {
        int status = exception.getStatusCode().value();
        String message = exception.getReason() == null ? defaultMessage(status) : exception.getReason();
        // 4xx 属于正常的业务拒绝，记一行 warn；5xx 记完整堆栈，方便线上排障
        if (status >= 500) {
            log.error("请求处理失败 status={} reason={}", status, message, exception);
        } else {
            log.warn("请求被拒绝 status={} reason={}", status, message);
        }
        return ResponseEntity.status(status).body(error(status, "REQUEST_FAILED", message));
    }

    @ExceptionHandler({MethodArgumentNotValidException.class, ConstraintViolationException.class})
    public ResponseEntity<ApiErrorResponse> handleValidation(Exception exception) {
        log.warn("请求参数校验失败: {}", exception.getMessage());
        return ResponseEntity.badRequest().body(error(400, "VALIDATION_FAILED", "请求参数不正确"));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiErrorResponse> handleUnexpected(Exception exception) {
        // 以前这里把异常直接吞掉：线上只看到 500，日志里查不到原因，排障无从下手。
        // 现在把堆栈完整记录下来（响应体仍然不暴露内部细节）。
        log.error("未预期的服务器异常: {}", exception.getMessage(), exception);
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
