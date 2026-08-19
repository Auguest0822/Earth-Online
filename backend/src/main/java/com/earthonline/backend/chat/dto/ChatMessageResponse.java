package com.earthonline.backend.chat.dto;

import com.earthonline.backend.chat.ChatMessageType;
import java.time.LocalDateTime;

public record ChatMessageResponse(
        Long id,
        ChatUserResponse sender,
        ChatUserResponse receiver,
        String content,
        String clientMessageId,
        ChatMessageType messageType,
        String imageUrl,
        LocalDateTime createdAt,
        LocalDateTime readAt,
        boolean mine
) {
}
