package com.earthonline.backend.chat.dto;

import java.time.LocalDateTime;

public record ChatMessageResponse(
        Long id,
        ChatUserResponse sender,
        String content,
        LocalDateTime createdAt,
        boolean mine
) {
}
