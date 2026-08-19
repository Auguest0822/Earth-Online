package com.earthonline.backend.chat.dto;

import java.time.LocalDateTime;

public record ChatReadReceiptResponse(
        String readerPublicId,
        Long lastReadMessageId,
        LocalDateTime readAt
) {
}
