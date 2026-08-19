package com.earthonline.backend.chat.dto;

import java.time.LocalDateTime;

public record ChatMessageResponse(
        Long id,
        ChatUserResponse sender,
        ChatUserResponse receiver,
        String content,
        String clientMessageId,
        LocalDateTime createdAt,
        LocalDateTime readAt,
        boolean mine
) {
}
