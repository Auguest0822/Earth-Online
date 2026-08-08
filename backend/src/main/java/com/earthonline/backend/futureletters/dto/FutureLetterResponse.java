package com.earthonline.backend.futureletters.dto;

import java.time.LocalDateTime;

public record FutureLetterResponse(
        Long id,
        String title,
        String content,
        String mood,
        LocalDateTime openAt,
        String status,
        String visibility,
        Boolean allowEditBeforeOpen,
        LocalDateTime createdAt
) {
}