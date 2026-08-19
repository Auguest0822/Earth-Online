package com.earthonline.backend.chat.dto;

public record ChatUnreadCountResponse(
        String friendPublicId,
        long unreadCount
) {
}
