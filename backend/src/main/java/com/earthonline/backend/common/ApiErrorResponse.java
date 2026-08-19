package com.earthonline.backend.common;

import java.time.LocalDateTime;

public record ApiErrorResponse(
        int status,
        String code,
        String message,
        LocalDateTime timestamp
) {}
